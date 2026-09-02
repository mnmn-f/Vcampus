package edu.seu.vcampus.server.ai.service;

/** AI 模块映射到稳定结果码的业务异常。 */
public final class AiServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String resultCode;

    public AiServiceException(String resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public AiServiceException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public String getResultCode() { return resultCode; }
}
