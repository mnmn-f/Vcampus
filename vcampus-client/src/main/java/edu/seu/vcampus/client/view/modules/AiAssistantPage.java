package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.modules.ai.AiChatPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgePanel;
import edu.seu.vcampus.client.view.modules.ai.AiMonitorPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgeTestPanel;
import edu.seu.vcampus.client.view.modules.ai.AiFeedbackPanel;
import edu.seu.vcampus.client.view.modules.ai.AiToolStatusPanel;
import edu.seu.vcampus.client.view.modules.ai.AiToolRouteTestPanel;
import edu.seu.vcampus.client.view.modules.ai.AiKnowledgeRegressionPanel;
import edu.seu.vcampus.client.view.pet.PetActivityListener;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

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
            AiKnowledgePanel knowledge = new AiKnowledgePanel(service);
            addAdminTask(tabs, "知识库管理", knowledge);
            addAdminTask(tabs, "知识测试", new AiKnowledgeTestPanel(service));
            addAdminTask(tabs, "批量回归", new AiKnowledgeRegressionPanel(service));
            addAdminTask(tabs, "用户反馈", new AiFeedbackPanel(service, id -> {
                tabs.setSelectedIndex(0); knowledge.openChunk(id);
            }));
            addAdminTask(tabs, "路由测试", new AiToolRouteTestPanel(service));
            addAdminTask(tabs, "工具状态", new AiToolStatusPanel(service));
            addAdminTask(tabs, "运行监控", new AiMonitorPanel(service));
            installFixedCenter(tabs);
        } else {
            installFixedCenter(new AiChatPanel(service, petActivity));
        }
    }

    /**
     * AI 页面自行管理滚动：学生端只滚动消息历史，管理员端在每个任务页内滚动。
     * 移除 BasePage 的外层滚动可避免嵌套视口压缩内容区。
     */
    private void installFixedCenter(Component content) {
        BorderLayout layout = (BorderLayout) getLayout();
        Component scrollingContent = layout.getLayoutComponent(BorderLayout.CENTER);
        if (scrollingContent != null) remove(scrollingContent);
        content.setMinimumSize(new Dimension(0, 0));
        add(content, BorderLayout.CENTER);
    }

    /** 管理员每个任务独立滚动，窄窗口也不会裁掉筛选项、表格或底部操作。 */
    private void addAdminTask(TaskTabs tabs, String title, JComponent content) {
        AdminScrollCanvas canvas = new AdminScrollCanvas();
        canvas.setOpaque(false);
        canvas.setBorder(BorderFactory.createEmptyBorder(20, 0, DesignTokens.SPACE_12, 0));
        content.setMinimumSize(new Dimension(0, 0));
        canvas.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(canvas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab(title, scroll);
    }

    /** 跟随视口宽度重新布局，同时保留内容真实高度供纵向滚动。 */
    private static final class AdminScrollCanvas extends JPanel implements Scrollable {
        private AdminScrollCanvas() { super(new BorderLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 32);
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static AiAssistantPage create(ClientSession session, Factory factory) {
        return new AiAssistantPage(session, factory, null);
    }

    public static AiAssistantPage create(ClientSession session, Factory factory,
                                         PetActivityListener petActivity) {
        return new AiAssistantPage(session, factory, petActivity);
    }
}
