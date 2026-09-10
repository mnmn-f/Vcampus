package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.DemoAuthClientService;
import edu.seu.vcampus.client.controller.LoginController;
import edu.seu.vcampus.client.ui.DesignTokens;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;

/** 登录页视觉预览。 */
public final class LoginPagePreviewTest {
    @Test public void renderLoginAtDeliverySize() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                DemoAuthClientService auth = new DemoAuthClientService();
                JPanel page = new JPanel(new GridLayout(1, 2));
                page.setBackground(DesignTokens.PAGE_BACKGROUND);
                page.add(new LoginBrandPanel());
                JPanel formArea = new JPanel(new java.awt.GridBagLayout());
                formArea.setBackground(DesignTokens.PAGE_BACKGROUND);
                formArea.add(new LoginFormPanel(new LoginController(auth), new LoginFormPanel.Listener() {
                    @Override public void onLoginSuccess(edu.seu.vcampus.common.dto.auth.LoginResult result) { }
                }, new Runnable() { @Override public void run() { } }));
                page.add(formArea);
                page.setSize(1120, 760); page.validate(); layout(page);
                BufferedImage image = new BufferedImage(1120, 760, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics(); page.printAll(graphics); graphics.dispose();
                try {
                    File dir = new File("target/ui-previews/roles"); dir.mkdirs();
                    ImageIO.write(image, "png", new File(dir, "login-personnel-1120x760.png"));
                } catch (Exception ex) { throw new PreviewFailure(ex); }
            }
        });
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }

    private static final class PreviewFailure extends RuntimeException {
        PreviewFailure(Exception cause) { super(cause); }
    }
}
