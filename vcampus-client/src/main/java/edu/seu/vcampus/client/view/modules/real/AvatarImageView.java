package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;

/** 个人中心头像：有地址时异步读取，失败或为空时显示姓名首字。 */
final class AvatarImageView extends JLabel {
    private static final int SIZE = 96;
    private String requestedUrl;

    AvatarImageView() {
        setHorizontalAlignment(SwingConstants.CENTER); setVerticalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(SIZE, SIZE)); setMinimumSize(getPreferredSize());
        setFont(DesignTokens.medium(28)); setForeground(Color.WHITE); setOpaque(false);
    }

    void showProfile(String displayName, String avatarUrl) {
        final String target = RealUi.optional(avatarUrl); requestedUrl = target;
        setIcon(null); setText(initial(displayName)); repaint();
        if (target == null) return;
        AsyncTask.run(() -> load(target), new AsyncTask.Callback<ImageIcon>() {
            @Override public void onSuccess(ImageIcon value) {
                if (!target.equals(requestedUrl)) return; setText(""); setIcon(value); repaint();
            }
            @Override public void onFailure(Throwable error) { /* 保留姓名占位，不阻断资料编辑。 */ }
        });
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(DesignTokens.PRIMARY); g.fillOval(0, 0, SIZE, SIZE);
        if (getIcon() != null) {
            g.setClip(new Ellipse2D.Double(0, 0, SIZE, SIZE)); getIcon().paintIcon(this, g, 0, 0);
        }
        g.dispose();
        if (getIcon() == null) super.paintComponent(graphics);
    }

    private static ImageIcon load(String value) throws Exception {
        BufferedImage source;
        if (value.startsWith("data:image/")) {
            int comma = value.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException("头像数据无效");
            source = ImageIO.read(new ByteArrayInputStream(
                    Base64.getDecoder().decode(value.substring(comma + 1))));
        } else {
            URL url = new URL(value); java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000); connection.setReadTimeout(5000);
            source = ImageIO.read(connection.getInputStream());
        }
        if (source == null) throw new IllegalArgumentException("头像不是有效图片");
        Image scaled = source.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static String initial(String name) {
        String value = name == null ? "" : name.trim();
        return value.isEmpty() ? "人" : value.substring(0, 1);
    }
}
