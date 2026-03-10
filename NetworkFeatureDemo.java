package com.adrino.passmanager;

import java.util.concurrent.CountDownLatch;

public class NetworkFeatureDemo {

    public static void main(String[] args) throws InterruptedException {
        
        javafx.application.Platform.startup(() -> {
        });

        System.out.println("==========================================");
        System.out.println("   NETWORK FEATURE DEMO (Terminal View)   ");
        System.out.println("==========================================");

        CountDownLatch latch = new CountDownLatch(4);

        System.out.println("\n[1] Testing Local Info Fetch...");
        NetworkHelper.getLocalInfo(info -> {
            System.out.println("   >>> RESULT: Local Info: " + info);
            latch.countDown();
        });

        System.out.println("[2] Testing Public IP Fetch...");
        NetworkHelper.getPublicIP(ip -> {
            System.out.println("   >>> RESULT: Public IP: " + ip);
            latch.countDown();
        });

        System.out.println("[3] Testing Site Inspector (google.com)...");
        NetworkHelper.checkSiteStatus("google.com",
                status -> System.out.println("   [Status Update]: " + status),
                result -> {
                    System.out.println("   >>> RESULT: Success!\n" + indent(result));
                    latch.countDown();
                },
                error -> {
                    System.out.println("   >>> RESULT: Failed! " + error);
                    latch.countDown();
                });

        System.out.println("[4] Testing Site Inspector (invalid-site-url-example-123.com)...");
        NetworkHelper.checkSiteStatus("invalid-site-url-example-123.com",
                status -> System.out.println("   [Status Update (Invalid)]: " + status),
                result -> {
                    System.out.println("   >>> RESULT: Unexpected Success!\n" + indent(result));
                    latch.countDown();
                },
                error -> {
                    System.out.println("   >>> RESULT: Expected Failure Caught!\n" + indent(error));
                    latch.countDown();
                });

        latch.await();
        System.out.println("\n==========================================");
        System.out.println("   DEMO COMPLETE");
        System.out.println("==========================================");
        System.exit(0);
    }

    private static String indent(String text) {
        return "       " + text.replace("\n", "\n       ");
    }
}