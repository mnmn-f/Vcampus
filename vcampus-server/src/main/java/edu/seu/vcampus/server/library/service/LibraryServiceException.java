package edu.seu.vcampus.server.library.service;

/** 图书馆业务异常，携带可直接映射到 Message 的结果码。 */
public final class LibraryServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public LibraryServiceException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public LibraryServiceException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
}
