package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConnect {

    private static final String URL = "jdbc:mysql://localhost:3306/spendingdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private static volatile Connection con;
    private static volatile Statement st;
    private static volatile boolean initialized = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // initialize once at startup if possible
            connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static synchronized void connect() throws SQLException {
        if (con == null || con.isClosed()) {
            con = DriverManager.getConnection(URL, USER, PASS);
            st = con.createStatement();
            System.out.println("DATABASE CONNECTED SUCCESSFULLY");
            
            // Initialize database and tables on first connection
            if (!initialized) {
                initializeDatabase();
                initialized = true;
            }
        }
    }

    private static void initializeDatabase() {
        try (Statement stmt = con.createStatement()) {
            // Create database if it doesn't exist
            stmt.execute("CREATE DATABASE IF NOT EXISTS spendingdb");
            
            // Use the database
            stmt.execute("USE spendingdb");
            
            // Create category_info table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS category_info (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "category VARCHAR(100) NOT NULL UNIQUE)");
            
            // Create spendings table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS spendings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "category VARCHAR(100) NOT NULL, " +
                    "sdate DATE NOT NULL, " +
                    "amount INT NOT NULL, " +
                    "FOREIGN KEY (category) REFERENCES category_info(category) ON DELETE CASCADE, " +
                    "INDEX idx_date (sdate))");
            
            // Insert default categories if they don't exist
            try {
                stmt.execute("INSERT IGNORE INTO category_info (category) VALUES ('Food')");
                stmt.execute("INSERT IGNORE INTO category_info (category) VALUES ('Transport')");
                stmt.execute("INSERT IGNORE INTO category_info (category) VALUES ('Entertainment')");
                stmt.execute("INSERT IGNORE INTO category_info (category) VALUES ('Utilities')");
                stmt.execute("INSERT IGNORE INTO category_info (category) VALUES ('Other')");
            } catch (SQLException ex) {
                // Ignore if categories already exist
            }
            
            System.out.println("DATABASE TABLES INITIALIZED SUCCESSFULLY");
        } catch (SQLException ex) {
            System.out.println("WARNING: Could not initialize database tables: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            connect();
            return con;
        } catch (SQLException ex) {
            // rethrow so callers can handle and show messages
            throw ex;
        }
    }

}
