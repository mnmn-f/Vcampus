package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.identity.repository.IdentityUserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** users/user_roles 的 MySQL PreparedStatement DAO。 */
public final class MySqlIdentityUserRepository implements IdentityUserRepository {
    private final MySqlIdentityUserQueryRepository queries = new MySqlIdentityUserQueryRepository();

    @Override public IdentityUserRecord findById(Connection c, long id, boolean lock) {
        return queries.findById(c, id, lock);
    }

    @Override public IdentityUserRecord findByAccount(Connection c, String account,
                                                                 boolean lock) {
        return queries.findByAccount(c, account, lock);
    }

    @Override public UserPage search(Connection c, UserQuery query) {
        return queries.search(c, query);
    }

    @Override public long insertStudent(Connection c, RegistrationRequest r, String hash) {
        String sql = "INSERT INTO users (username, password_hash, display_name, email, phone, status) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getAccount().trim());
            ps.setString(2, hash);
            ps.setString(3, r.getDisplayName().trim());
            nullable(ps, 4, r.getEmail());
            nullable(ps, 5, r.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new IdentityRepositoryException("用户编号生成失败");
                long id = keys.getLong(1);
                if (!assignRole(c, id, Role.STUDENT, 0L)) {
                    throw new IdentityRepositoryException("默认学生角色不存在");
                }
                return id;
            }
        } catch (SQLException ex) {
            throw duplicateOrFailure("注册用户失败", ex);
        }
    }

    @Override public boolean updateProfile(Connection c, long id, ProfileUpdateRequest r) {
        String sql = "UPDATE users SET display_name = ?, email = ?, phone = ?, avatar_url = ?, "
                + "version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getDisplayName().trim());
            nullable(ps, 2, r.getEmail());
            nullable(ps, 3, r.getPhone());
            nullable(ps, 4, r.getAvatarUrl());
            ps.setLong(5, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw duplicateOrFailure("修改用户资料失败", ex); }
    }

    @Override public boolean updatePassword(Connection c, long id, String oldHash, String newHash) {
        return updatePassword(c, id, oldHash, newHash, true);
    }

    @Override public boolean resetPassword(Connection c, long id, String newHash) {
        return updatePassword(c, id, null, newHash, false);
    }

    @Override public boolean updateStatus(Connection c, long id, String status) {
        String sql = "UPDATE users SET status = ?, version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("更新用户状态失败", ex); }
    }

    @Override public boolean assignRole(Connection c, long id, Role role, long operatorId) {
        String sql = "INSERT INTO user_roles (user_id, role_id, assigned_by) "
                + "SELECT ?, id, NULLIF(?, 0) FROM roles WHERE code = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, operatorId);
            ps.setString(3, role.name());
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            if (isDuplicate(ex)) return false;
            throw failure("分配角色失败", ex);
        }
    }

    @Override public boolean revokeRole(Connection c, long id, Role role) {
        String sql = "DELETE ur FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ? AND r.code = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, role.name());
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("撤销角色失败", ex); }
    }

    @Override public int countRoles(Connection c, long id) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = ? AND r.status = 'ACTIVE'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException ex) { throw failure("读取用户角色失败", ex); }
    }

    private boolean updatePassword(Connection c, long id, String oldHash, String newHash,
                                   boolean expected) {
        String sql = "UPDATE users SET password_hash = ?, version = version + 1 WHERE id = ?"
                + (expected ? " AND password_hash = ?" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, id);
            if (expected) ps.setString(3, oldHash);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("更新用户密码失败", ex); }
    }

    private static void nullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, value.trim());
    }

    private static IdentityRepositoryException duplicateOrFailure(String message, SQLException ex) {
        return isDuplicate(ex) ? new IdentityRepositoryException(ResultCodes.CONFLICT, "账号或资料已存在", ex)
                : failure(message, ex);
    }

    private static boolean isDuplicate(SQLException ex) { return "23000".equals(ex.getSQLState()); }

    private static IdentityRepositoryException failure(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
