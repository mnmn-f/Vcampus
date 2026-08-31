package edu.seu.vcampus.client.auth;

/** 客户端服务边界的可展示错误，不把底层异常直接暴露给界面。 */
public class ClientServiceException extends Exception {
    private final String code;

    public ClientServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ClientServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
