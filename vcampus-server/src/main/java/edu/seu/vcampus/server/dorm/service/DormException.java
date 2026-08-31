package edu.seu.vcampus.server.dorm.service;

/** 可安全映射为协议结果的宿舍业务异常。 */
public final class DormException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public DormException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public DormException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return getMessage(); }
}
