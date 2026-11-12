package com.fajekrina.universal;

import com.fajekrina.universal.controller.UniversalClientController;
import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.service.SignatureService;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signing-test;DB_CLOSE_DELAY=-1")
class SduiSigningIntegrationTest {

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        KeyPair kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Path dir = Files.createTempDirectory("sdui-keys");
        Path priv = dir.resolve("private.b64");
        Path pub = dir.resolve("public.b64");
        Files.writeString(priv, Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
        Files.writeString(pub, Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        registry.add("sdui.signing.enabled", () -> "true");
        registry.add("sdui.signing.private-key-file", priv::toString);
        registry.add("sdui.signing.public-key-file", pub::toString);
    }

    @Autowired UniversalClientController client;
    @Autowired SignatureService signing;

    @Test
    void signedEnvelopeVerifiesWithRegisteredKey() throws Exception {
        var keys = client.getSigningKeys().keys();
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).keyId()).isEqualTo(signing.keyId());

        ResponseEntity<Object> res = client.getPublishedPage("merchant_demo", "home", true, null);
        var env = (PublishedPageResponse.SignedEnvelope) res.getBody();
        assertThat(env.keyId()).isEqualTo(signing.keyId());
        byte[] payload = Base64.getDecoder().decode(env.payload());
        byte[] sig = Base64.getDecoder().decode(env.signature());
        byte[] rawPub = Base64.getDecoder().decode(keys.get(0).publicKey());
        assertThat(SignatureService.verify(rawPub, payload, sig)).isTrue();
        // the signed bytes are exactly the served bytes: their hash is the ETag
        assertThat("\"" + CanonicalJson.sha256Hex(payload) + "\"").isEqualTo(res.getHeaders().getETag());
        assertThat(new ObjectMapper().readTree(payload).get("pageKey").asText()).isEqualTo("home");
    }
}
