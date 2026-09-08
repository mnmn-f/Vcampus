package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.security.Role;

import java.sql.Connection;

/** users 与 user_roles 的用户写读边界。 */
public interface IdentityUserRepository {
    IdentityUserRecord findById(Connection connection, long userId, boolean forUpdate);
    IdentityUserRecord findByAccount(Connection connection, String account,
                                                boolean forUpdate);
    UserPage search(Connection connection, UserQuery query);
    long insertStudent(Connection connection, RegistrationRequest request, String passwordHash);
    boolean updateProfile(Connection connection, long userId, ProfileUpdateRequest request);
    boolean updatePassword(Connection connection, long userId, String expectedHash,
                           String newHash);
    boolean resetPassword(Connection connection, long userId, String newHash);
    boolean updateStatus(Connection connection, long userId, String status);
    boolean assignRole(Connection connection, long userId, Role role, long operatorId);
    boolean revokeRole(Connection connection, long userId, Role role);
    int countRoles(Connection connection, long userId);
}
