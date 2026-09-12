package edu.seu.vcampus.client.view.modules.real;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** 头像、商品图和封面共用本地解码限制；上传压缩内容，不上传本地路径。 */
final class ImageUploadSupport {
    private ImageUploadSupport() { }
    static byte[] encode(File file, int width, int height, boolean crop) throws Exception {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > 5L * 1024 * 1024)
            throw new IllegalArgumentException("请选择不超过 5MB 的 JPG 或 PNG 图片");
        BufferedImage source;
        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("图片格式不支持");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream); String format = reader.getFormatName();
                if (!("png".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format))) throw new IllegalArgumentException("仅支持 JPG 或 PNG");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 12_000_000) throw new IllegalArgumentException("图片尺寸过大，请先缩小原图");
                source = reader.read(0);
            } finally { reader.dispose(); }
        }
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        try {
            g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double sx = (double) width / source.getWidth(), sy = (double) height / source.getHeight();
            double scale = crop ? Math.max(sx, sy) : Math.min(sx, sy);
            int w = Math.max(1, (int) Math.ceil(source.getWidth() * scale)), h = Math.max(1, (int) Math.ceil(source.getHeight() * scale));
            g.drawImage(source, (width - w) / 2, (height - h) / 2, w, h, null);
        } finally { g.dispose(); }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(output, "jpg", bytes);
        if (bytes.size() > 256 * 1024) throw new IllegalArgumentException("图片压缩后过大，请选择较小的图片");
        return bytes.toByteArray();
    }
}
