package com.fajekrina.universal;

import com.fajekrina.universal.controller.ScreenClientController;
import com.fajekrina.universal.controller.ScreenMerchantController;
import com.fajekrina.universal.model.request.SaveScreenBody;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:screen-test;DB_CLOSE_DELAY=-1")
class SduiScreenIntegrationTest {

    @Autowired ScreenMerchantController merchant;
    @Autowired ScreenClientController client;
    @Autowired JdbcTemplate jdbc;

    private static Map<String, Object> screen(String key, String text) {
        return Map.of("schemaVersion", "1.1", "screen", Map.of("key", key,
                "root", Map.of("type", "vstack", "children", List.of(Map.of("type", "text", "props", Map.of("value", text))))));
    }

    private static UniversalFault faultOf(Throwable t) {
        return ((UniversalException) t).getFault();
    }

    @Test
    void seededScreensAreServedWithEtagAndListedInTheManifest() {
        assertThat(merchant.listScreens("merchant_demo").screens()).extracting("key")
                .contains("checkout", "home", "seatSelection");

        var res = client.getScreen("merchant_demo", "home", false, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = (String) res.getBody();
        assertThat(CanonicalJson.readMap(body).get("schemaVersion")).isEqualTo("1.1");
        // the served bytes are the hashed bytes
        assertThat(res.getHeaders().getETag()).isEqualTo("\"" + CanonicalJson.sha256Hex(body) + "\"");
        assertThat(client.getScreen("merchant_demo", "home", false, res.getHeaders().getETag()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_MODIFIED);

        var manifest = CanonicalJson.readMap((String) client.getManifest("merchant_demo", false, null).getBody());
        assertThat((Map<String, Object>) manifest.get("screens")).containsEntry("checkout", "v1").containsEntry("home", "v1").containsEntry("seatSelection", "v1");
        assertThat(((Number) manifest.get("sequence")).longValue()).isPositive();

        assertThatThrownBy(() -> client.getScreen("merchant_demo", "nope", false, null))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.PUBLISHED_SCREEN_NOT_FOUND));
        assertThatThrownBy(() -> client.getScreen("merchant_demo", "home", true, null))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.SIGNING_DISABLED));
    }

    @Test
    void saveValidatesLocksAndPublishesNewSequences() {
        // invalid: unknown action type / key mismatch
        var bad = Map.of("schemaVersion", "1.1", "screen", Map.of("key", "promo",
                "root", Map.of("type", "button", "action", Map.of("type", "eval"))));
        assertThatThrownBy(() -> merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(bad, null)))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.INVALID_SCREEN));
        assertThatThrownBy(() -> merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(screen("other", "x"), null)))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.INVALID_SCREEN));

        var saved = merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(screen("promo", "Promo 1"), null));
        assertThat(saved.unchanged()).isFalse();
        assertThat(merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(screen("promo", "Promo 1"), saved.lockVersion())).unchanged()).isTrue();
        // not published yet → the client does not see it
        assertThatThrownBy(() -> client.getScreen("merchant_demo", "promo", false, null))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.PUBLISHED_SCREEN_NOT_FOUND));

        long before = ((Number) CanonicalJson.readMap((String) client.getManifest("merchant_demo", false, null).getBody()).get("sequence")).longValue();
        var first = merchant.publish("merchant_demo", "promo");
        assertThat(first.sequence()).isEqualTo(1);
        assertThat(merchant.publish("merchant_demo", "promo").unchanged()).isTrue();   // nothing new to publish

        var second = merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(screen("promo", "Promo 2"), first.lockVersion()));
        assertThatThrownBy(() -> merchant.saveDraft("merchant_demo", "promo", new SaveScreenBody(screen("promo", "Promo 3"), first.lockVersion())))
                .satisfies(t -> assertThat(faultOf(t)).isEqualTo(UniversalFault.LOCK_CONFLICT));
        assertThat(merchant.publish("merchant_demo", "promo").sequence()).isEqualTo(2);
        assertThat(merchant.getDraft("merchant_demo", "promo").lockVersion()).isEqualTo(second.lockVersion() + 1); // publish touched the row

        assertThat((String) client.getScreen("merchant_demo", "promo", false, null).getBody()).contains("Promo 2");
        var manifest = CanonicalJson.readMap((String) client.getManifest("merchant_demo", false, null).getBody());
        assertThat(((Number) manifest.get("sequence")).longValue()).isGreaterThan(before);
        assertThat(((Map<?, ?>) manifest.get("screens")).get("promo")).isEqualTo("v2");
    }

    @Test
    void releasesAreImmutableInTheDatabase() {
        assertThatThrownBy(() -> jdbc.update("UPDATE sdui_screen_release SET payload = '{}'")).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM sdui_screen_release")).hasMessageContaining("immutable");
    }
}
