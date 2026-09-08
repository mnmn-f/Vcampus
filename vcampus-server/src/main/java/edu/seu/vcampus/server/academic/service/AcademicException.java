package edu.seu.vcampus.server.academic.service;

/** 可安全返回客户端的教务业务错误。 */
public final class AcademicException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public AcademicException(String resultCode, String userMessage) {
        super(userMessage);
        this.resultCode = resultCode;
    }

    public AcademicException(String resultCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getUserMessage() {
        return getMessage();
    }
}
