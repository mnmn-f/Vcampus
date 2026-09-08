package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.SQLException;

/** 统一控制提交与回滚；DAO不得自行提交业务事务。 */
public final class TransactionManager {
    private final JdbcConnectionFactory connections;

    public TransactionManager(JdbcConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("connections is required");
        }
        this.connections = connections;
    }

    public <T> T execute(TransactionWork<T> work) throws Exception {
        if (work == null) {
            throw new IllegalArgumentException("work is required");
        }
        try (Connection connection = connections.open()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                rollback(connection, ex);
                throw ex;
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean value) {
        try {
            connection.setAutoCommit(value);
        } catch (SQLException ignored) {
            // 连接即将关闭，原始业务异常更重要。
        }
    }
}

