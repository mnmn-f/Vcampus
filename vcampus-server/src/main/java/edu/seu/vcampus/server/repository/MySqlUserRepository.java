package edu.seu.vcampus.server.repository;

import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

/** MySQL 登录用户仓储；登录标识可为账号、学号或工号。 */
public final class MySqlUserRepository implements UserRepository {
    private static final String FIND_BY_IDENTIFIER =
            "SELECT u.id AS user_id, u.username AS account, u.password_hash, u.display_name, "
                    + "u.status FROM users u "
                    + "LEFT JOIN student_profiles sp ON sp.user_id = u.id "
                    + "LEFT JOIN teacher_profiles tp ON tp.user_id = u.id "
                    + "WHERE u.username = ? OR sp.student_no = ? OR tp.employee_no = ?";
    private static final String FIND_BY_ID =
            "SELECT id AS user_id, username AS account, password_hash, display_name, "
                    + "status FROM users WHERE id = ?";
    private static final String FIND_ROLES =
            "SELECT r.code AS role_name FROM user_roles ur "
                    + "JOIN roles r ON r.id = ur.role_id "
                    + "WHERE ur.user_id = ? AND r.status = 'ACTIVE' ORDER BY r.code";
    private static final String UPSERT_USER =
            "INSERT INTO users (id, username, password_hash, display_name, status) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "AS new ON DUPLICATE KEY UPDATE username = new.username, "
                    + "password_hash = new.password_hash, "
                    + "display_name = new.display_name, status = new.status";
    private static final String DELETE_ROLES =
            "DELETE FROM user_roles WHERE user_id = ?";
    private static final String INSERT_ROLE =
            "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
    private static final String FIND_ROLE_ID =
            "SELECT id FROM roles WHERE code = ? AND status = 'ACTIVE'";

    private final JdbcConnectionFactory connectionFactory;

    public MySqlUserRepository(JdbcConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("connectionFactory is required");
        }
        this.connectionFactory = connectionFactory;
    }
    @Override
    public UserRecord findByAccount(String account) {
        if (account == null || account.trim().isEmpty()) {
            return null;
        }
        return findByIdentifier(account.trim());
    }
    @Override
    public UserRecord findById(long userId) {
        if (userId <= 0) {
            return null;
        }
        return find(FIND_BY_ID, Long.valueOf(userId));
    }
    @Override
    public void save(UserRecord user) {
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
        try (Connection connection = connectionFactory.open()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                executeUserUpsert(connection, user);
                replaceRoles(connection, user);
                connection.commit();
            } catch (SQLException ex) {
                rollback(connection);
                throw ex;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("failed to save user", ex);
        }
    }
    private UserRecord find(String sql, Object parameter) {
        try (Connection connection = connectionFactory.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                UserRecord user = readUser(connection, resultSet);
                return user;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("failed to query user", ex);
        }
    }
    private UserRecord findByIdentifier(String identifier) {
        try (Connection connection = connectionFactory.open();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_IDENTIFIER)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            statement.setString(3, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                UserRecord found = null;
                while (resultSet.next()) {
                    UserRecord candidate = readUser(connection, resultSet);
                    if (found != null && found.getUserId() != candidate.getUserId()) {
                        return null;
                    }
                    found = candidate;
                }
                return found;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("failed to query user", ex);
        }
    }
    private UserRecord readUser(Connection connection, ResultSet resultSet)
            throws SQLException {
        long userId = resultSet.getLong("user_id");
        Set<Role> roles = readRoles(connection, userId);
        if (roles.isEmpty()) {
            throw new RepositoryException("user has no assigned role");
        }
        return new UserRecord(userId, resultSet.getString("account"),
                resultSet.getString("password_hash"),
                resultSet.getString("display_name"), roles,
                "ACTIVE".equalsIgnoreCase(resultSet.getString("status")));
    }

    private Set<Role> readRoles(Connection connection, long userId) throws SQLException {
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        try (PreparedStatement statement = connection.prepareStatement(FIND_ROLES)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        roles.add(Role.valueOf(resultSet.getString("role_name")));
                    } catch (IllegalArgumentException ex) {
                        throw new SQLException("unknown role in user_roles", ex);
                    }
                }
            }
        }
        return roles;
    }

    private static void executeUserUpsert(Connection connection, UserRecord user)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_USER)) {
            statement.setLong(1, user.getUserId());
            statement.setString(2, user.getAccount());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getDisplayName());
            statement.setString(5, user.isEnabled() ? "ACTIVE" : "DISABLED");
            statement.executeUpdate();
        }
    }

    private static void replaceRoles(Connection connection, UserRecord user)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_ROLES)) {
            delete.setLong(1, user.getUserId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_ROLE)) {
            for (Role role : user.getRoles()) {
                long roleId = findRoleId(connection, role);
                insert.setLong(1, user.getUserId());
                insert.setLong(2, roleId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static long findRoleId(Connection connection, Role role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_ROLE_ID)) {
            statement.setString(1, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("role is not present in database: " + role.name());
                }
                return resultSet.getLong(1);
            }
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 保留原始异常，避免回滚失败覆盖数据库错误。
        }
    }
}
