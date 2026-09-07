package edu.seu.vcampus.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;

/** VCampus 的 Swing 视觉令牌。 */
public final class DesignTokens {
    public static final Color PRIMARY = new Color(0x60, 0x78, 0x30);
    public static final Color PRIMARY_HOVER = new Color(0x72, 0x87, 0x48);
    public static final Color PRIMARY_PRESSED = new Color(0x4F, 0x62, 0x28);
    public static final Color PRIMARY_LIGHT = new Color(0xF0, 0xF3, 0xE9);
    public static final Color PRIMARY_BORDER = new Color(0xC7, 0xD0, 0xB1);
    public static final Color GOLD = new Color(0xC8, 0xA7, 0x5D);

    public static final Color PAGE_BACKGROUND = new Color(0xF4, 0xF6, 0xF2);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(0x24, 0x2A, 0x20);
    public static final Color TEXT_SECONDARY = new Color(0x68, 0x70, 0x63);
    public static final Color TEXT_PLACEHOLDER = new Color(0x9A, 0xA5, 0xA0);
    public static final Color BORDER = new Color(0xD9, 0xE1, 0xDD);
    public static final Color BORDER_LIGHT = new Color(0xEA, 0xEF, 0xEC);

    public static final Color SUCCESS = new Color(0x1A, 0x8F, 0x5A);
    public static final Color SUCCESS_BACKGROUND = new Color(0xEF, 0xFA, 0xF3);
    public static final Color WARNING = new Color(0xA6, 0x72, 0x00);
    public static final Color WARNING_BACKGROUND = new Color(0xFF, 0xF8, 0xE6);
    public static final Color ERROR = new Color(0xC9, 0x2A, 0x2A);
    public static final Color ERROR_BACKGROUND = new Color(0xFF, 0xF1, 0xF0);
    public static final Color INFO = new Color(0x1D, 0x62, 0x8F);
    public static final Color INFO_BACKGROUND = new Color(0xF0, 0xF8, 0xFF);

    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;

    public static final int RADIUS = 10;
    public static final Dimension BUTTON_SIZE = new Dimension(96, 34);
    public static final Dimension LARGE_BUTTON_SIZE = new Dimension(120, 38);
    public static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

    private static final String FONT_FAMILY = resolveFontFamily();

    private DesignTokens() {
    }

    public static Font regular(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font medium(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    private static String resolveFontFamily() {
        final String[] preferred = {"Microsoft YaHei UI", "Microsoft YaHei",
                "Noto Sans CJK SC", "Source Han Sans SC", "WenQuanYi Micro Hei"};
        try {
            for (String wanted : preferred)
                for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames())
                    if (wanted.equalsIgnoreCase(family)) return family;
        } catch (RuntimeException ignored) {
            // 无法读取字体清单时使用 Java 逻辑无衬线字体。
        }
        return Font.SANS_SERIF;
    }
}
