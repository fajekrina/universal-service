package com.fajekrina.universal;

import com.fajekrina.universal.controller.UniversalClientController;
import com.fajekrina.universal.model.request.PageKeyRequestBuilder;
import com.fajekrina.universal.model.request.RollbackPageRequestBuilder;
import com.fajekrina.universal.model.request.SaveDraftPageRequest;
import com.fajekrina.universal.model.request.SaveDraftPageRequestBuilder;
import com.fajekrina.universal.model.request.SavePolicyRequestBuilder;
import com.fajekrina.universal.model.request.SaveStyleRequestBuilder;
import com.fajekrina.universal.model.request.SetPageStyleRequestBuilder;
import com.fajekrina.universal.service.ArchivePageService;
import com.fajekrina.universal.service.GetDraftPageService;
import com.fajekrina.universal.service.GetPageAuditService;
import com.fajekrina.universal.service.GetPolicyService;
import com.fajekrina.universal.service.ListPagesService;
import com.fajekrina.universal.service.ListReleasesService;
import com.fajekrina.universal.service.PublishPageService;
import com.fajekrina.universal.service.RollbackPageService;
import com.fajekrina.universal.service.SaveDraftPageService;
import com.fajekrina.universal.service.SavePolicyService;
import com.fajekrina.universal.service.SaveStyleService;
import com.fajekrina.universal.service.SetPageStyleService;
import com.fajekrina.universal.model.request.MerchantRequestBuilder;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** H2 end-to-end: seed → 3 artefacts → frozen release → client payload; DB-enforced immutability. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:store-test;DB_CLOSE_DELAY=-1")
class SduiStoreIntegrationTest {

    @Autowired SaveDraftPageService saveDraftPageService;
    @Autowired PublishPageService publishPageService;
    @Autowired RollbackPageService rollbackPageService;
    @Autowired ArchivePageService archivePageService;
    @Autowired GetDraftPageService getDraftPageService;
    @Autowired ListPagesService listPagesService;
    @Autowired ListReleasesService listReleasesService;
    @Autowired GetPageAuditService getPageAuditService;
    @Autowired SetPageStyleService setPageStyleService;
    @Autowired SaveStyleService saveStyleService;
    @Autowired SavePolicyService savePolicyService;
    @Autowired GetPolicyService getPolicyService;
    @Autowired UniversalClientController client;
    @Autowired JdbcClient jdbc;
    final ObjectMapper om = new ObjectMapper();

    private JsonNode source(String key) throws Exception {
        try (InputStream is = new ClassPathResource("data/universal/pages/merchant_demo/" + key + ".json").getInputStream()) {
            return om.readTree(is);
        }
    }

    private JsonNode served(String mid, String key) throws Exception {
        ResponseEntity<Object> res = client.getPublishedPage(mid, key, false, null);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return om.readTree((String) res.getBody());
    }

    private static SaveDraftPageRequest save(String mid, String key, List<Map<String, Object>> components, Integer lock) {
        return SaveDraftPageRequestBuilder.builder().merchantId(mid).pageKey(key)
                .layout(new LinkedHashMap<>(Map.of("type", "Container", "components", components))).lockVersion(lock).build();
    }

