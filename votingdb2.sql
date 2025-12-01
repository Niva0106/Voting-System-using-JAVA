CREATE DATABASE IF NOT EXISTS votingdb2;
USE votingdb2;

-- ------------------------
-- ADMIN TABLE
-- ------------------------
CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL
);

INSERT INTO admin (username, password)
VALUES ('admin', 'admin123')
ON DUPLICATE KEY UPDATE username='admin', password='admin123';    -- default admin credentials

-- ------------------------
-- VOTERS TABLE
-- ------------------------
CREATE TABLE IF NOT EXISTS voters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    has_voted BOOLEAN DEFAULT 0,
    verified BOOLEAN DEFAULT 0,
    dob DATE
);

-- ------------------------
-- CANDIDATES TABLE
-- ------------------------
CREATE TABLE IF NOT EXISTS candidates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    age INT,
    position VARCHAR(100),
    photo LONGBLOB,          -- stores actual photo binary
    bio TEXT,
    votes INT DEFAULT 0
);

-- ------------------------
-- VOTING STATUS TABLE
-- ------------------------
CREATE TABLE IF NOT EXISTS voting_status (
    id INT PRIMARY KEY,
    is_active BOOLEAN DEFAULT 0
);

INSERT INTO voting_status (id, is_active)
VALUES (1, 0)
ON DUPLICATE KEY UPDATE is_active=0;

-- ------------------------
-- CLEAN START (OPTIONAL)
-- ------------------------
DELETE FROM candidates;
DELETE FROM voters;

ALTER TABLE candidates AUTO_INCREMENT = 1;
ALTER TABLE voters AUTO_INCREMENT = 1;

UPDATE voting_status SET is_active = 0;
