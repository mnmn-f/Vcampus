package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 宿舍服务共享的事务、会话和输入校验边界。 */
abstract class DormServiceSupport {
    private static final Logger LOGGER = Logger.getLogger(DormServiceSupport.class.getName());

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
            // 这一档兜住的是没人预料到的异常（SQL 写错、字段名对不上…），返回给客户端的
            // 只有一句「暂时不可用」。原始堆栈必须留在服务端日志里，否则界面上永远只
            // 看得到那一句话，谁也查不出真正坏在哪儿。
            LOGGER.log(Level.SEVERE, "宿舍服务执行失败", ex);
            throw new DormException(DormCommands.INTERNAL_ERROR, message(ex), ex);
        }
    }

    /**
     * 给这一类「没人预料到」的失败一句还能用的话。
     *
     * <p>光说「宿舍服务暂时不可用」，界面上看不出是网断了、库没迁移，还是 SQL 写错了。
     * 数据库拒绝写入时把它的原话带上一截：这个项目最常见的原因就是迁移脚本没跑全，
     * 而 MySQL 的报错里会直接点名是哪条约束。其余情况仍然只给那句通用的话——把随便
     * 什么异常的 message 抛到界面上，既看不懂也可能带出内部细节。</p>
     */
    private static String message(Exception ex) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof java.sql.SQLException)) cause = cause.getCause();
        if (cause == null || cause.getMessage() == null) return "宿舍服务暂时不可用";
        String detail = cause.getMessage().trim();
        if (detail.length() > 160) detail = detail.substring(0, 160) + "…";
        return "数据库拒绝了这次操作：" + detail
                + "（若提示某条 CHECK 约束，多半是 scripts/apply-dorm-migrations.sql 还没跑）";
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
