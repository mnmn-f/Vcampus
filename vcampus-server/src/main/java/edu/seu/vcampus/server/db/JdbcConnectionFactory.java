package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL 连接创建边界。
 *
 * <p>默认值只用于本地开发，生产部署应通过 JVM 属性或环境变量注入配置：</p>
 * <ul>
 *     <li>{@code vcampus.db.url}/{@code VCAMPUS_DB_URL}</li>
 *     <li>{@code vcampus.db.user}/{@code VCAMPUS_DB_USER}</li>
 *     <li>{@code vcampus.db.password}/{@code VCAMPUS_DB_PASSWORD}</li>
 * </ul>
 */
public final class JdbcConnectionFactory {
    public static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/vcampus"
                    + "?useUnicode=true&characterEncoding=UTF-8"
                    + "&serverTimezone=Asia/Shanghai&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";

    private final String url;
    private final String username;
    private final String password;

    public JdbcConnectionFactory() {
        this(config("vcampus.db.url", "VCAMPUS_DB_URL", DEFAULT_URL),
                config("vcampus.db.user", "VCAMPUS_DB_USER", DEFAULT_USER),
                config("vcampus.db.password", "VCAMPUS_DB_PASSWORD", DEFAULT_PASSWORD));
    }

    public JdbcConnectionFactory(String url, String username, String password) {
        this.url = requireText(url, "url");
        this.username = requireText(username, "username");
        this.password = password == null ? "" : password;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /** 与常见 JDBC 命名保持一致。 */
    public Connection getConnection() throws SQLException {
        return open();
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    private static String config(String property, String environment,
                                 String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environment);
        }
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
