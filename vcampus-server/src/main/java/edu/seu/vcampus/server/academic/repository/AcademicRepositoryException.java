package edu.seu.vcampus.server.academic.repository;

/** 将底层 JDBC 错误封装在教务仓储边界内。 */
public final class AcademicRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AcademicRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public AcademicRepositoryException(String message) {
        super(message);
    }
}
