package com.fajekrina.universal.config;

import com.fajekrina.universal.config.properties.SeedProperties;
import com.fajekrina.universal.model.entity.SduiSigningKey;
import com.fajekrina.universal.model.request.PageKeyRequestBuilder;
import com.fajekrina.universal.model.request.SaveDraftPageRequestBuilder;
import com.fajekrina.universal.model.request.SavePolicyRequestBuilder;
import com.fajekrina.universal.model.request.SaveScreenRequestBuilder;
import com.fajekrina.universal.model.request.ScreenKeyRequestBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiPolicyRepository;
import com.fajekrina.universal.repository.SduiScreenRepository;
import com.fajekrina.universal.repository.SduiSigningKeyRepository;
import com.fajekrina.universal.service.PublishPageService;
import com.fajekrina.universal.service.PublishScreenService;
import com.fajekrina.universal.service.SaveDraftPageService;
import com.fajekrina.universal.service.SaveScreenDraftService;
import com.fajekrina.universal.service.SavePolicyService;
import com.fajekrina.universal.service.SignatureService;
import com.fajekrina.universal.utils.constants.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Startup:
 *  1. registers the configured Ed25519 public key in sdui_signing_key (if signing is on);
 *  2. seeds an EMPTY database from classpath data/universal/{policies,pages} — save + publish,
 *     exactly like the builder would (actor "seed");
 *  3. seeds spec screens from classpath data/universal/screens/{merchantId}/*.json the same way.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class SduiBootstrap implements ApplicationRunner {

    private final SduiPageRepository pageRepository;
    private final SduiPolicyRepository policyRepository;
    private final SduiSigningKeyRepository signingKeyRepository;
    private final SaveDraftPageService saveDraftPageService;
    private final PublishPageService publishPageService;
    private final SavePolicyService savePolicyService;
    private final SignatureService signatureService;
    private final SduiScreenRepository screenRepository;
    private final SaveScreenDraftService saveScreenDraftService;
    private final PublishScreenService publishScreenService;
    private final SeedProperties seedProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (signatureService.isEnabled()) registerSigningKey();
        if (seedProperties.enabled() && pageRepository.count() == 0) {
            ActorContext.runAs(Constants.SEED_ACTOR, () -> {
                seed();
                return null;
            });
        }
        if (seedProperties.enabled() && screenRepository.count() == 0) {
            ActorContext.runAs(Constants.SEED_ACTOR, () -> {
                seedScreens();
                return null;
            });
        }
    }

    /** Spec screens: classpath data/universal/screens/{merchantId}/*.json — save + publish, like the builder. */
    private void seedScreens() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] screens = resolver.getResources("classpath*:data/universal/screens/*/*.json");
            Arrays.sort(screens, Comparator.comparing(Resource::getDescription, Comparator.nullsLast(String::compareTo)));
            for (Resource r : screens) {
                String[] parts = r.getURL().getPath().split("/");
                String merchantId = parts[parts.length - 2];
                Map<String, Object> payload = read(r, new TypeReference<>() {});
                @SuppressWarnings("unchecked")
                String screenKey = String.valueOf(((Map<String, Object>) payload.get("screen")).get("key"));
                saveScreenDraftService.execute(SaveScreenRequestBuilder.builder()
                        .merchantId(merchantId).screenKey(screenKey).payload(payload).build());
                publishScreenService.execute(ScreenKeyRequestBuilder.builder().merchantId(merchantId).screenKey(screenKey).build());
                log.info("SDUI seeded screen {}/{}", merchantId, screenKey);
            }
        } catch (IOException e) {
            throw new IllegalStateException("SDUI screen seed failed", e);
        }
    }

    private void registerSigningKey() throws Exception {
        String keyId = signatureService.keyId();
        if (!signingKeyRepository.existsById(keyId)) {
            ActorContext.runAs(Constants.SYSTEM_ACTOR, () -> signingKeyRepository.saveAndFlush(SduiSigningKey.builder()
                    .keyId(keyId)
                    .publicKey(publicKey())
                    .algorithm("Ed25519")
                    .status(Constants.SIGNING_KEY_ACTIVE)
                    .build()));
        }
        log.info("SDUI signing key registered: {}", keyId);
    }

    private String publicKey() {
        try {
            return signatureService.publicKeyBase64();
        } catch (Exception e) {
            throw new IllegalStateException("cannot read signing public key", e);
        }
    }

    private void seed() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            for (Resource r : resolver.getResources("classpath*:data/universal/policies/*.json")) {
                Map<String, Object> policy = read(r, new TypeReference<>() {});
                String merchantId = String.valueOf(policy.get("merchantId"));
                if (policyRepository.findFirstByMerchantIdOrderByPolicyVersionDesc(merchantId).isEmpty()) {
                    savePolicyService.execute(SavePolicyRequestBuilder.builder().merchantId(merchantId).body(policy).build());
                }
            }
            Resource[] pages = resolver.getResources("classpath*:data/universal/pages/*/*.json");
            Arrays.sort(pages, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));
            for (Resource r : pages) {
                Map<String, Object> page = read(r, new TypeReference<>() {});
                String merchantId = (String) page.get("merchantId");
                String pageKey = (String) page.get("pageKey");
                if (merchantId == null || pageKey == null) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> layout = (Map<String, Object>) page.get("layout");
                saveDraftPageService.execute(SaveDraftPageRequestBuilder.builder()
                        .merchantId(merchantId)
                        .pageKey(pageKey)
                        .pageId((String) page.get("pageId"))
                        .schemaVersion((String) page.get("schemaVersion"))
                        .layout(layout)
                        .build());
                if ("PUBLISHED".equalsIgnoreCase(String.valueOf(page.get("status")))) {
                    publishPageService.execute(PageKeyRequestBuilder.builder().merchantId(merchantId).pageKey(pageKey).build());
                }
                log.info("SDUI seeded {}/{}", merchantId, pageKey);
            }
        } catch (IOException e) {
            throw new IllegalStateException("SDUI seed failed", e);
        }
    }

    private <T> T read(Resource r, TypeReference<T> type) throws IOException {
        try (InputStream is = r.getInputStream()) {
            return objectMapper.readValue(is, type);
        }
    }
}
