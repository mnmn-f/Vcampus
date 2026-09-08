package edu.seu.vcampus.server.repository;

/** 持久化失败的非受检异常，避免把 JDBC 细节泄漏到业务层。 */
public final class RepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
