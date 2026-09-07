package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.modules.ai.AiChatPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgePanel;
import edu.seu.vcampus.client.view.modules.ai.AiMonitorPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgeTestPanel;
import edu.seu.vcampus.client.view.modules.ai.AiFeedbackPanel;
import edu.seu.vcampus.client.view.modules.ai.AiToolStatusPanel;
import edu.seu.vcampus.client.view.pet.PetActivityListener;
import edu.seu.vcampus.common.security.Role;

import java.awt.BorderLayout;
import java.awt.Component;

/** 学生对话与知识管理员工作台的统一模块入口。 */
public final class AiAssistantPage extends BasePage {
    public interface Factory {
        AiAssistantClientService create();
    }

    private AiAssistantPage(ClientSession session, Factory factory,
                            PetActivityListener petActivity) {
        super(session, "校园助手", "");
        setHeaderContext("身份：" + session.getActiveRole().getDisplayName());
        AiAssistantClientService built = factory == null ? null : factory.create();
        AiAssistantClientService service = built == null
                ? new DisabledAiAssistantClientService() : built;
        if (session.getActiveRole() == Role.AI_KNOWLEDGE_ADMIN) {
            TaskTabs tabs = new TaskTabs();
            tabs.addTask("知识库管理", new AiKnowledgePanel(service));
            tabs.addTask("知识测试", new AiKnowledgeTestPanel(service));
            tabs.addTask("用户反馈", new AiFeedbackPanel(service));
            tabs.addTask("工具状态", new AiToolStatusPanel(service));
            tabs.addTask("运行监控", new AiMonitorPanel(service));
            addBlock(tabs);
        } else {
            installFixedChat(new AiChatPanel(service, petActivity));
        }
    }

    /**
     * 对话页内部已经由消息历史承担滚动，因此移除 BasePage 的整页滚动层。
     * 这样顶部模式栏和底部输入区固定，只有中间的历史消息会滚动。
     */
    private void installFixedChat(AiChatPanel chat) {
        BorderLayout layout = (BorderLayout) getLayout();
        Component scrollingContent = layout.getLayoutComponent(BorderLayout.CENTER);
        if (scrollingContent != null) remove(scrollingContent);
        chat.setMinimumSize(new java.awt.Dimension(0, 0));
        add(chat, BorderLayout.CENTER);
    }

    public static AiAssistantPage create(ClientSession session, Factory factory) {
        return new AiAssistantPage(session, factory, null);
    }

    public static AiAssistantPage create(ClientSession session, Factory factory,
                                         PetActivityListener petActivity) {
        return new AiAssistantPage(session, factory, petActivity);
    }
}
