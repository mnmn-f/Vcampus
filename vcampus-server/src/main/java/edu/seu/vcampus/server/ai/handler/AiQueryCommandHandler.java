package edu.seu.vcampus.server.ai.handler;

import edu.seu.vcampus.common.ai.AiQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ai.service.AiAssistantService;
import edu.seu.vcampus.server.router.StreamWriter;
import edu.seu.vcampus.server.router.StreamingCommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** AI_QUERY 的多响应流式处理器。 */
public final class AiQueryCommandHandler implements StreamingCommandHandler {
    private final AiAssistantService service;

    public AiQueryCommandHandler(AiAssistantService service) { this.service = service; }
    public void handleStream(Message request, SessionContext session, StreamWriter writer) {
        service.query(request, request.getPayload() instanceof AiQuery
                ? (AiQuery) request.getPayload() : null, session, writer);
    }
    public Message handle(Message request, SessionContext session) { return null; }
    public Permission requiredPermission() { return Permission.AI_QUERY; }
    public boolean requiresAuthentication() { return true; }
}
