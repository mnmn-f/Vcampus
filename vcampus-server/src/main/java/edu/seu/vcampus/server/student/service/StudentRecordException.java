package edu.seu.vcampus.server.student.service;

/** 可安全返回给客户端的学籍业务失败。 */
public final class StudentRecordException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public StudentRecordException(String resultCode, String userMessage) {
        super(userMessage);
        this.resultCode = resultCode;
    }

    public StudentRecordException(String resultCode, String userMessage,
                                  Throwable cause) {
        super(userMessage, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return getMessage(); }
}
