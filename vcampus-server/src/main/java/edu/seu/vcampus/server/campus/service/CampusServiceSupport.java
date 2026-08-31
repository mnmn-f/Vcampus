package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.campus.repository.CampusRepositoryException;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** 事务、会话鉴权和基本输入校验的共享边界。 */
abstract class CampusServiceSupport {
    protected final TransactionManager transactions;

    CampusServiceSupport(TransactionManager transactions) { this.transactions = transactions; }

    protected <T> T execute(final Work<T> work) {
        try {
            if (transactions == null) return work.run(null);
            return transactions.execute(new TransactionWork<T>() {
                @Override
                public T execute(Connection connection) throws Exception {
                    return work.run(connection);
                }
            });
        } catch (CampusRepositoryException ex) {
            throw new CampusException(ex.getResultCode(), ex.getMessage(), ex);
        } catch (CampusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CampusException(CampusCommands.INTERNAL_ERROR, "教务扩展服务暂时不可用", ex);
        }
    }

    protected static void require(SessionContext session, Permission permission) {
        if (session == null) throw new CampusException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (permission == null || !session.allows(permission)) {
            throw new CampusException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    protected static void requireAny(SessionContext session, Permission first, Permission second) {
        if (session == null) throw new CampusException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(first) && !session.allows(second)) {
            throw new CampusException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    protected static void id(long value, String name) {
        if (value <= 0L) throw new CampusException(CampusCommands.INVALID_INPUT, name + "不正确");
    }

    protected static String text(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new CampusException(CampusCommands.INVALID_INPUT, name + "不能为空");
        }
        return value.trim();
    }

    protected static void page(int number, int size) {
        if (number <= 0 || size <= 0 || size > 100) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "分页参数不正确");
        }
    }

    protected interface Work<T> { T run(Connection connection) throws Exception; }
}
