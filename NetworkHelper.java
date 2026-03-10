package com.adrino.passmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.application.Platform;

public class NetworkHelper {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void resolveSite(String host_or_url, Consumer<String> onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                String host = cleanUrl(host_or_url);
                System.out.println("[Network] Resolving IP for host: " + host);

                long start = System.currentTimeMillis();
                InetAddress address = InetAddress.getByName(host);
                long duration = System.currentTimeMillis() - start;

                String result = String.format("Host: %s\nIP: %s\nLookup Time: %dms",
                        address.getHostName(), address.getHostAddress(), duration);

                System.out.println("[Network] Resolved: " + address.getHostAddress());

                Platform.runLater(() -> onSuccess.accept(result));
            } catch (UnknownHostException e) {
                System.err.println("[Network] Unknown host: " + e.getMessage());
                Platform.runLater(() -> onError.accept("Could not resolve host: " + e.getMessage()));
            } catch (Exception e) {
                System.err.println("[Network] Error resolving: " + e.getMessage());
                Platform.runLater(() -> onError.accept("Error: " + e.getMessage()));
            }
        });
    }

    public static void checkSiteStatus(String siteUrl, Consumer<String> onStatus, Consumer<String> onSuccess,
            Consumer<String> onError) {
        executor.submit(() -> {
            HttpURLConnection connection = null;
            try {
                String validUrl = formatUrl(siteUrl);

                Platform.runLater(() -> onStatus.accept("Resolving DNS..."));
                System.out.println("[Network] Resolving DNS for: " + validUrl);
                URL url = java.net.URI.create(validUrl).toURL();
                InetAddress address = InetAddress.getByName(url.getHost()); 

                Platform.runLater(() -> onStatus.accept("Connecting (" + address.getHostAddress() + ")..."));
                System.out.println("[Network] Connecting to: " + address.getHostAddress());

                long start = System.currentTimeMillis();

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                connection.connect();

                Platform.runLater(() -> onStatus.accept("Handshaking..."));

                int code = connection.getResponseCode();
                String msg = connection.getResponseMessage();
                String type = connection.getContentType();
                long latency = System.currentTimeMillis() - start;

                StringBuilder sb = new StringBuilder();
                sb.append("URL: ").append(validUrl).append("\n");
                sb.append("IP: ").append(address.getHostAddress()).append("\n");
                sb.append("Status: ").append(code).append(" ").append(msg).append("\n");
                sb.append("Latency: ").append(latency).append(" ms\n");
                sb.append("Content-Type: ").append(type != null ? type : "Unknown");

                System.out.println("[Network] Check complete. Status: " + code + ", Latency: " + latency + "ms");

                Platform.runLater(() -> onSuccess.accept(sb.toString()));

            } catch (UnknownHostException e) {
                System.err.println("[Network] DNS Lookup failed: " + e.getMessage());
                Platform.runLater(() -> onError.accept("DNS Lookup Failed.\nHost: " + e.getMessage()
                        + "\nPlease check the URL or your internet connection."));
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[Network] Connection Timed out");
                Platform.runLater(() -> onError
                        .accept("Connection Timed Out.\nThe server took too long to respond.\nLatency > 5000ms"));
            } catch (IOException e) {
                System.err.println("[Network] Connection failed: " + e.getMessage());
                Platform.runLater(() -> onError.accept("Connection Failed: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void getPublicIP(Consumer<String> onResult) {
        executor.submit(() -> {
            try {
                System.out.println("[Network] Fetching public IP...");
                URL url = java.net.URI.create("https://api.ipify.org").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String ip = reader.readLine();
                reader.close();

                System.out.println("[Network] Public IP fetched: " + ip);
                Platform.runLater(() -> onResult.accept(ip));
            } catch (Exception e) {
                System.err.println("[Network] Failed to get public IP: " + e.getMessage());
                Platform.runLater(() -> onResult.accept("Unavailable"));
            }
        });
    }

    public static void getLocalInfo(Consumer<String> onResult) {
        executor.submit(() -> {
            try {
                InetAddress local = InetAddress.getLocalHost();
                String info = local.getHostName() + " (" + local.getHostAddress() + ")";
                Platform.runLater(() -> onResult.accept(info));
            } catch (Exception e) {
                Platform.runLater(() -> onResult.accept("Unknown"));
            }
        });
    }

    public static void checkPwdBreach(String sha1Prefix, Consumer<java.util.List<String>> onResult) {
        executor.submit(() -> {
            try {
                String apiUrl = "https://api.pwnedpasswords.com/range/" + sha1Prefix;
                System.out.println("[Network] Checking Breach API: " + apiUrl);

                URL url = java.net.URI.create(apiUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    java.util.List<String> entries = new java.util.ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        entries.add(line);
                    }
                    reader.close();
                    Platform.runLater(() -> onResult.accept(entries));
                } else {
                    System.err.println("[Network] Breach API Failed: " + conn.getResponseCode());
                    Platform.runLater(() -> onResult.accept(java.util.Collections.emptyList()));
                }
            } catch (Exception e) {
                System.err.println("[Network] Breach Check Error: " + e.getMessage());
                Platform.runLater(() -> onResult.accept(java.util.Collections.emptyList()));
            }
        });
    }

    private static String cleanUrl(String rawUrl) throws IOException {
        String urlStr = rawUrl.trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = "http://" + urlStr;
        }
        URL url = java.net.URI.create(urlStr).toURL();
        return url.getHost();
    }

    private static String formatUrl(String rawUrl) {
        String urlStr = rawUrl.trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return "https://" + urlStr;
        }
        return urlStr;
    }
}