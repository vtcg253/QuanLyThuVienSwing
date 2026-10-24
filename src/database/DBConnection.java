package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL", "jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        String username = setting("DB_USERNAME", "thuthu");
        String password = setting("DB_PASSWORD", "123456");
        if (username.isBlank() || password.isBlank()) {
            throw new SQLException("Hãy cấu hình DB_USERNAME và DB_PASSWORD trước khi chạy ứng dụng.");
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
