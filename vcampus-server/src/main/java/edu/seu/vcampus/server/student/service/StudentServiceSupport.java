package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.StudentRepositoryException;

/** 学籍服务共享的会话、事务和文本校验工具。 */
final class StudentServiceSupport {
    private StudentServiceSupport() {
    }

    static void requirePermission(SessionContext session, Permission permission)
            throws StudentRecordException {
        if (session == null) {
            throw new StudentRecordException(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        if (!session.allows(permission)) {
            throw new StudentRecordException(ResultCodes.FORBIDDEN, "当前职责无权执行此操作");
        }
    }

    static <T> T inTransaction(StudentTransactionRunner runner, TransactionWork<T> work)
            throws StudentRecordException {
        try {
            return runner.execute(work);
        } catch (StudentRecordException ex) {
            throw ex;
        } catch (StudentRepositoryException ex) {
            throw new StudentRecordException(ResultCodes.INTERNAL_ERROR,
                    "学籍数据暂时无法访问", ex);
        } catch (Exception ex) {
            throw new StudentRecordException(ResultCodes.INTERNAL_ERROR,
                    "学籍操作暂时无法完成", ex);
        }
    }

    static String required(String value, String field) throws StudentRecordException {
        if (value == null || value.trim().isEmpty()) {
            throw new StudentRecordException(ResultCodes.INVALID_INPUT, field + "不能为空");
        }
        return value.trim();
    }

    static void maxLength(String value, int max, String field)
            throws StudentRecordException {
        if (value != null && value.length() > max) {
            throw new StudentRecordException(ResultCodes.INVALID_INPUT,
                    field + "长度不能超过" + max + "个字符");
        }
    }
}
