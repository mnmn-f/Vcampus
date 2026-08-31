package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

/** 登录页品牌侧栏，保持克制的东大绿色视觉识别。 */
public final class LoginBrandPanel extends JPanel {
    public LoginBrandPanel() {
        super(new BorderLayout());
        setBackground(DesignTokens.PRIMARY);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(52, 56, 52, 56));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel mark = new JPanel(new BorderLayout(13, 0));
        mark.setOpaque(false);
        mark.add(new JLabel(LineIcon.brand(Color.WHITE, 42)), BorderLayout.WEST);
        JLabel university = new JLabel("SEU");
        university.setFont(DesignTokens.medium(42));
        university.setForeground(Color.WHITE);
        mark.add(university, BorderLayout.CENTER);
        content.add(mark);
        content.add(Box.createVerticalStrut(24));
        JLabel title = new JLabel("VCampus");
        title.setFont(DesignTokens.medium(30));
        title.setForeground(Color.WHITE);
        content.add(title);
        content.add(Box.createVerticalStrut(12));
        JLabel subtitle = new JLabel("东南大学校园服务");
        subtitle.setFont(DesignTokens.regular(16));
        subtitle.setForeground(new Color(0xE8, 0xF5, 0xEF));
        content.add(subtitle);
        add(content, BorderLayout.NORTH);
    }
}
