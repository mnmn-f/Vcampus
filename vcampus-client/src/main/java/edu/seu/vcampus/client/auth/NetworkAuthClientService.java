package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;

import java.io.IOException;

/** 真实 Socket 认证实现；业务规则仍由服务端负责。 */
public final class NetworkAuthClientService implements AuthClientService {
    private final ClientGateway gateway;
    private LoginResult current;

    public NetworkAuthClientService(ClientGateway gateway) {
        if (gateway == null) {
            throw new IllegalArgumentException("gateway 不能为空");
        }
        this.gateway = gateway;
    }

    @Override
    public LoginResult login(String account, String password) throws ClientServiceException {
        if (account == null || account.trim().length() == 0
                || password == null || password.length() == 0) {
            throw new ClientServiceException(ResultCodes.INVALID_INPUT, "请输入账号和密码");
        }
        Message request = Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest(account.trim(), password));
        try {
            Message response = gateway.send(request);
            if (response == null) {
                throw new ClientServiceException(ResultCodes.INTERNAL_ERROR, "服务器没有返回结果");
            }
            if (!response.isSuccess()) {
                throw new ClientServiceException(response.getResultCode(), response.getUserMessage());
            }
            if (!(response.getPayload() instanceof LoginResult)) {
                throw new ClientServiceException(ResultCodes.INTERNAL_ERROR, "登录响应格式不正确");
            }
            current = (LoginResult) response.getPayload();
            return current;
        } catch (ClientServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new ClientServiceException(ResultCodes.INTERNAL_ERROR,
                    "无法连接服务器，请检查地址和网络后重试", e);
        } catch (ClassNotFoundException e) {
            throw new ClientServiceException(ResultCodes.INTERNAL_ERROR,
                    "服务器响应版本不兼容，请联系管理员", e);
        }
    }

    @Override
    public void logout() {
        if (current != null) {
            Message request = Message.request(Commands.AUTH_LOGOUT,
                    current.getSessionToken(), null);
            try {
                gateway.send(request);
            } catch (Exception ignored) {
                // 本地会话仍然清除，避免因网络故障卡住界面。
            }
        }
        current = null;
    }

    @Override
    public Role switchRole(Role role) throws ClientServiceException {
        if (current == null || role == null) {
            throw new ClientServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        Message request = Message.request(Commands.AUTH_SWITCH_ROLE,
                current.getSessionToken(), new SwitchRoleRequest(role));
        try {
            Message response = gateway.send(request);
            if (!response.isSuccess() || !(response.getPayload() instanceof Role)) {
                throw new ClientServiceException(response.getResultCode(),
                        response.getUserMessage());
            }
            Role activeRole = (Role) response.getPayload();
            current = new LoginResult(current.getUserId(), current.getAccount(),
                    current.getDisplayName(), current.getRoles(), activeRole,
                    current.getSessionToken());
            return activeRole;
        } catch (ClientServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ClientServiceException(ResultCodes.INTERNAL_ERROR,
                    "职责切换失败，请检查网络后重试", ex);
        }
    }

    @Override
    public boolean isLoggedIn() {
        return current != null;
    }
}
