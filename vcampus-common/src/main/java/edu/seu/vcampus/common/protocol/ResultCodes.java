package edu.seu.vcampus.common.protocol;

/** 客户端与服务端共享的标准结果码。 */
public final class ResultCodes {
    public static final String OK = "OK";
    public static final String INVALID_INPUT = "COMMON.INVALID_INPUT";
    public static final String UNAUTHORIZED = "AUTH.UNAUTHORIZED";
    public static final String FORBIDDEN = "AUTH.FORBIDDEN";
    public static final String INVALID_CREDENTIALS = "AUTH.INVALID_CREDENTIALS";
    public static final String ACCOUNT_DISABLED = "AUTH.ACCOUNT_DISABLED";
    public static final String NOT_FOUND = "COMMON.NOT_FOUND";
    public static final String CONFLICT = "COMMON.CONFLICT";
    public static final String INTERNAL_ERROR = "COMMON.INTERNAL_ERROR";

    private ResultCodes() {
    }
}
