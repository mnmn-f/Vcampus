package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

/** 身份用例共享的鉴权、校验、事务和异常映射。 */
final class IdentityServiceSupport {
    private IdentityServiceSupport() { }

    static void require(SessionContext session, Permission permission)
            throws IdentityServiceException {
        if (session == null) throw error(ResultCodes.UNAUTHORIZED, "请先登录");
        if (!session.allows(permission)) throw error(ResultCodes.FORBIDDEN, "当前职责无权执行此操作");
    }

    static String text(String value, String field) throws IdentityServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw error(ResultCodes.INVALID_INPUT, field + "不能为空");
        }
        return value.trim();
    }

    static void length(String value, int max, String field) throws IdentityServiceException {
        if (value != null && value.length() > max) {
            throw error(ResultCodes.INVALID_INPUT, field + "长度超限");
        }
    }

    static void positive(long value, String field) throws IdentityServiceException {
        if (value <= 0L) throw error(ResultCodes.INVALID_INPUT, field + "必须为正数");
    }

    static void password(String password, String field) throws IdentityServiceException {
        text(password, field);
        if (password.length() < 8 || password.length() > 72
                || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")) {
            throw error(ResultCodes.INVALID_INPUT, field + "至少8位且须包含大小写字母和数字");
        }
    }

    static <T> T inTransaction(IdentityTransactionRunner runner, TransactionWork<T> work)
            throws IdentityServiceException {
        try {
            return runner.execute(work);
        } catch (IdentityServiceException ex) {
            throw ex;
        } catch (IdentityRepositoryException ex) {
            throw new IdentityServiceException(ex.getResultCode(), ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new IdentityServiceException(ResultCodes.INTERNAL_ERROR, "身份服务暂时不可用", ex);
        }
    }

    static void audit(IdentityRecordRepository repository, java.sql.Connection connection,
                      SessionContext actor, String action, String type, Long resourceId,
                      String detail) {
        repository.insertBusinessAudit(connection, actor, action, type, resourceId,
                "SUCCESS", detail);
    }

    static IdentityServiceException error(String code, String message) {
        return new IdentityServiceException(code, message);
    }
}
