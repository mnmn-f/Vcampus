package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 页面标题区：只保留当前任务所需的信息层级。 */
public class PageHeader extends JPanel {
    private final JLabel titleLabel;
    private final JLabel descriptionLabel;
    private final JLabel contextLabel;

    public PageHeader(String title, String description) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0,
                DesignTokens.SPACE_16, 0));
        titleLabel = UiFactory.title(title);
        descriptionLabel = UiFactory.muted(description == null ? "" : description);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(descriptionLabel, BorderLayout.SOUTH);
        add(text, BorderLayout.WEST);
        contextLabel = UiFactory.muted("");
        add(contextLabel, BorderLayout.EAST);
    }

    public void setContext(String text) {
        contextLabel.setText(text == null ? "" : text);
    }
}
