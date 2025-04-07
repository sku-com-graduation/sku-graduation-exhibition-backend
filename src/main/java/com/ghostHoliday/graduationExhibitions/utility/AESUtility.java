package com.ghostHoliday.graduationExhibitions.utility;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AESUtility {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    // 키 생성
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    // 키를 Base64로 인코딩
    public static String encodeKey(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    // Base64 → SecretKey
    public static SecretKey decodeKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    // 암호화: IV + 암호문 → Base64
    public static String encrypt(String data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        // 랜덤 IV 생성
        byte[] ivBytes = new byte[IV_SIZE];
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encrypted = cipher.doFinal(data.getBytes());

        // IV + 암호문을 Base64로 결합
        byte[] combined = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(ivBytes, 0, combined, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    // 복호화: Base64 → IV + 암호문 → 복호화
    public static String decrypt(String encryptedData, SecretKey secretKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // IV 분리
        byte[] ivBytes = new byte[IV_SIZE];
        byte[] encryptedBytes = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, 0, ivBytes, 0, IV_SIZE);
        System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted);
    }

    public static String encryptDeterministic(String data, SecretKey secretKey) throws Exception {
        // data 기반으로 IV 생성 (계속 결정론적으로 유지)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        byte[] ivBytes = Arrays.copyOf(hash, IV_SIZE);  // 16바이트
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // IV + 암호문을 합쳐서 Base64 인코딩
        byte[] combined = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decryptDeterministic(String encryptedData, SecretKey secretKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // IV 추출
        byte[] ivBytes = Arrays.copyOfRange(combined, 0, IV_SIZE);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        // 암호문 추출
        byte[] encrypted = Arrays.copyOfRange(combined, IV_SIZE, combined.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

}