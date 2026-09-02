package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.modules.ai.AiChatPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgePanel;
import edu.seu.vcampus.client.view.modules.ai.AiMonitorPanel;
import edu.seu.vcampus.common.security.Role;

/** 学生对话与知识管理员工作台的统一模块入口。 */
public final class AiAssistantPage extends BasePage {
    public interface Factory {
        AiAssistantClientService create();
    }

    private AiAssistantPage(ClientSession session, Factory factory) {
        super(session, "校园助手", "");
        setHeaderContext("身份：" + session.getActiveRole().getDisplayName());
        AiAssistantClientService built = factory == null ? null : factory.create();
        AiAssistantClientService service = built == null
                ? new DisabledAiAssistantClientService() : built;
        if (session.getActiveRole() == Role.AI_KNOWLEDGE_ADMIN) {
            TaskTabs tabs = new TaskTabs();
            tabs.addTask("知识库管理", new AiKnowledgePanel(service));
            tabs.addTask("运行监控", new AiMonitorPanel(service));
            addBlock(tabs);
        } else {
            addBlock(new AiChatPanel(service));
        }
    }

    public static AiAssistantPage create(ClientSession session, Factory factory) {
        return new AiAssistantPage(session, factory);
    }
}
