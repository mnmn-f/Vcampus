package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

/** 宿舍设置卡片的公共布局和按钮样式。 */
final class DormExtSettingsSupport {
    private DormExtSettingsSupport() { }

    static JPanel wrap(SectionCard card, JPanel fields, JButton primary, JButton secondary) {
        JPanel actions = UiFactory.horizontal(8);
        actions.add(secondary);
        actions.add(primary);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel result = new JPanel(new BorderLayout());
        result.setOpaque(false);
        result.add(card, BorderLayout.CENTER);
        return result;
    }

    static JButton button(String label, boolean primary, Runnable action) {
        JButton result = primary ? new PrimaryButton(label) : new SecondaryButton(label);
        result.addActionListener((ActionListener) event -> action.run());
        return result;
    }
}
