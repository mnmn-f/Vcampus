package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** 登录页品牌侧栏：品牌主视觉与桌宠松鼠在绿区中央组合展示。 */
public final class LoginBrandPanel extends JPanel {
    private static final String MASCOT_RESOURCE =
            "/edu/seu/vcampus/client/pet/squirrel-login-wave.png";

    public LoginBrandPanel() {
        super(new BorderLayout());
        setBackground(DesignTokens.PRIMARY);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(36, 48, 36, 48));
        JPanel mark = new JPanel(new BorderLayout(13, 0));
        mark.setOpaque(false);
        mark.add(new JLabel(LineIcon.brand(Color.WHITE, 38)), BorderLayout.WEST);
        JLabel university = new JLabel("SEU");
        university.setFont(DesignTokens.medium(38));
        university.setForeground(Color.WHITE);
        mark.add(university, BorderLayout.CENTER);
        add(mark, BorderLayout.NORTH);
        add(new BrandHero(), BorderLayout.CENTER);
    }

    private static final class BrandHero extends JLayeredPane {
        private static final int IMAGE_WIDTH = 330;
        private static final int IMAGE_HEIGHT = 226;
        private final JLabel mascot = new JLabel(loadMascot());
        private final JLabel title = new JLabel("VCampus", JLabel.CENTER);
        private final JLabel subtitle = new JLabel("东南大学校园服务", JLabel.CENTER);

        private BrandHero() {
            setOpaque(false);
            setPreferredSize(new Dimension(430, 410));
            mascot.setName("loginMascot");
            title.setName("loginBrandTitle");
            title.setFont(DesignTokens.medium(48));
            title.setForeground(Color.WHITE);
            subtitle.setFont(DesignTokens.regular(17));
            subtitle.setForeground(new Color(0xE8, 0xF5, 0xEF));
            add(title, Integer.valueOf(1));
            add(subtitle, Integer.valueOf(1));
            add(mascot, Integer.valueOf(2));
        }

        @Override public void doLayout() {
            int centerX = getWidth() / 2;
            int groupHeight = 315;
            int top = Math.max(8, (getHeight() - groupHeight) / 2);
            mascot.setBounds(centerX - IMAGE_WIDTH / 2, top, IMAGE_WIDTH, IMAGE_HEIGHT);
            // 松鼠底部压住字的上沿，使撑在透明横线上的手自然落到字面上。
            title.setBounds(centerX - 190, top + IMAGE_HEIGHT - 25, 380, 66);
            subtitle.setBounds(centerX - 180, top + IMAGE_HEIGHT + 43, 360, 28);
        }
    }

    private static ImageIcon loadMascot() {
        try {
            if (LoginBrandPanel.class.getResource(MASCOT_RESOURCE) == null) return new ImageIcon();
            BufferedImage source = ImageIO.read(LoginBrandPanel.class.getResource(MASCOT_RESOURCE));
            BufferedImage transparent = removeConnectedCheckerboard(source);
            int cropHeight = Math.max(1, transparent.getHeight() - transparent.getHeight() / 14);
            BufferedImage cropped = transparent.getSubimage(0, 0, transparent.getWidth(), cropHeight);
            BufferedImage scaled = new BufferedImage(BrandHero.IMAGE_WIDTH, BrandHero.IMAGE_HEIGHT,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(cropped.getScaledInstance(BrandHero.IMAGE_WIDTH,
                        BrandHero.IMAGE_HEIGHT, Image.SCALE_SMOOTH), 0, 0, null);
            } finally { graphics.dispose(); }
            return new ImageIcon(scaled);
        } catch (IOException | RuntimeException ignored) {
            return new ImageIcon();
        }
    }

    /** 原图带烘焙棋盘格，只清除与边缘连通的灰白格，保留眼白等内部白色。 */
    private static BufferedImage removeConnectedCheckerboard(BufferedImage source) {
        int width = source.getWidth(); int height = source.getHeight(); int size = width * height;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy = result.createGraphics();
        try { copy.drawImage(source, 0, 0, null); } finally { copy.dispose(); }
        boolean[] visited = new boolean[size]; int[] queue = new int[size]; int head = 0; int tail = 0;
        for (int x = 0; x < width; x++) {
            tail = seed(result, x, 0, width, visited, queue, tail);
            tail = seed(result, x, height - 1, width, visited, queue, tail);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = seed(result, 0, y, width, visited, queue, tail);
            tail = seed(result, width - 1, y, width, visited, queue, tail);
        }
        while (head < tail) {
            int value = queue[head++]; int x = value % width; int y = value / width;
            result.setRGB(x, y, 0);
            if (x > 0) tail = seed(result, x - 1, y, width, visited, queue, tail);
            if (x + 1 < width) tail = seed(result, x + 1, y, width, visited, queue, tail);
            if (y > 0) tail = seed(result, x, y - 1, width, visited, queue, tail);
            if (y + 1 < height) tail = seed(result, x, y + 1, width, visited, queue, tail);
        }
        return result;
    }

    private static int seed(BufferedImage image, int x, int y, int width,
                            boolean[] visited, int[] queue, int tail) {
        int index = y * width + x;
        if (visited[index]) return tail;
        visited[index] = true;
        int rgb = image.getRGB(x, y); int red = (rgb >>> 16) & 255;
        int green = (rgb >>> 8) & 255; int blue = rgb & 255;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        if (max - min <= 12 && min >= 175) queue[tail++] = index;
        return tail;
    }
}
