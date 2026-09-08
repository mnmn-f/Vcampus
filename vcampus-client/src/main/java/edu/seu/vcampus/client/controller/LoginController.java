package edu.seu.vcampus.client.controller;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.auth.ClientServiceException;
import edu.seu.vcampus.common.dto.auth.LoginResult;

import javax.swing.SwingWorker;

/** 登录控制器：编排异步认证，不让 View 直接调用认证服务。 */
public final class LoginController {
    public interface Callback {
        void onSuccess(LoginResult result);
        void onFailure(ClientServiceException error);
    }

    private final AuthClientService authService;

    public LoginController(AuthClientService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("authService 不能为空");
        }
        this.authService = authService;
    }

    public void login(final String account, final String password, final Callback callback) {
        new SwingWorker<LoginResult, Void>() {
            private ClientServiceException failure;

            @Override
            protected LoginResult doInBackground() {
                try {
                    return authService.login(account, password);
                } catch (ClientServiceException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    callback.onFailure(failure);
                    return;
                }
                try {
                    callback.onSuccess(get());
                } catch (Exception e) {
                    callback.onFailure(new ClientServiceException("COMMON.INTERNAL_ERROR",
                            "登录过程发生异常，请稍后重试。", e));
                }
            }
        }.execute();
    }
}
