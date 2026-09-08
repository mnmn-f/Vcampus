package edu.seu.vcampus.server.dorm.repository;

/** 仓储层可识别的业务冲突，不泄露 SQL 异常给客户端。 */
public final class DormRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public DormRepositoryException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public DormRepositoryException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
}
