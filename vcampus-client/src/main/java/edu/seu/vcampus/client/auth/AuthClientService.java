package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;

/** 登录、登出等身份边界，视图不直接接触 Socket。 */
public interface AuthClientService {
    LoginResult login(String account, String password) throws ClientServiceException;

    void logout();

    Role switchRole(Role role) throws ClientServiceException;

    boolean isLoggedIn();
}
