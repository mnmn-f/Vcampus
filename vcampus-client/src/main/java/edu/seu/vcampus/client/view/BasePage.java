package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.FeedbackBanner;
import edu.seu.vcampus.client.ui.components.PageHeader;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** 所有工作区页面的公共布局与页面内反馈。 */
public abstract class BasePage extends JPanel {
    protected final ClientSession session;
    protected final JPanel body;
    protected final FeedbackBanner feedback;
    private final PageHeader pageHeader;

    protected BasePage(ClientSession session, String title, String description) {
        super(new BorderLayout());
        this.session = session;
        setBackground(DesignTokens.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(DesignTokens.SPACE_24,
                DesignTokens.SPACE_32, DesignTokens.SPACE_24, DesignTokens.SPACE_32));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        pageHeader = new PageHeader(title, description);
        header.add(pageHeader, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        body = UiFactory.vertical(DesignTokens.SPACE_16);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, DesignTokens.SPACE_24, 0));
        body.setMinimumSize(new Dimension(0, 0));
        feedback = new FeedbackBanner();

        JPanel content = new JPanel(new BorderLayout(0, DesignTokens.SPACE_12));
        content.setOpaque(false);
        content.setMinimumSize(new Dimension(0, 0));
        content.add(feedback, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    protected void addBlock(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        body.add(component);
    }

    protected void info(String text) {
        feedback.show(FeedbackBanner.Type.INFO, text);
    }

    protected void success(String text) {
        feedback.show(FeedbackBanner.Type.SUCCESS, text);
    }

    protected void warning(String text) {
        feedback.show(FeedbackBanner.Type.WARNING, text);
    }

    protected void error(String text) {
        feedback.show(FeedbackBanner.Type.ERROR, text);
    }

    /** 供页面组合组件使用的明确反馈边界。 */
    public final void showInfo(String text) {
        info(text);
    }

    public final void showSuccess(String text) {
        success(text);
    }

    public final void showWarning(String text) {
        warning(text);
    }

    public final void showError(String text) {
        error(text);
    }

    protected final void setHeaderContext(String text) {
        pageHeader.setContext(text);
    }
}
