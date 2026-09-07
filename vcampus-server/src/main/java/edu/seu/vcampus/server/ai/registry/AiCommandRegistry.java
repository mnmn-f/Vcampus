package edu.seu.vcampus.server.ai.registry;

import edu.seu.vcampus.common.protocol.command.AiCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ai.handler.AiCommandHandler;
import edu.seu.vcampus.server.ai.handler.AiQueryCommandHandler;
import edu.seu.vcampus.server.ai.handler.AiLiveDataCommandHandler;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiModelConfig;
import edu.seu.vcampus.server.ai.model.ResponsesAiModel;
import edu.seu.vcampus.server.ai.repository.AiConversationRepository;
import edu.seu.vcampus.server.ai.repository.AiKnowledgeRepository;
import edu.seu.vcampus.server.ai.repository.AiToolRepository;
import edu.seu.vcampus.server.ai.repository.AiLiveDataRepository;
import edu.seu.vcampus.server.ai.repository.AiFeedbackRepository;
import edu.seu.vcampus.server.ai.service.AiAssistantService;
import edu.seu.vcampus.server.ai.service.AiConversationService;
import edu.seu.vcampus.server.ai.service.AiKnowledgeService;
import edu.seu.vcampus.server.ai.service.AiToolService;
import edu.seu.vcampus.server.ai.service.AiFeedbackService;
import edu.seu.vcampus.server.ai.tool.AiToolRegistry;
import edu.seu.vcampus.server.ai.tool.ToolBridge;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.router.CommandRouter;

/** AI 模块生产组合根；业务工具在既有模块注册完成后接入。 */
public final class AiCommandRegistry {
    private AiCommandRegistry() { }

    public static CommandRouter registerAll(CommandRouter router, TransactionManager transactions) {
        router.register(AiCommands.LIVE_COMPETITIONS_MINE,
                new AiLiveDataCommandHandler(new AiLiveDataRepository(), transactions));
        AiModel model = new ResponsesAiModel(AiModelConfig.load());
        AiConversationService conversations = new AiConversationService(
                new AiConversationRepository(), transactions, model.getModelName());
        AiKnowledgeService knowledge = new AiKnowledgeService(
                new AiKnowledgeRepository(), transactions);
        AiToolService toolLogs = new AiToolService(new AiToolRepository(), transactions);
        AiFeedbackService feedback = new AiFeedbackService(new AiFeedbackRepository(), transactions);
        AiAssistantService assistant = new AiAssistantService(conversations, knowledge, toolLogs,
                AiToolRegistry.campusDefaults(), new ToolBridge(router), model);

        router.register(AiCommands.QUERY, new AiQueryCommandHandler(assistant));
        register(router, AiCommands.PING, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.CANCEL, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.CONFIRM, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.SESSION_LIST, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.SESSION_CREATE, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.SESSION_HISTORY, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.SESSION_CLEAR, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.SESSION_RENAME, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.FEEDBACK_SAVE, Permission.AI_QUERY, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_LIST, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_SAVE, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_DELETE, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_TEST, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_VERSIONS, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.KNOWLEDGE_ROLLBACK, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.FEEDBACK_LIST, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.TOOL_STATUS, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        register(router, AiCommands.MONITOR, Permission.AI_KNOWLEDGE_MANAGE, assistant, conversations, knowledge, feedback);
        return router;
    }

    private static void register(CommandRouter router, String command, Permission permission,
            AiAssistantService assistant, AiConversationService conversations,
            AiKnowledgeService knowledge, AiFeedbackService feedback) {
        router.register(command, new AiCommandHandler(command, permission,
                assistant, conversations, knowledge, feedback));
    }
}
