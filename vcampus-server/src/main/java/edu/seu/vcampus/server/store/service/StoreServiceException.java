package edu.seu.vcampus.server.store.service;

/** 可安全映射为统一协议错误的商店业务异常。 */
public final class StoreServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public StoreServiceException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public StoreServiceException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return getMessage(); }
}
