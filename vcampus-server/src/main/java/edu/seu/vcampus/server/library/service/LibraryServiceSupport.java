package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** 业务服务共享事务、权限和输入校验边界。 */
abstract class LibraryServiceSupport {
    protected final TransactionManager transactions;

    protected LibraryServiceSupport(TransactionManager transactions) {
        this.transactions = transactions;
    }

    protected <T> T execute(final Work<T> work) {
        try {
            if (transactions == null) return work.run(null);
            return transactions.execute(new TransactionWork<T>() {
                @Override
                public T execute(Connection connection) throws Exception {
                    return work.run(connection);
                }
            });
        } catch (LibraryServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LibraryServiceException(ResultCodes.INTERNAL_ERROR,
                    "图书馆服务暂时不可用", ex);
        }
    }

    protected static void require(SessionContext session, Permission permission) {
        if (session == null) throw new LibraryServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (permission == null || !session.allows(permission)) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "当前职责无权执行此操作");
        }
    }

    protected static void requireAny(SessionContext session, Permission first, Permission second) {
        if (session == null) throw new LibraryServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(first) && !session.allows(second)) {
            throw new LibraryServiceException(ResultCodes.FORBIDDEN, "当前职责无权执行此操作");
        }
    }

    protected static void id(long value, String message) {
        if (value <= 0) throw new LibraryServiceException(ResultCodes.INVALID_INPUT, message);
    }

    protected static void page(int number, int size) {
        if (number <= 0 || size <= 0 || size > 100) {
            throw new LibraryServiceException(ResultCodes.INVALID_INPUT, "分页参数不正确");
        }
    }

    protected static String text(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryServiceException(ResultCodes.INVALID_INPUT, field + "不能为空");
        }
        return value.trim();
    }

    protected interface Work<T> {
        T run(Connection connection) throws Exception;
    }
}
