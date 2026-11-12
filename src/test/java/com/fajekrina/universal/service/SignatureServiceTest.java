package com.fajekrina.universal.service;

import com.fajekrina.universal.config.properties.SigningProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignatureServiceTest {

    private static KeyPair ed25519() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    @Test
    void signAndVerifyRoundTrip() throws Exception {
        byte[] payload = "{\"schemaVersion\":\"1.0\",\"ok\":true}".getBytes();
        SignatureService svc = new SignatureService(new SigningProperties(false, "", ""));
        // disabled service must fail closed
        assertThatThrownBy(() -> svc.sign(payload)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyDetectsTampering() throws Exception {
        KeyPair kp = ed25519();
        byte[] payload = "{\"a\":1}".getBytes();
        java.security.Signature signer = java.security.Signature.getInstance("Ed25519");
        signer.initSign(kp.getPrivate());
        signer.update(payload);
        byte[] sig = signer.sign();
        assertThat(SignatureService.verify(kp.getPublic().getEncoded(), payload, sig)).isTrue();
        byte[] tampered = payload.clone();
        tampered[0] = (byte) 'X';
        assertThat(SignatureService.verify(kp.getPublic().getEncoded(), tampered, sig)).isFalse();
    }

    @Test
    void disabledServiceReportsClearly() {
        SignatureService svc = new SignatureService(new SigningProperties(false, "", ""));
        assertThat(svc.isEnabled()).isFalse();
        assertThatThrownBy(svc::publicKeyBase64).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void loadsKeysFromFilesAndVerifies(@TempDir Path dir) throws Exception {
        KeyPair kp = ed25519();
        byte[] pubDer = kp.getPublic().getEncoded();
        java.nio.file.Path pubFile = dir.resolve("pub.b64");
        java.nio.file.Files.writeString(pubFile, java.util.Base64.getEncoder().encodeToString(pubDer));
        java.nio.file.Path privFile = dir.resolve("priv.b64");
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("Ed25519");
        java.security.spec.PKCS8EncodedKeySpec pkcs8 =
                kf.getKeySpec(kp.getPrivate(), java.security.spec.PKCS8EncodedKeySpec.class);
        java.nio.file.Files.writeString(privFile, java.util.Base64.getEncoder().encodeToString(pkcs8.getEncoded()));

        SignatureService svc = new SignatureService(new SigningProperties(true, privFile.toString(), pubFile.toString()));
        assertThat(svc.publicKeyRaw()).hasSize(32);
        byte[] payload = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] sig = svc.sign(payload);
        assertThat(SignatureService.verify(svc.publicKeyRaw(), payload, sig)).isTrue();
    }
}
