package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

/** 页面内反馈条，统一承载成功、提示、警告和错误信息。 */
public class FeedbackBanner extends JPanel {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private final JLabel icon = new JLabel();
    private final JLabel message = new JLabel();

    public FeedbackBanner() {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        icon.setFont(DesignTokens.medium(14));
        message.setFont(DesignTokens.regular(13));
        add(icon, BorderLayout.WEST);
        add(message, BorderLayout.CENTER);
        setVisible(false);
    }

    public FeedbackBanner(Type type, String text) {
        this();
        show(type, text);
    }

    public void show(Type type, String text) {
        if (type == null) {
            type = Type.INFO;
        }
        icon.setText(iconText(type));
        message.setText(text == null ? "" : text);
        switch (type) {
            case SUCCESS:
                setBackground(DesignTokens.SUCCESS_BACKGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xB7, 0xE4, 0xC7)),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)));
                icon.setForeground(DesignTokens.SUCCESS);
                message.setForeground(DesignTokens.SUCCESS);
                break;
            case WARNING:
                setBackground(DesignTokens.WARNING_BACKGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF2, 0xD4, 0x8A)),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)));
                icon.setForeground(DesignTokens.WARNING);
                message.setForeground(DesignTokens.WARNING);
                break;
            case ERROR:
                setBackground(DesignTokens.ERROR_BACKGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xF1, 0xB7, 0xB3)),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)));
                icon.setForeground(DesignTokens.ERROR);
                message.setForeground(DesignTokens.ERROR);
                break;
            default:
                setBackground(DesignTokens.INFO_BACKGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xB9, 0xD9, 0xED)),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)));
                icon.setForeground(DesignTokens.INFO);
                message.setForeground(DesignTokens.INFO);
                break;
        }
        setVisible(true);
        revalidate();
        repaint();
    }

    public void hideBanner() {
        setVisible(false);
    }

    public String getMessageText() {
        return message.getText();
    }

    private String iconText(Type type) {
        switch (type) {
            case SUCCESS: return "✓";
            case WARNING: return "!";
            case ERROR: return "×";
            default: return "i";
        }
    }
}
