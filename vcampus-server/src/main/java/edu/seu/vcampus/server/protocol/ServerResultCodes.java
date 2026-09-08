package edu.seu.vcampus.server.protocol;

/**
 * 服务端生成的协议错误码。
 *
 * <p>业务成功、鉴权和通用输入错误优先使用 common 模块中的 {@code ResultCodes}；
 * 本类只登记无法放入具体业务模块的服务端协议错误。</p>
 */
public final class ServerResultCodes {
    public static final String UNKNOWN_COMMAND = "COMMON.UNKNOWN_COMMAND";
    public static final String MALFORMED_REQUEST = "COMMON.MALFORMED_REQUEST";

    private ServerResultCodes() {
    }
}
