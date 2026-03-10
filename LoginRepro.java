package com.adrino.passmanager;

import java.io.File;

public class LoginRepro {
    public static void main(String[] args) {
        System.out.println("TEST: Login Issue Reproduction (Offline Mode)");

        DatabaseHandler.initDB();

        System.out.println("InitDB Complete. Checking if offline...");
        
        String testUser = "testuser_" + System.currentTimeMillis();
        String testPass = "Pass123!";

        System.out.println("Attempting Register: " + testUser);
        boolean reg = DatabaseHandler.registerUser(testUser, testPass);

        if (reg) {
            System.out.println("Registration SUCCESS.");
        } else {
            System.out.println("Registration FAILED.");
        }

        System.out.println("Attempting Login...");
        DatabaseHandler.User user = DatabaseHandler.login(testUser, testPass);

        if (user != null) {
            System.out.println("Login SUCCESS! Role: " + user.role);
        } else {
            System.out.println("Login FAILED (User is null).");

            System.out.println("Existing users:");
            for (DatabaseHandler.User u : DatabaseHandler.getAllUsers()) {
                System.out.println(" - " + u.username + " | Hash: " + u.password);
            }

            String hashedInput = SecurityUtil.hashPassword(testPass);
            System.out.println("Input Hash: " + hashedInput);
        }

        if (user != null) {
            DatabaseHandler.deleteUser(user.id);
            System.out.println("Test user deleted.");
        }
    }
}