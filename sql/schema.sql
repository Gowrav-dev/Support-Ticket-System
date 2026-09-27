-- AI IT Helpdesk Database Schema
CREATE DATABASE IF NOT EXISTS ai_helpdesk;
USE ai_helpdesk;

DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS users;

-- 1. Users Table (Stores Employees and Support Staff)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER' -- 'USER' or 'SUPPORT_STAFF'
);

-- 2. Tickets Table (Includes assigned_to support employee)
CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'OTHER',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    ai_summary TEXT,
    ai_solution TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    assigned_to VARCHAR(100) DEFAULT NULL, -- Support staff employee handling this ticket
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample Accounts (Password for all: Password@123)
-- Hash for Password@123: $2a$10$7R0O15p9W4yJt6.4Y1B1s.29rI5y1L0G9S0D1E2F3G4H5I6J7K8L9
INSERT INTO users (id, username, password_hash, full_name, role) VALUES
(1, 'john_doe', '$2a$10$0kK868m0W242M01m6mU04uG1YFsqBv0C67a1YhX1L8mK1lB6jG.C.', 'John Doe', 'USER'),
(2, 'alex_support', '$2a$10$0kK868m0W242M01m6mU04uG1YFsqBv0C67a1YhX1L8mK1lB6jG.C.', 'Alex Rivera', 'SUPPORT_STAFF'),
(3, 'sarah_support', '$2a$10$0kK868m0W242M01m6mU04uG1YFsqBv0C67a1YhX1L8mK1lB6jG.C.', 'Sarah Chen', 'SUPPORT_STAFF');

-- Sample Tickets
INSERT INTO tickets (id, user_name, title, description, category, priority, ai_summary, ai_solution, status, assigned_to) VALUES
(1, 'John Doe', 'Cannot connect to Wi-Fi in conference room B', 'Laptop cannot find the corporate Wi-Fi network.', 'NETWORK', 'HIGH', 'User unable to detect corporate Wi-Fi in room B.', '1. Toggle Wi-Fi adapter off and on.\n2. Verify conference room access point is powered.\n3. Re-enter 802.1x network credentials.', 'IN_PROGRESS', 'Alex Rivera'),
(2, 'John Doe', 'External monitor blanking periodically', 'Dell monitor goes black every 10 minutes when connected to docking station.', 'HARDWARE', 'MEDIUM', 'Intermittent external display flickering via dock.', '1. Check USB-C cable connection.\n2. Power cycle docking station.\n3. Update display drivers.', 'OPEN', NULL);