    private static Map<String, Object> comp(String id, String type, String parent, int order, Map<String, Object> props) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", id);
        c.put("component", type);
        c.put("parentId", parent);
        c.put("props", new LinkedHashMap<>(props));
        c.put("style", new LinkedHashMap<>());
        c.put("order", order);
        return c;
    }

    private static com.fajekrina.universal.model.request.PageKeyRequest page(String mid, String key) {
        return PageKeyRequestBuilder.builder().merchantId(mid).pageKey(key).build();
    }

    @Test
    void seededPagesRoundTripThroughThreeArtefacts() throws Exception {
        for (String key : List.of("home", "checkout", "seat-selection")) {
            JsonNode out = served("merchant_demo", key);
            assertThat(out.get("layout").get("components")).as(key).isEqualTo(source(key).get("layout").get("components"));
            assertThat(out.get("releaseSequence").asInt()).isEqualTo(1);
            assertThat(out.get("policy").get("allowedDomains").toString()).contains("example.com");
        }
        assertThat(served("merchant_demo", "seat-selection").get("layout").get("title").asText()).isEqualTo("Seat Selection");
        assertThat(getPageAuditService.execute(page("merchant_demo", "home")).entries().get(0).actor()).isEqualTo("seed");
    }

    @Test
    void etagRevalidationReturns304() {
        String etag = client.getPublishedPage("merchant_demo", "home", false, null).getHeaders().getETag();
        assertThat(etag).isNotBlank();
        ResponseEntity<Object> second = client.getPublishedPage("merchant_demo", "home", false, etag);
        assertThat(second.getStatusCode().value()).isEqualTo(304);
        assertThat(second.getBody()).isNull();
    }

    @Test
    void saveDedupPublishRollback() throws Exception {
        String mid = "merchant_rb";
        List<Map<String, Object>> v1 = new ArrayList<>(List.of(
                comp("01HQ00000000000000000000R1", "Container", null, 0, Map.of("as", "div")),
                comp("01HQ00000000000000000000R2", "Button", "01HQ00000000000000000000R1", 1,
                        Map.of("label", "Satu", "action", Map.of("behavior", "navigate", "target", "home")))));
        saveDraftPageService.execute(save(mid, "home", v1, null));
        assertThat(publishPageService.execute(page(mid, "home")).sequence()).isEqualTo(1);

        var again = saveDraftPageService.execute(save(mid, "home", v1, null));
        assertThat(again.unchanged()).containsExactly("tree", "script");

        List<Map<String, Object>> v2 = new ArrayList<>(List.of(v1.get(0), comp("01HQ00000000000000000000R2", "Button", "01HQ00000000000000000000R1", 1,
                Map.of("label", "Dua", "action", Map.of("behavior", "navigate", "target", "home")))));
        var saved = saveDraftPageService.execute(save(mid, "home", v2, again.page().lockVersion()));
        assertThat(saved.tree()).isEqualTo("home@2");
        assertThat(saved.unchanged()).containsExactly("script");
        assertThat(publishPageService.execute(page(mid, "home")).sequence()).isEqualTo(2);
        assertThat(served(mid, "home").at("/layout/components/1/props/label").asText()).isEqualTo("Dua");

        var rb = rollbackPageService.execute(RollbackPageRequestBuilder.builder().merchantId(mid).pageKey("home").toSequence(1).build());
        assertThat(rb.sequence()).isEqualTo(3);
        assertThat(served(mid, "home").at("/layout/components/1/props/label").asText()).isEqualTo("Satu");

        var releases = listReleasesService.execute(page(mid, "home")).releases();
        assertThat(releases).hasSize(3);
        assertThat(releases.get(0).isActive()).isTrue();
        assertThat(releases.get(0).rollbackOf()).isNotNull();
        assertThat(releases.get(0).treeVersion()).isEqualTo(1);
        assertThat(getPageAuditService.execute(page(mid, "home")).entries()).extracting(e -> e.event())
                .contains("SAVE_DRAFT", "PUBLISH", "ROLLBACK");
    }

    @Test
    void databaseEnforcesImmutability() {
        assertThatThrownBy(() -> jdbc.sql("UPDATE sdui_tree SET note = 'x'").update()).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.sql("UPDATE sdui_policy SET created_by = 'x'").update()).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM sdui_release").update()).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.sql("UPDATE sdui_release SET payload = '{}'").update()).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.sql("UPDATE sdui_release SET sequence = sequence + 100").update()).hasMessageContaining("immutable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void styleArtefactResolvesAndUnknownRefsFailPublish() throws Exception {
        String mid = "merchant_st";
        Map<String, Object> styleBody = Map.of(
                "tokens", Map.of("color", Map.of("brand.primary", "#6c8cff")),
                "shared", Map.of("btnPrimary", Map.of("color", "#fff", "borderRadius", "8px", "backgroundColor", "#000")));
        saveStyleService.execute(SaveStyleRequestBuilder.builder().merchantId(mid).styleKey("brand-default").body(styleBody).build());
        Map<String, Object> button = comp("01HQ00000000000000000000S1", "Button", null, 0, Map.of("label", "Beli"));
        button.put("style", new LinkedHashMap<>(Map.of("borderRadius", "20px")));
        button.put("token", Map.of("backgroundColor", "brand.primary"));
        button.put("sharedStyle", "btnPrimary");
        saveDraftPageService.execute(save(mid, "promo", new ArrayList<>(List.of(button)), null));
        setPageStyleService.execute(SetPageStyleRequestBuilder.builder().merchantId(mid).pageKey("promo").styleKey("brand-default").build());
        publishPageService.execute(page(mid, "promo"));
        JsonNode style = served(mid, "promo").at("/layout/components/0/style");
        assertThat(om.convertValue(style, Map.class)).isEqualTo(Map.of("color", "#fff", "borderRadius", "20px", "backgroundColor", "#6c8cff"));

        button.put("token", Map.of("color", "brand.missing"));
        saveDraftPageService.execute(save(mid, "promo", new ArrayList<>(List.of(button)), null));
        assertThatThrownBy(() -> publishPageService.execute(page(mid, "promo")))
                .isInstanceOf(UniversalException.class)
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.UNRESOLVED_REFERENCE));
        assertThat(listReleasesService.execute(page(mid, "promo")).releases()).hasSize(1); // failed publish rolled back
        assertThat(saveStyleService.execute(SaveStyleRequestBuilder.builder().merchantId(mid).styleKey("brand-default").body(styleBody).build()).version())
                .as("identical style body does not create a new version").isEqualTo(1);
    }

    @Test
    void archiveStopsServingAndLockVersionConflicts() {
        String mid = "merchant_ar";
        var first = saveDraftPageService.execute(save(mid, "gone",
                new ArrayList<>(List.of(comp("01HQ00000000000000000000A1", "Text", null, 0, Map.of("content", "x")))), null));
        int lock = first.page().lockVersion();
        assertThatThrownBy(() -> saveDraftPageService.execute(save(mid, "gone",
                new ArrayList<>(List.of(comp("01HQ00000000000000000000A1", "Text", null, 0, Map.of("content", "y")))), lock + 7)))
                .isInstanceOf(UniversalException.class)
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.LOCK_CONFLICT));

        publishPageService.execute(page(mid, "gone"));
        assertThat(client.getPublishedPage(mid, "gone", false, null).getStatusCode().value()).isEqualTo(200);
        archivePageService.execute(page(mid, "gone"));
        assertThatThrownBy(() -> client.getPublishedPage(mid, "gone", false, null))
                .isInstanceOf(UniversalException.class);
        assertThat(listPagesService.execute(MerchantRequestBuilder.builder().merchantId(mid).build()).pages()).doesNotContain("gone");
        assertThatThrownBy(() -> getDraftPageService.execute(page(mid, "nope"))).isInstanceOf(UniversalException.class);
    }

    @Test
    void policiesAreVersionedNeverUpdated() {
        var v1 = savePolicyService.execute(SavePolicyRequestBuilder.builder().merchantId("m_one")
                .body(Map.of("allowedDomains", List.of("example.com"), "allowedEvents", List.of("addToCart"))).build());
        assertThat(v1.policy().policyVersion()).isEqualTo("1");
        var v2 = savePolicyService.execute(SavePolicyRequestBuilder.builder().merchantId("m_one")
                .body(Map.of("allowedDomains", List.of("example.com"))).build());
        assertThat(v2.policy().policyVersion()).isEqualTo("2");
        assertThat(getPolicyService.execute(MerchantRequestBuilder.builder().merchantId("m_one").build()).allowedEvents()).isEmpty();
        assertThat(getPolicyService.execute(MerchantRequestBuilder.builder().merchantId("ghost").build()).policyVersion()).isEqualTo("0");
        assertThatThrownBy(() -> savePolicyService.execute(SavePolicyRequestBuilder.builder().merchantId("m_two")
                .body(Map.of("allowedDomains", "example.com")).build())).isInstanceOf(UniversalException.class);
    }
}
