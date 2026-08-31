package edu.seu.vcampus.server.campus.repository;

/** 可向服务层传递的稳定仓储业务错误。 */
public final class CampusRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public CampusRepositoryException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public CampusRepositoryException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
}
