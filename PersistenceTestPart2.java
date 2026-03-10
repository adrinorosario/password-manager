package com.adrino.passmanager;

public class PersistenceTestPart2 {
    public static void main(String[] args) {
        System.out.println("TEST: Persistence Part 2 (Login verification)");
        DatabaseHandler.initDB(); 

        String u = "persistUser";
        String p = "persistPass123";

        System.out.println("Attempting Login for '" + u + "'...");
        DatabaseHandler.User user = DatabaseHandler.login(u, p);

        if (user != null) {
            System.out.println("PERSISTENCE SUCCESS: User found. ID=" + user.id);
        } else {
            System.out.println("PERSISTENCE FAIL: User not found.");

            System.out.println("Dumping all local users:");
            for (DatabaseHandler.User ex : DatabaseHandler.getAllUsers()) {
                System.out.println(" - " + ex.username + " (" + ex.role + ")");
            }
        }
    }
}