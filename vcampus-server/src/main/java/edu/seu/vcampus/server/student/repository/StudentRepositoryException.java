package edu.seu.vcampus.server.student.repository;

/** 学籍仓储访问失败；不把 SQL 细节泄露给客户端。 */
public final class StudentRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StudentRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public StudentRepositoryException(String message) {
        super(message);
    }
}
