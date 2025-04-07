package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.utility.AESUtility;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class EncryptionService {

    private SecretKey secretKey;

    @Value("${encryption.aes-secret-key}")
    private String encodedKey;

    @PostConstruct
    public void init() {
        this.secretKey = AESUtility.decodeKey(encodedKey);
    }

    public String encryptPrimaryKey(Long primaryKey) throws Exception {
        return AESUtility.encrypt(String.valueOf(primaryKey), secretKey);
    }

    public Long decryptPrimaryKey(String encryptedKey) throws Exception {
        String decryptedKey = AESUtility.decrypt(encryptedKey, secretKey);
        return Long.parseLong(decryptedKey);
    }

    public String encryptTeamId(Long teamId) throws Exception {
        return AESUtility.encryptDeterministic(String.valueOf(teamId), secretKey);
    }

    public Long decryptTeamId(String encryptedTeamId) throws Exception {
        String decrypted = AESUtility.decryptDeterministic(encryptedTeamId, secretKey);
        return Long.parseLong(decrypted);
    }
}
