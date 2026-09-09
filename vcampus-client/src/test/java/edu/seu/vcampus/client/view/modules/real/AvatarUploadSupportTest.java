package edu.seu.vcampus.client.view.modules.real;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 头像上传只保留压缩后的标准图片，不传递用户本地路径。 */
public final class AvatarUploadSupportTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void imageIsCroppedCompressedAndEmbedded() throws Exception {
        File source = temporary.newFile("avatar.png");
        BufferedImage image = new BufferedImage(640, 320, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE); graphics.fillRect(0, 0, 640, 320); graphics.dispose();
        ImageIO.write(image, "png", source);

        String encoded = AvatarUploadSupport.encode(source);
        assertTrue(encoded.startsWith("data:image/jpeg;base64,"));
        byte[] bytes = Base64.getDecoder().decode(encoded.substring(encoded.indexOf(',') + 1));
        BufferedImage result = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        assertEquals(256, result.getWidth()); assertEquals(256, result.getHeight());
        assertTrue(encoded.length() < 400_000);
    }
}
