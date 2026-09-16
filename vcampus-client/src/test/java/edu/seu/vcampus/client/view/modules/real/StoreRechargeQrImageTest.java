package edu.seu.vcampus.client.view.modules.real;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import org.junit.Test;
import static org.junit.Assert.*;
public class StoreRechargeQrImageTest {
    @Test public void bundledUserImageRetainsPortraitProportions() {
        BufferedImage source = StoreRechargeQrDialog.load(new File("missing-qr-test-file.png"));
        assertEquals(828, source.getWidth());
        assertEquals(1124, source.getHeight());
        ImageIcon icon = StoreRechargeQrDialog.preview(source);
        assertTrue(icon.getIconWidth() <= 380);
        assertTrue(icon.getIconHeight() <= 460);
        assertEquals(828.0 / 1124.0, (double) icon.getIconWidth() / icon.getIconHeight(), 0.005);
    }
}
