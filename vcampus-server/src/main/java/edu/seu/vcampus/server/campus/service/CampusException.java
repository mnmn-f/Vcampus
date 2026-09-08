package edu.seu.vcampus.server.campus.service;

/** 教务扩展服务稳定业务异常。 */
public final class CampusException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public CampusException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public CampusException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return getMessage(); }
}
