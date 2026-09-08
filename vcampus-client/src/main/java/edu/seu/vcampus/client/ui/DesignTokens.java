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
    /**
     * 分区之间的分隔线。
     *
     * <p>比 {@link #BORDER} 深一档。BORDER 用来收表格边缘时够了，但拿它分隔两大块
     * 内容时和页面底色的明度差太小，扫一眼看不出「这里换了一件事」。</p>
     */
    public static final Color DIVIDER = new Color(0xBF, 0xCB, 0xC4);
    /** 表头底色：主色的极浅一档，让列头和数据行明确分层。 */
    public static final Color TABLE_HEADER_BACKGROUND = new Color(0xE7, 0xED, 0xDC);
    /** 表格行之间的横线，比 BORDER_LIGHT 深，行多时才分得清。 */
    public static final Color TABLE_GRID = new Color(0xD3, 0xDC, 0xD6);

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

    /**
     * 顶部任务标签用的衬线字体。
     *
     * <p>界面里唯一一处刻意换字体的地方：标签栏是「选哪个板块」的导航，用华文中宋
     * 和正文的雅黑拉开层次，比单纯加粗更容易一眼定位。其余所有文字仍走
     * {@link #FONT_FAMILY}，不要在别处用它。</p>
     *
     * <p>候选按「最想要 → 最保底」排：华文中宋在不同 Windows 语言版本下注册的族名
     * 可能是英文名也可能是中文名，两个都试；再往下退到宋体，最后退到 Java 的逻辑
     * 衬线字体——逻辑字体一定存在，所以这条链不会落空。</p>
     */
    private static final String SERIF_FAMILY = resolveFamily(
            new String[]{"STZhongsong", "华文中宋", "STSong", "SimSun", "宋体"}, Font.SERIF);

    private DesignTokens() {
    }

    public static Font regular(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font medium(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    /** 任务标签专用的华文中宋，取不到时按候选链回退。 */
    public static Font serif(int size) {
        return new Font(SERIF_FAMILY, Font.PLAIN, size);
    }

    public static Font serifBold(int size) {
        return new Font(SERIF_FAMILY, Font.BOLD, size);
    }

    private static String resolveFontFamily() {
        return resolveFamily(new String[]{"Microsoft YaHei UI", "Microsoft YaHei"}, Font.SANS_SERIF);
    }

    /**
     * 按优先级挑第一个系统真正装了的字体族。
     *
     * <p>直接 {@code new Font("华文中宋", ...)} 在字体缺失时不会报错，会静默退回
     * Dialog，界面上表现为「字体设置没生效」且无从排查，所以这里显式对着系统字体
     * 清单查一遍。</p>
     */
    private static String resolveFamily(String[] candidates, String fallback) {
        try {
            String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            for (String candidate : candidates) {
                for (String family : installed) {
                    if (candidate.equalsIgnoreCase(family)) return candidate;
                }
            }
        } catch (RuntimeException ignored) {
            // 无法读取字体清单时用 Java 逻辑字体，它一定存在。
        }
        return fallback;
    }
}
