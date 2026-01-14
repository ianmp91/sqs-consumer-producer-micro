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
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Component
public class DecryptEncryptMessageUtil {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public DecryptEncryptMessageUtil(@Value("classpath:private_key_c.pem") Resource resourcePrivateKey) throws Exception {
        if (!resourcePrivateKey.exists()) {
            throw new RuntimeException("FATAL ERROR: private KEY not found in src/main/resource");
        }
        loadPrivateKey(resourcePrivateKey);
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

    public void loadPublicKey(String publicKeyContent) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 1. Limpieza preventiva: A veces el Config Server mete saltos de linea o espacios
        String cleanedKey = publicKeyContent
                .replace("\n", "")
                .replace("\r", "")
                .trim();

        // 2. DETECCIÓN: ¿Es formato PEM (con cabeceras) o Raw Base64?
        if (cleanedKey.startsWith("-----BEGIN")) {
            // Es PEM, usamos tu lógica de Bouncy Castle pero mejorada
            loadPublicKeyFromPem(publicKeyContent);
        } else {
            // 3. Es Raw Base64 (lo que tienes en tu YAML) -> Usamos Java Nativo
            byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(spec);
        }
    }

    // Extraemos tu lógica anterior a un método privado para mantener el código limpio
    private void loadPublicKeyFromPem(String pemContent) throws IOException {
        try (Reader reader = new StringReader(pemContent);
             PEMParser pemParser = new PEMParser(reader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof SubjectPublicKeyInfo) {
                // PKCS#8 (El estándar moderno)
                this.publicKey = converter.getPublicKey((SubjectPublicKeyInfo) object);
            } else if (object instanceof PEMKeyPair) {
                // Si por error pasaron un par de claves
                this.publicKey = converter.getKeyPair((PEMKeyPair) object).getPublic();
            } else if (object instanceof org.bouncycastle.asn1.pkcs.RSAPublicKey) {
                // CASO FALTANTE: PKCS#1 (-----BEGIN RSA PUBLIC KEY-----)
                // Bouncy Castle a veces devuelve esto directamente
                // Necesitamos convertirlo a SubjectPublicKeyInfo o procesarlo manualmente,
                // pero usualmente converter lo maneja si el objeto es correcto.
                // Para simplificar, si usas claves modernas, caerá en SubjectPublicKeyInfo.
                throw new RuntimeException("Format PKCS#1 detected but not implemented in this snippet.");
            } else {
                // Aquí es donde caía tu error antes porque 'object' era null o irreconocible sin cabeceras
                throw new RuntimeException("Formato de Public Key no reconocido o null. Objeto detectado: " + (object != null ? object.getClass().getName() : "null"));
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

    public EncryptedMessageBundle encryptHybridWithPublicKey(String plainTextPayload) throws Exception {

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
