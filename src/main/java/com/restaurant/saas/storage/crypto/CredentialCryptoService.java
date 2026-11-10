package com.restaurant.saas.storage.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

/**
 * AES/GCM encryption for Cloudinary api_secret at rest.
 * <p>
 * Key source: {@code app.storage.cloudinary.encrypt-key}. It may be a base64 string that
 * decodes to 16/24/32 bytes, or any arbitrary passphrase (which is SHA-256 hashed to a
 * 32-byte AES-256 key). If unset, a deterministic dev key is used and logged once.
 */
public class CredentialCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    public CredentialCryptoService(String configuredKey) {
        byte[] raw;
        if (configuredKey != null && !configuredKey.isBlank()) {
            raw = normalizeKey(configuredKey.trim());
        } else {
            raw = sha256("resto-saas-dev-credential-key".getBytes(StandardCharsets.UTF_8));
            System.out.println("[CredentialCryptoService] WARNING: STORAGE_CREDENTIAL_KEY not set; using dev key. Do not use in production.");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /**
     * Resolve any configured value into a valid AES key length (16/24/32 bytes).
     * Tries base64 first; otherwise derives a 32-byte key via SHA-256.
     */
    private static byte[] normalizeKey(String configured) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configured);
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // not base64, fall through to hash
        }
        // Derive a stable 32-byte AES-256 key from the passphrase
        return sha256(configured.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(byte[] seed) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(seed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            new Random().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_BYTES + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(ct, 0, out, IV_BYTES, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt cloudinary secret", ex);
        }
    }

    public String decrypt(String ciphertextBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);
            if (combined.length <= IV_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted secret");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt cloudinary secret", ex);
        }
    }
}
