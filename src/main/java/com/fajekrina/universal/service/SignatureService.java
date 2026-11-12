package com.fajekrina.universal.service;

import com.fajekrina.universal.config.properties.SigningProperties;
import com.fajekrina.universal.utils.CanonicalJson;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Detached Ed25519 signing for Universal payloads (mirrors the client-side
 * verifyEnvelope in @fajekrina/sdui). Opt-in: without configured key files the
 * service stays disabled and all signing calls fail closed with a clear error.
 *
 * Config:
 *   sdui.signing.enabled=false
 *   sdui.signing.private-key-file=   (PKCS8 DER, base64 text file)
 *   sdui.signing.public-key-file=    (X.509 DER, base64 text file)
 * Generate test keys with: openssl genpkey -algorithm ed25519 -out private.pem
 * then DER-encode both keys to base64 files.
 */
@Service
public class SignatureService {

    private final boolean enabled;
    private final String privateKeyFile;
    private final String publicKeyFile;

    public SignatureService(SigningProperties properties) {
        this.enabled = properties.enabled();
        this.privateKeyFile = properties.privateKeyFile();
        this.publicKeyFile = properties.publicKeyFile();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static byte[] readKeyFile(String path) throws Exception {
        if (path == null || path.isBlank()) throw new IllegalStateException("signing key file is not configured");
        String text = Files.readString(Paths.get(path)).trim()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(text);
    }

    private PrivateKey loadPrivateKey() throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(readKeyFile(privateKeyFile));
        return KeyFactory.getInstance("Ed25519").generatePrivate(spec);
    }

    public byte[] sign(byte[] payload) throws Exception {
        if (!enabled) throw new IllegalStateException("signing is disabled (sdui.signing.enabled=false)");
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(loadPrivateKey());
        sig.update(payload);
        return sig.sign();
    }

    public byte[] publicKeyRaw() throws Exception {
        if (!enabled) throw new IllegalStateException("signing is disabled (sdui.signing.enabled=false)");
        byte[] der = readKeyFile(publicKeyFile);
        if (der == null || der.length == 0) throw new IllegalStateException("signing public key file is not configured");
        // Accept X.509 DER (raw key is the last 32 bytes) or an already-raw 32-byte key.
        // Raw form matches what clients need for verification.
        if (der.length == 32) return der;
        if (der.length > 32) {
            byte[] raw = new byte[32];
            System.arraycopy(der, der.length - 32, raw, 0, 32);
            return raw;
        }
        throw new IllegalStateException("signing public key has unexpected length: " + der.length);
    }

    public String publicKeyBase64() throws Exception {
        return Base64.getEncoder().encodeToString(publicKeyRaw());
    }

    /** Stable key id: first 16 hex chars of sha256(raw public key). Sent in envelopes as `keyId`. */
    public String keyId() throws Exception {
        return CanonicalJson.sha256Hex(publicKeyRaw()).substring(0, 16);
    }

    private static final byte[] ED25519_SPKI_PREFIX = new byte[] {
        0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00 };

    /** Accepts a raw 32-byte key or X.509 DER (wraps raw automatically). */
    public static PublicKey publicKeyFromBytes(byte[] keyBytes) throws Exception {
        byte[] der = keyBytes;
        if (keyBytes.length == 32) {
            der = new byte[ED25519_SPKI_PREFIX.length + 32];
            System.arraycopy(ED25519_SPKI_PREFIX, 0, der, 0, ED25519_SPKI_PREFIX.length);
            System.arraycopy(keyBytes, 0, der, ED25519_SPKI_PREFIX.length, 32);
        }
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
    }

    public static boolean verify(byte[] publicKey, byte[] payload, byte[] signature) throws Exception {
        PublicKey pub = publicKeyFromBytes(publicKey);
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(pub);
        sig.update(payload);
        return sig.verify(signature);
    }
}
