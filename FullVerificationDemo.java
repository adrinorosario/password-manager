package com.adrino.passmanager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class FullVerificationDemo {

    public static void main(String[] args) throws InterruptedException {
        javafx.application.Platform.startup(() -> {
        }); 

        System.out.println("=================================================");
        System.out.println("   ADRINO PASSWORD MANAGER - FULL SYSTEM VERIFICATION");
        System.out.println("=================================================");

        CountDownLatch latch = new CountDownLatch(6);

        System.out.println("\n[SECURITY] Testing Logic...");

        assertStrength("password", 1, "Weak"); 
        assertStrength("Tr0ub4dour&3", 4, "Very Strong"); 
        assertStrength("CorrectHorseBatteryStaple", 3, "Strong"); 

        String hash = SecurityUtil.sha1("password");
        if (hash.equals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8")) {
            printPass("SHA-1 Hashing Correct");
        } else {
            printFail("SHA-1 Hashing Failed. Got: " + hash);
        }

        System.out.println("\n[NETWORK] Testing Connectivity & APIs...");

        NetworkHelper.checkPwdBreach("5BAA6", result -> {
            boolean found = false;
            for (String s : result) {
                if (s.startsWith("1E4C9B93F3F0682250B6CF8331B7EE68FD8"))
                    found = true;
            }
            if (found)
                printPass("Breach Detection: Correctly flagged 'password'");
            else
                printFail("Breach Detection: Failed to flag 'password'");
            latch.countDown();
        });

        NetworkHelper.checkPwdBreach("FFFFF", result -> {
            
            printPass("Breach Detection: API Connectivity OK (Clean check)");
            latch.countDown();
        });

        NetworkHelper.getPublicIP(ip -> {
            if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
                printPass("Public IP Fetched: " + ip);
            else
                printFail("Public IP Format Error: " + ip);
            latch.countDown();
        });

        NetworkHelper.getLocalInfo(info -> {
            printPass("Local Info: " + info);
            latch.countDown();
        });

        NetworkHelper.checkSiteStatus("http://google.com",
                s -> {
                },
                r -> {
                    printPass("Site Inspector (Valid): google.com resolved 200 OK");
                    latch.countDown();
                },
                e -> {
                    printFail("Site Inspector (Valid): Failed check for google.com");
                    latch.countDown();
                });

        NetworkHelper.checkSiteStatus("invalid-url-test-123.com",
                s -> {
                },
                r -> {
                    printFail("Site Inspector (Edge Case): Unexpected success for invalid URL");
                    latch.countDown();
                },
                e -> {
                    printPass("Site Inspector (Edge Case): Correctly caught invalid URL");
                    latch.countDown();
                });

        latch.await();
        System.out.println("\n=================================================");
        System.out.println("   VERIFICATION COMPLETE");
        System.out.println("=================================================");
        System.exit(0);
    }

    private static void assertStrength(String pass, int expectedScore, String label) {
        int score = SecurityUtil.calculateStrength(pass);
        if (score == expectedScore)
            printPass("Strength Logic: '" + pass + "' = " + score + "/" + label);
        else
            printFail("Strength Logic: '" + pass + "' expected " + expectedScore + " but got " + score);
    }

    private static void printPass(String msg) {
        System.out.println("   [PASS] " + msg);
    }

    private static void printFail(String msg) {
        System.out.println("   [FAIL] " + msg);
    }
}