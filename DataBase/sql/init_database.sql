-- Create Database
CREATE DATABASE IF NOT EXISTS spendingdb;

-- Use the database
USE spendingdb;

-- Create category_info table
CREATE TABLE IF NOT EXISTS category_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL UNIQUE
);

-- Create spendings table with id column
CREATE TABLE IF NOT EXISTS spendings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    sdate DATE NOT NULL,
    amount INT NOT NULL,
    FOREIGN KEY (category) REFERENCES category_info(category) ON DELETE CASCADE,
    INDEX idx_date (sdate)
);

-- Insert default categories if not exists
INSERT IGNORE INTO category_info (category) VALUES ('Food');
INSERT IGNORE INTO category_info (category) VALUES ('Transport');
INSERT IGNORE INTO category_info (category) VALUES ('Entertainment');
INSERT IGNORE INTO category_info (category) VALUES ('Utilities');
INSERT IGNORE INTO category_info (category) VALUES ('Other');
