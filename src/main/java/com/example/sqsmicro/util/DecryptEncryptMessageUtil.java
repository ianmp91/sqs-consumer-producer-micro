package com.example.sqsmicro.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Component
public class DecryptEncryptMessageUtil {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public DecryptEncryptMessageUtil(@Value("classpath:private_key_c.pem") Resource resourcePrivateKey,
                                     @Value("classpath:public_key_b.pem") Resource resourcePublicKey) throws Exception {
        if (!resourcePrivateKey.exists() && !resourcePublicKey.exists()) {
            throw new RuntimeException("FATAL ERROR: private & public KEY not found in src/main/resource");
        }
        loadPrivateKey(resourcePrivateKey);
        loadPublicKey(resourcePublicKey);
    }

    private void loadPrivateKey(Resource resource) throws Exception {
        try (Reader reader = new InputStreamReader(resource.getInputStream());
             PEMParser pemParser = new PEMParser(reader)) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair) {
                this.privateKey = converter.getKeyPair((PEMKeyPair) object).getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                this.privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new RuntimeException("The private_key_c.pem file does not have a supported format (expected PKCS#1 or PKCS#8)");
            }
        }
    }

    private void loadPublicKey(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream());
             PEMParser pemParser = new PEMParser(reader)) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair) {
                this.publicKey = converter.getKeyPair((PEMKeyPair) object).getPublic();
            } else if (object instanceof SubjectPublicKeyInfo) {
                this.publicKey = converter.getPublicKey((SubjectPublicKeyInfo) object);
            } else {
                throw new RuntimeException("The public_key_b.pem file does not have a supported format (expected PKCS#1 or PKCS#8)");
            }
        }
    }

    public String decryptHybrid(String encryptedPayload, String encryptedAesKeyBase64) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(Base64.getDecoder().decode(encryptedAesKeyBase64));
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedBytes = aesCipher.doFinal(Base64.getDecoder().decode(encryptedPayload));

        return new String(decryptedBytes);
    }

    public EncryptedMessageBundle encryptHybridWithPrivateKey(String plainTextPayload) throws Exception {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedPayloadBytes = aesCipher.doFinal(plainTextPayload.getBytes());
        String encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayloadBytes);

        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());
        String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

        return new EncryptedMessageBundle(encryptedPayloadBase64, encryptedAesKeyBase64);
    }

    public record EncryptedMessageBundle(String encryptedPayload, String encryptedKey) {}
}
