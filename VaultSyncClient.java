package com.adrino.passmanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VaultSyncClient {

    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_PORT = 4446;
    private static final int DISCOVERY_TIMEOUT_MS = 10_000;

    private static final String RMI_SERVICE_NAME = "AdrVaultSync";

    static class SyncCallbackImpl extends UnicastRemoteObject implements ISyncCallback {
        private static final long serialVersionUID = 1L;

        protected SyncCallbackImpl() throws RemoteException {
            super();
        }

        @Override
        public void onProgress(String message, int percent) throws RemoteException {
            log("⟳  [" + percent + "%] " + message);
        }

        @Override
        public void onSyncComplete(String summary) throws RemoteException {
            log("✅  Sync complete → " + summary);
        }

        @Override
        public void onError(String errorMessage) throws RemoteException {
            log("❌  Error → " + errorMessage);
        }
    }

    private static String[] discoverServer() throws IOException {
        log("Listening for server via UDP Multicast…");
        log("  Group   : " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
        log("  Timeout : " + DISCOVERY_TIMEOUT_MS + " ms");

        @SuppressWarnings("deprecation")
        MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
        socket.joinGroup(group);
        socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT_MS) {
            try {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                String senderHost = packet.getAddress().getHostAddress();

                if (received.startsWith("ADRINO_VAULT_SERVER:")) {
                    log("✓ Received broadcast from " + senderHost + " → \"" + received + "\"");
                    socket.leaveGroup(group);
                    socket.close();
                    String rmiPort = received.split(":")[1];
                    return new String[] { senderHost, rmiPort };
                } else {
                    log("  (Ignored irrelevant traffic: \"" + received + "\")");
                }
            } catch (SocketTimeoutException e) {
            }
        }
        socket.leaveGroup(group);
        socket.close();
        throw new IOException("Server discovery timed out after " + DISCOVERY_TIMEOUT_MS + " ms");
    }

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║   ADRINO VAULT SYNC CLIENT (RMI + UDP Discover)  ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println();

        try {
            String[] serverInfo = discoverServer();
            String serverHost = serverInfo[0];
            int rmiPort = Integer.parseInt(serverInfo[1]);

            log("Looking up RMI service '" + RMI_SERVICE_NAME
                    + "' at " + serverHost + ":" + rmiPort + "…");

            Registry registry = LocateRegistry.getRegistry(serverHost, rmiPort);
            IRemoteVaultSync syncService = (IRemoteVaultSync) registry.lookup(RMI_SERVICE_NAME);

            String pong = syncService.ping();
            log("Ping response → " + pong);
            System.out.println();

            log("Preparing vault entries (VaultEntryBean)…");
            List<VaultEntryBean> entries = new ArrayList<>();

            VaultEntryBean e1 = new VaultEntryBean();
            e1.setEntryId(1);
            e1.setUserId(1);
            e1.setSiteName("github.com");
            e1.setSiteUsername("adrino");
            e1.setEncryptedPassword("enc_gh_pass_1234");
            e1.setIv("iv_gh_001");
            e1.setStrengthScore(SecurityUtil.calculateStrength("Str0ng!Pass"));
            entries.add(e1);

            VaultEntryBean e2 = new VaultEntryBean(
                    2, 1, "gmail.com", "adrino@gmail.com",
                    "enc_gm_pass_5678", "iv_gm_002", 4);
            entries.add(e2);

            VaultEntryBean e3 = new VaultEntryBean(
                    3, 1, "stackoverflow.com", "adrino_dev",
                    "enc_so_pass_9012", "iv_so_003", 3);
            entries.add(e3);

            for (VaultEntryBean entry : entries) {
                log("  → " + entry);
            }
            System.out.println();

            log("Calling syncVault() with RMI Callback…");
            ISyncCallback callback = new SyncCallbackImpl();

            String result = syncService.syncVault("adrino", entries, callback);
            System.out.println();
            log("Server returned: " + result);

            System.out.println();
            System.out.println("▸ Entering Interactive Messaging Mode...");
            System.out.println("  Commands: Type anything to send | 'exit' to quit");
            java.util.Scanner sc = new java.util.Scanner(System.in);
            while (true) {
                System.out.print("[Your Message]: ");
                String msg = sc.nextLine();
                if ("exit".equalsIgnoreCase(msg))
                    break;

                String serverResponse = syncService.sendMessage("Client-Terminal", msg);
                log("Server response: " + serverResponse);
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println(" Client finished.");
        System.out.println("═══════════════════════════════════════════════════");
    }

    private static void log(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[Client " + ts + "] " + msg);
    }
}