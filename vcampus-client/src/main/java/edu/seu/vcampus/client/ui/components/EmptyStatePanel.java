package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** 空列表状态，解释原因并给出下一步动作。 */
public class EmptyStatePanel extends JPanel {
    private final JLabel titleLabel;
    private final JLabel reasonLabel;

    public EmptyStatePanel(String title, String reason, javax.swing.JButton nextAction) {
        super(new BorderLayout(0, DesignTokens.SPACE_8));
        setOpaque(false);
        titleLabel = UiFactory.sectionTitle(title);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.CENTER);
        reasonLabel = UiFactory.muted(reason);
        reasonLabel.setHorizontalAlignment(JLabel.CENTER);
        add(reasonLabel, BorderLayout.SOUTH);
        if (nextAction != null) {
            JPanel action = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
            action.setOpaque(false);
            action.add(nextAction);
            add(action, BorderLayout.NORTH);
        }
        setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 16, 24, 16));
    }

    public void setCopy(String title, String reason) {
        titleLabel.setText(title == null ? "暂无记录" : title);
        reasonLabel.setText(reason == null ? "" : reason);
    }
}
