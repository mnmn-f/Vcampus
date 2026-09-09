package edu.seu.vcampus.client.view.modules.real;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

/** 本地头像选择、缩放和内嵌编码；服务端只保存压缩后的图片。 */
final class AvatarUploadSupport {
    private static final long MAX_SOURCE_BYTES = 5L * 1024L * 1024L;
    private static final int OUTPUT_SIZE = 256;
    private static final int MAX_ENCODED_LENGTH = 400_000;

    private AvatarUploadSupport() { }

    static String chooseAndEncode(Component parent) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择头像");
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件（JPG、PNG）", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        return encode(chooser.getSelectedFile());
    }

    static String encode(File file) throws Exception {
        if (file == null || !file.isFile()) throw new IllegalArgumentException("请选择有效的图片文件");
        if (file.length() > MAX_SOURCE_BYTES) throw new IllegalArgumentException("头像原图不能超过 5MB");
        BufferedImage source = ImageIO.read(file);
        if (source == null) throw new IllegalArgumentException("仅支持 JPG 或 PNG 图片");
        BufferedImage output = fit(source);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(output, "jpg", bytes);
        String value = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        if (value.length() > MAX_ENCODED_LENGTH) throw new IllegalArgumentException("头像压缩后仍然过大");
        return value;
    }

    private static BufferedImage fit(BufferedImage source) {
        BufferedImage output = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
        double scale = Math.max((double) OUTPUT_SIZE / source.getWidth(),
                (double) OUTPUT_SIZE / source.getHeight());
        int width = (int) Math.ceil(source.getWidth() * scale);
        int height = (int) Math.ceil(source.getHeight() * scale);
        graphics.drawImage(source, (OUTPUT_SIZE - width) / 2, (OUTPUT_SIZE - height) / 2,
                width, height, null);
        graphics.dispose();
        return output;
    }
}
