package com.adrino.passmanager;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DatabaseHandler {

    private static final String URL = "jdbc:mysql://localhost:3306/adrino_db?allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true&connectTimeout=3000&socketTimeout=5000";
    private static final String USER = "root";
    private static final String PASS = "password";

    private static boolean useLocalStorage = false;
    private static final String LOCAL_DB_FILE = "adrino_local.dat";

    private static List<User> localUsers = new ArrayList<>();
    private static List<VaultItem> localVault = new ArrayList<>();
    private static AtomicInteger userIdCounter = new AtomicInteger(1);
    private static AtomicInteger vaultIdCounter = new AtomicInteger(1);

    public static void initDB() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                Statement stmt = conn.createStatement()) {

            System.out.println("Connected to MySQL.");

            String sqlUser = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) DEFAULT 'USER', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(sqlUser);

            String sqlVault = "CREATE TABLE IF NOT EXISTS vault_items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "site_name VARCHAR(100) NOT NULL, " +
                    "site_username VARCHAR(100), " +
                    "encrypted_password TEXT NOT NULL, " +
                    "iv VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            stmt.executeUpdate(sqlVault);

            seedAdmin(conn);

        } catch (SQLException e) {
            System.err.println("MySQL Connection failed (" + e.getMessage() + "). Switching to Offline Mode.");
            useLocalStorage = true;
            loadLocalDB();
        }
    }

    private static void seedAdmin(Connection conn) throws SQLException {
        String checkSql = "SELECT count(*) FROM users WHERE username = 'admin'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String hash = SecurityUtil.hashPassword("admin123");
                String insert = "INSERT INTO users (username, password_hash, role) VALUES ('admin', '" + hash
                        + "', 'ADMIN')";
                stmt.executeUpdate(insert);
                System.out.println("Admin seeded (MySQL).");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static User login(String username, String password) {
        if (useLocalStorage) {
            return localUsers.stream()
                    .filter(u -> u.username.equals(username) && u.password.equals(SecurityUtil.hashPassword(password)))
                    .findFirst()
                    .orElse(null);
        }

        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String inputHash = SecurityUtil.hashPassword(password);
                if (storedHash.equals(inputHash)) {
                    return new User(rs.getInt("id"), rs.getString("username"), null,
                            Role.valueOf(rs.getString("role")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean registerUser(String username, String password) {
        if (useLocalStorage) {
            if (localUsers.stream().anyMatch(u -> u.username.equals(username)))
                return false;
            localUsers.add(new User(userIdCounter.getAndIncrement(), username, SecurityUtil.hashPassword(password),
                    Role.USER));
            saveLocalDB();
            return true;
        }

        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'USER')";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, SecurityUtil.hashPassword(password));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<User> getAllUsers() {
        if (useLocalStorage)
            return new ArrayList<>(localUsers);

        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(rs.getInt("id"), rs.getString("username"), null, Role.valueOf(rs.getString("role"))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deleteUser(int userId) {
        if (useLocalStorage) {
            localUsers.removeIf(u -> u.id == userId);
            localVault.removeIf(v -> v.userId == userId);
            saveLocalDB();
            return;
        }

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<VaultItem> getVaultItems(int userId) {
        if (useLocalStorage) {
            return localVault.stream()
                    .filter(v -> v.userId == userId)
                    .collect(Collectors.toList());
        }

        List<VaultItem> list = new ArrayList<>();
        String sql = "SELECT * FROM vault_items WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String decryptedPass = SecurityUtil.decrypt(rs.getString("encrypted_password"), rs.getString("iv"));
                list.add(new VaultItem(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("site_name"),
                        rs.getString("site_username"),
                        decryptedPass));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void addVaultItem(int userId, String site, String username, String password) {
        if (useLocalStorage) {
            localVault.add(new VaultItem(vaultIdCounter.getAndIncrement(), userId, site, username, password));
            saveLocalDB();
            return;
        }

        String[] enc = SecurityUtil.encrypt(password);
        String sql = "INSERT INTO vault_items (user_id, site_name, site_username, encrypted_password, iv) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, site);
            pstmt.setString(3, username);
            pstmt.setString(4, enc[0]);
            pstmt.setString(5, enc[1]);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteVaultItem(int itemId) {
        if (useLocalStorage) {
            localVault.removeIf(v -> v.id == itemId);
            saveLocalDB();
            return;
        }

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM vault_items WHERE id=?")) {
            pstmt.setInt(1, itemId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadLocalDB() {
        File f = new File(LOCAL_DB_FILE);
        if (!f.exists()) {

            localUsers.add(new User(userIdCounter.getAndIncrement(), "admin", SecurityUtil.hashPassword("admin123"),
                    Role.ADMIN));
            System.out.println("Initialized Local DB with Admin.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            localUsers = (List<User>) ois.readObject();
            localVault = (List<VaultItem>) ois.readObject();
            userIdCounter.set(localUsers.stream().mapToInt(u -> u.id).max().orElse(0) + 1);
            vaultIdCounter.set(localVault.stream().mapToInt(v -> v.id).max().orElse(0) + 1);
            System.out.println("Loaded Local DB.");
        } catch (Exception e) {
            System.err.println("Error loading local DB: " + e.getMessage());
        }
    }

    private static void saveLocalDB() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOCAL_DB_FILE))) {
            oos.writeObject(localUsers);
            oos.writeObject(localVault);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class User implements Serializable {
        public int id;
        public String username;
        public String password;
        public Role role;

        public User(int id, String u, String p, Role r) {
            this.id = id;
            this.username = u;
            this.password = p;
            this.role = r;
        }

        public String getUsername() {
            return username;
        }

        public Role getRole() {
            return role;
        }
    }

    public enum Role {
        ADMIN, USER
    }

    public static class VaultItem implements Serializable {
        public int id;
        public int userId;
        public String site;
        public String username;
        public String password;

        public VaultItem(int id, int uid, String s, String u, String p) {
            this.id = id;
            this.userId = uid;
            this.site = s;
            this.username = u;
            this.password = p;
        }

        public String getSite() {
            return site;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}