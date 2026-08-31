package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 商店服务共享权限、输入和事务异常处理。 */
final class StoreServiceSupport {
    private StoreServiceSupport() { }

    static void requirePermission(SessionContext session, Permission permission)
            throws StoreServiceException {
        if (session == null) throw new StoreServiceException(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(permission)) {
            throw new StoreServiceException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
    }

    static void requireRole(SessionContext session, Role role) throws StoreServiceException {
        if (session == null || session.getActiveRole() != role) {
            throw new StoreServiceException(ResultCodes.FORBIDDEN, "当前职责不能执行此操作");
        }
    }

    static <T> T inTransaction(StoreTransactionRunner transactions, TransactionWork<T> work)
            throws StoreServiceException {
        try {
            return transactions.execute(work);
        } catch (StoreServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StoreServiceException(ResultCodes.INTERNAL_ERROR, "商店服务暂时不可用", ex);
        }
    }

    static String required(String value, String field) throws StoreServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, field + "不能为空");
        }
        return value.trim();
    }

    static void maxLength(String value, int max, String field) throws StoreServiceException {
        if (value != null && value.length() > max) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, field + "长度超限");
        }
    }

    static BigDecimal money(BigDecimal value, String field, boolean positive)
            throws StoreServiceException {
        if (value == null || (positive ? value.compareTo(BigDecimal.ZERO) <= 0
                : value.compareTo(BigDecimal.ZERO) < 0) || value.scale() > 2
                || value.precision() > 12) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, field + "金额不正确");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    static void quantity(int value) throws StoreServiceException {
        if (value < 1 || value > 999999) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "商品数量不正确");
        }
    }

    static long positiveId(long value, String field) throws StoreServiceException {
        if (value <= 0) throw new StoreServiceException(ResultCodes.INVALID_INPUT, field + "不正确");
        return value;
    }
}
