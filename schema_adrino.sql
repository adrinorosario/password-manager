CREATE DATABASE IF NOT EXISTS adrino_db;
USE adrino_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER', -- 'ADMIN' or 'USER'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vault_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    site_name VARCHAR(100) NOT NULL,
    site_username VARCHAR(100),
    encrypted_password TEXT NOT NULL,
    iv VARCHAR(255) NOT NULL, -- Initialization vector for AES
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Default Admin (Password: admin123)
-- Hash generated will need to match the Java implementation (SHA-256)
-- We will handle seeding in Java to ensure hash consistency
