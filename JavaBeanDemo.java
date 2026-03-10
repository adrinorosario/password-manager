package com.adrino.passmanager;

import java.io.*;

public class JavaBeanDemo {

    private static final String SERIAL_FILE = "/tmp/vault_entry_bean.ser";

    public static void main(String[] args) {

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║       ADRINO PASSWORD MANAGER — JavaBean Demo     ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("▸ Step 1 : Creating VaultEntryBean via no-arg constructor + setters");
        VaultEntryBean bean1 = new VaultEntryBean();
        bean1.setEntryId(101);
        bean1.setUserId(1);
        bean1.setSiteName("github.com");
        bean1.setSiteUsername("adrino");
        bean1.setEncryptedPassword("aGVsbG93b3JsZA==");
        bean1.setIv("c29tZUl2VmFsdWU=");
        bean1.setStrengthScore(3);

        printBean("Bean-1 (setter-built)", bean1);

        System.out.println("▸ Step 2 : Creating VaultEntryBean via parameterised constructor");
        VaultEntryBean bean2 = new VaultEntryBean(
                102, 1, "stackoverflow.com", "adrino_dev",
                "ZW5jcnlwdGVk", "aXZTdHJpbmc=", 4);
        printBean("Bean-2 (ctor-built)", bean2);

        System.out.println("▸ Step 3 : Updating siteName of Bean-2 via setter");
        System.out.println("  Before : getSiteName() → " + bean2.getSiteName());
        bean2.setSiteName("gitlab.com");
        System.out.println("  After  : getSiteName() → " + bean2.getSiteName());
        System.out.println();

        System.out.println("▸ Step 4 : Serialization → writing Bean-1 to file");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SERIAL_FILE))) {
            oos.writeObject(bean1);
            System.out.println("  ✓ Serialized to " + SERIAL_FILE);
        } catch (IOException e) {
            System.err.println("  ✗ Serialization failed: " + e.getMessage());
        }

        System.out.println();
        System.out.println("▸ Step 5 : Deserialization → reading Bean-1 back from file");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SERIAL_FILE))) {
            VaultEntryBean restored = (VaultEntryBean) ois.readObject();
            System.out.println("  ✓ Deserialized successfully");
            printBean("Restored Bean", restored);

            boolean match = restored.getEntryId() == bean1.getEntryId()
                    && restored.getSiteName().equals(bean1.getSiteName())
                    && restored.getSiteUsername().equals(bean1.getSiteUsername());
            System.out.println("  Round-trip integrity check : " + (match ? "✓ PASS" : "✗ FAIL"));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("  ✗ Deserialization failed: " + e.getMessage());
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println(" JavaBean demonstration complete.");
        System.out.println("═══════════════════════════════════════════════════");
    }

    private static void printBean(String label, VaultEntryBean b) {
        System.out.println("  ┌─ " + label + " ─────────────────────────");
        System.out.println("  │ getEntryId()           → " + b.getEntryId());
        System.out.println("  │ getUserId()             → " + b.getUserId());
        System.out.println("  │ getSiteName()           → " + b.getSiteName());
        System.out.println("  │ getSiteUsername()        → " + b.getSiteUsername());
        System.out.println("  │ getEncryptedPassword()  → " + b.getEncryptedPassword());
        System.out.println("  │ getIv()                 → " + b.getIv());
        System.out.println("  │ getStrengthScore()      → " + b.getStrengthScore());
        System.out.println("  │ getCreatedAt()          → " + b.getCreatedAt());
        System.out.println("  └──────────────────────────────────────────");
        System.out.println();
    }
}