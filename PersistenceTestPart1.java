package com.adrino.passmanager;

public class PersistenceTestPart1 {
    public static void main(String[] args) {
        System.out.println("TEST: Persistence Part 1 (Register User)");
        DatabaseHandler.initDB(); 

        String u = "persistUser";
        String p = "persistPass123";

        DatabaseHandler.User existing = DatabaseHandler.login(u, p);
        if (existing != null) {
            DatabaseHandler.deleteUser(existing.id);
            System.out.println("Removed existing user.");
        }

        boolean reg = DatabaseHandler.registerUser(u, p);
        System.out.println("Registered '" + u + "': " + reg);

        DatabaseHandler.User user = DatabaseHandler.login(u, p);
        System.out.println("Login Check: " + (user != null ? "SUCCESS" : "FAIL"));

        System.out.println("Exiting Part 1. Data should be saved.");
    }
}