package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;

/** 同时使用文字与色彩表达状态，避免只依赖颜色。 */
public final class StatusBadge extends JLabel {
    public enum Type { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

    public StatusBadge(String text, Type type) {
        super(text);
        setFont(DesignTokens.regular(12));
        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        apply(type);
    }

    public void apply(Type type) {
        Type actual = type == null ? Type.NEUTRAL : type;
        switch (actual) {
            case SUCCESS:
                setForeground(DesignTokens.SUCCESS);
                setBackground(DesignTokens.SUCCESS_BACKGROUND);
                break;
            case WARNING:
                setForeground(DesignTokens.WARNING);
                setBackground(DesignTokens.WARNING_BACKGROUND);
                break;
            case ERROR:
                setForeground(DesignTokens.ERROR);
                setBackground(DesignTokens.ERROR_BACKGROUND);
                break;
            case INFO:
                setForeground(DesignTokens.INFO);
                setBackground(DesignTokens.INFO_BACKGROUND);
                break;
            default:
                setForeground(DesignTokens.TEXT_SECONDARY);
                setBackground(new Color(0xF0, 0xF3, 0xF1));
                break;
        }
        setOpaque(true);
    }
}
