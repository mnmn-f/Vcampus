package edu.seu.vcampus.client.network;

/** 网络服务调用中的可展示错误。 */
public final class NetworkClientException extends Exception {
    private final String code;

    public NetworkClientException(String code, String message) {
        super(message);
        this.code = code;
    }

    public NetworkClientException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
