package com.adrino.passmanager;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    
    private static final byte[] FIXED_KEY = "AdrinoSuperSecretKey2025".getBytes(); 
    
    private static final byte[] AES_KEY = new byte[16];

    static {
        System.arraycopy(FIXED_KEY, 0, AES_KEY, 0, 16);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static String[] encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            return new String[] {
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv)
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encryptedText, String ivStr) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(ivStr));
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] original = cipher.doFinal(decoded);

            return new String(original);
        } catch (Exception e) {
            e.printStackTrace();
            return "[Decryption Failed]";
        }
    }
    
    public static String sha1(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static int calculateStrength(String password) {
        if (password == null || password.isEmpty())
            return 0;

        int score = 0;
        int len = password.length();

        if (len >= 8)
            score++;
        if (len >= 12)
            score++;

        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (hasUpper && hasLower)
            score++;
        if (hasDigit && hasSpecial)
            score++;

        return Math.min(score, 4);
    }

    public static String getStrengthLabel(int score) {
        switch (score) {
            case 0:
                return "Very Weak";
            case 1:
                return "Weak";
            case 2:
                return "Medium";
            case 3:
                return "Strong";
            case 4:
                return "Very Strong";
            default:
                return "Unknown";
        }
    }
}