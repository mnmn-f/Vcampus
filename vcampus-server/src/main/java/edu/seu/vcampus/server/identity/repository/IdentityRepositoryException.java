package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.protocol.ResultCodes;

/** 身份仓储可映射为安全业务错误的数据库异常。 */
public final class IdentityRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public IdentityRepositoryException(String message) {
        this(ResultCodes.INTERNAL_ERROR, message, null);
    }

    public IdentityRepositoryException(String resultCode, String message) {
        this(resultCode, message, null);
    }

    public IdentityRepositoryException(String message, Throwable cause) {
        this(ResultCodes.INTERNAL_ERROR, message, cause);
    }

    public IdentityRepositoryException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
}
