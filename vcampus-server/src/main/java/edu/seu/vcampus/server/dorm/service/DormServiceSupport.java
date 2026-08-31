package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** 宿舍服务共享的事务、会话和输入校验边界。 */
abstract class DormServiceSupport {
    protected final TransactionManager transactions;

    DormServiceSupport(TransactionManager transactions) {
        this.transactions = transactions;
    }

    protected <T> T execute(final Work<T> work) {
        try {
            if (transactions == null) {
                return work.run(null);
            }
            return transactions.execute(new edu.seu.vcampus.server.db.TransactionWork<T>() {
                @Override
                public T execute(Connection connection) throws Exception {
                    return work.run(connection);
                }
            });
        } catch (DormException ex) {
            throw ex;
        } catch (DormRepositoryException ex) {
            throw new DormException(ex.getResultCode(), ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new DormException(DormCommands.INTERNAL_ERROR,
                    "宿舍服务暂时不可用", ex);
        }
    }

    protected static void require(SessionContext session, Permission permission) {
        if (session == null) {
            throw new DormException(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        if (permission == null || !session.allows(permission)) {
            throw new DormException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    protected static void requireAny(SessionContext session, Permission first, Permission second) {
        if (session == null) throw new DormException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(first) && !session.allows(second)) {
            throw new DormException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    protected static void id(long value, String field) {
        if (value <= 0) {
            throw new DormException(DormCommands.INVALID_INPUT, field + "不正确");
        }
    }

    protected static void page(int number, int size) {
        if (number <= 0 || size <= 0 || size > 100) {
            throw new DormException(DormCommands.INVALID_INPUT, "分页参数不正确");
        }
    }

    protected static String text(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new DormException(DormCommands.INVALID_INPUT, field + "不能为空");
        }
        return value.trim();
    }

    protected interface Work<T> {
        T run(Connection connection) throws Exception;
    }
}
