package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.protocol.ResultCodes;

/** 可安全映射到统一协议的身份业务异常。 */
public class IdentityServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public IdentityServiceException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public IdentityServiceException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public IdentityServiceException(String message, Throwable cause) {
        this(ResultCodes.INTERNAL_ERROR, message, cause);
    }

    public String getResultCode() { return resultCode; }
    public String getUserMessage() { return getMessage(); }
}
