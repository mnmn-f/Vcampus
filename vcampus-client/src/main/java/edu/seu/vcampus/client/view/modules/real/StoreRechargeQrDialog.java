package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;

/** 充值二维码确认窗；优先读取外部 config 图片，便于部署后直接替换。 */
final class StoreRechargeQrDialog {
    private static final String RESOURCE = "/edu/seu/vcampus/client/store/recharge-qr.png";
    private static final File EXTERNAL = new File("config", "recharge-qr.png");

    private StoreRechargeQrDialog() { }

    static boolean show(java.awt.Component parent, BigDecimal amount) {
        JLabel image = new JLabel(new ImageIcon(load().getScaledInstance(260, 260, Image.SCALE_SMOOTH)));
        image.setHorizontalAlignment(JLabel.CENTER);
        JPanel panel = new JPanel(new BorderLayout(0, 10)); panel.setOpaque(false);
        panel.add(UiFactory.title("支付金额：¥" + amount.setScale(2).toPlainString()), BorderLayout.NORTH);
        panel.add(image, BorderLayout.CENTER);
        panel.add(UiFactory.muted("扫码完成后点击“已完成支付”。"), BorderLayout.SOUTH);
        Object[] options = {"已完成支付", "取消"};
        return JOptionPane.showOptionDialog(parent, panel, "充值支付", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]) == 0;
    }

    private static BufferedImage load() {
        try {
            if (EXTERNAL.isFile()) return ImageIO.read(EXTERNAL);
            try (InputStream input = StoreRechargeQrDialog.class.getResourceAsStream(RESOURCE)) {
                if (input != null) return ImageIO.read(input);
            }
        } catch (Exception ignored) { }
        return placeholder();
    }

    private static BufferedImage placeholder() {
        BufferedImage image = new BufferedImage(260, 260, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, 260, 260);
        g.setColor(new Color(0x2F, 0x4B, 0x24));
        for (int y = 18; y < 210; y += 16) for (int x = 18; x < 242; x += 16)
            if (((x / 16) * 7 + (y / 16) * 11) % 5 < 2) g.fillRect(x, y, 11, 11);
        g.setColor(Color.WHITE); g.fillRect(34, 101, 192, 58); g.setColor(new Color(0x2F, 0x4B, 0x24));
        g.drawString("充值二维码待配置", 78, 132); g.dispose(); return image;
    }
}
