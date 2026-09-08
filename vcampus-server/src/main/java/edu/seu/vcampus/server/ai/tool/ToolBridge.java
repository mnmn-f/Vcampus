package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.service.AiServiceException;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;

/** 通过原命令路由调用业务模块，复用其鉴权、事务和错误映射。 */
public final class ToolBridge {
    private final CommandRouter router;

    public ToolBridge(CommandRouter router) { this.router = router; }

    public boolean isAvailable(AiTool tool) {
        return tool != null && router.isRegistered(tool.getTargetCommand());
    }

    public Object execute(AiTool tool, String argumentsJson, SessionContext session) {
        if (!isAvailable(tool)) {
            throw new AiServiceException(ResultCodes.NOT_FOUND,
                    "对应业务模块尚未接入，当前工具不可用");
        }
        Message response = router.route(Message.request(tool.getTargetCommand(),
                session.getSessionToken(), tool.payload(argumentsJson)));
        if (response == null || !response.isSuccess()) {
            String code = response == null ? ResultCodes.INTERNAL_ERROR : response.getResultCode();
            String text = response == null ? "业务模块没有返回结果" : response.getUserMessage();
            throw new AiServiceException(code, text);
        }
        return response.getPayload();
    }
}
