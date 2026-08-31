package edu.seu.vcampus.server.auth;

/** 可安全返回给客户端的认证失败，不包含 JDBC 或密码校验细节。 */
public final class AuthenticationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public AuthenticationException(String resultCode, String userMessage) {
        super(userMessage);
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getUserMessage() {
        return getMessage();
    }
}
