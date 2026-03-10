package com.adrino.passmanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultSyncServer extends UnicastRemoteObject implements IRemoteVaultSync {

    private static final long serialVersionUID = 1L;

    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_PORT = 4446;
    private static final int BROADCAST_INTERVAL_MS = 3000;

    private static final String RMI_SERVICE_NAME = "AdrVaultSync";
    private static final int RMI_PORT = 1099;

    private final Map<String, List<VaultEntryBean>> backupStore = new HashMap<>();

    protected VaultSyncServer() throws RemoteException {
        super();
    }

    @Override
    public String syncVault(String username,
            List<VaultEntryBean> entries,
            ISyncCallback callback) throws RemoteException {

        log("┌─ syncVault() invoked by user: " + username);
        log("│  Entries received: " + entries.size());

        try {
            int total = entries.size();

            callback.onProgress("Validating " + total + " entries…", 10);
            Thread.sleep(600);

            for (int i = 0; i < total; i++) {
                VaultEntryBean e = entries.get(i);
                if (e.getSiteName() == null || e.getSiteName().isEmpty()) {
                    callback.onError("Entry #" + (i + 1) + " has an empty site name.");
                    return "SYNC_FAILED";
                }
            }
            callback.onProgress("Validation passed ✓", 30);
            Thread.sleep(400);

            callback.onProgress("Storing entries on server…", 50);
            Thread.sleep(500);

            backupStore.put(username, new ArrayList<>(entries));

            callback.onProgress("Stored " + total + " entries for '" + username + "'", 80);
            Thread.sleep(300);

            String summary = String.format(
                    "Synced %d vault entries for user '%s' at %s",
                    total, username,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            callback.onProgress("Finalising…", 95);
            Thread.sleep(200);

            callback.onSyncComplete(summary);
            log("└─ Sync complete for " + username);
            return "SYNC_OK";

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            callback.onError("Server interrupted during sync.");
            return "SYNC_INTERRUPTED";
        }
    }

    @Override
    public String ping() throws RemoteException {
        String msg = "AdrVaultSync Server is alive — " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log("Ping received → responded.");
        return msg;
    }

    @Override
    public String sendMessage(String sender, String message) throws RemoteException {
        System.out.println("\n[MESSAGE FROM " + sender.toUpperCase() + "]: " + message);
        return "Server: Message Received!";
    }

    private static void startMulticastBroadcaster() {
        Thread broadcaster = new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket()) {
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                String announcement = "ADRINO_VAULT_SERVER:" + RMI_PORT;

                log("UDP Multicast broadcaster started");
                log("  Group : " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
                log("  Payload : \"" + announcement + "\"");

                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buf = announcement.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            buf, buf.length, group, MULTICAST_PORT);
                    socket.send(packet);
                    Thread.sleep(BROADCAST_INTERVAL_MS);
                }
            } catch (IOException | InterruptedException e) {
                log("Broadcaster stopped: " + e.getMessage());
            }
        }, "UDP-Broadcaster");
        broadcaster.setDaemon(true);
        broadcaster.start();
    }

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║   ADRINO VAULT SYNC SERVER (RMI + UDP Multicast) ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println();

        try {
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            log("RMI Registry created on port " + RMI_PORT);

            VaultSyncServer server = new VaultSyncServer();
            registry.rebind(RMI_SERVICE_NAME, server);
            log("Service '" + RMI_SERVICE_NAME + "' bound to registry ✓");

            startMulticastBroadcaster();

            log("");
            log("Server is READY — waiting for client connections…");
            log("Press Ctrl+C to stop.\n");

        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[Server " + ts + "] " + msg);
    }
}