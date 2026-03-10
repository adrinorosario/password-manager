package com.adrino.passmanager;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VaultEntryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int entryId;
    private int userId;
    private String siteName;
    private String siteUsername;
    private String encryptedPassword;
    private String iv;
    private String createdAt;
    private int strengthScore;

    public VaultEntryBean() {
        this.entryId = -1;
        this.userId = -1;
        this.siteName = "";
        this.siteUsername = "";
        this.encryptedPassword = "";
        this.iv = "";
        this.createdAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.strengthScore = 0;
    }

    public VaultEntryBean(int entryId, int userId, String siteName,
            String siteUsername, String encryptedPassword,
            String iv, int strengthScore) {
        this.entryId = entryId;
        this.userId = userId;
        this.siteName = siteName;
        this.siteUsername = siteUsername;
        this.encryptedPassword = encryptedPassword;
        this.iv = iv;
        this.createdAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.strengthScore = strengthScore;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteUsername() {
        return siteUsername;
    }

    public void setSiteUsername(String siteUsername) {
        this.siteUsername = siteUsername;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getStrengthScore() {
        return strengthScore;
    }

    public void setStrengthScore(int strengthScore) {
        this.strengthScore = strengthScore;
    }

    @Override
    public String toString() {
        return String.format(
                "VaultEntryBean { id=%d, userId=%d, site='%s', user='%s', strength=%d, created='%s' }",
                entryId, userId, siteName, siteUsername, strengthScore, createdAt);
    }
}