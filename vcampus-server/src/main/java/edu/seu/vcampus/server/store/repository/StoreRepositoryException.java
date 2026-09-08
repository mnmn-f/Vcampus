package edu.seu.vcampus.server.store.repository;

/** DAO 不能安全转换为用户提示时使用的内部异常。 */
public final class StoreRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StoreRepositoryException(String message) { super(message); }

    public StoreRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
