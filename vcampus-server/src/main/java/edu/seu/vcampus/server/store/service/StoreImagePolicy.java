package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import java.io.ByteArrayInputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** 不接受客户端路径；存量网络图片仍可读，新上传限制格式和解码尺寸。 */
final class StoreImagePolicy {
    private StoreImagePolicy() { }
    static boolean reference(String value) { return value != null && value.matches("store-image:[0-9a-fA-F-]{36}"); }
    static void validate(ProductWriteRequest request) throws StoreServiceException {
        String value = request.getImageUrl(); byte[] bytes = request.getImageData();
        try {
            if (bytes != null) {
                if (!reference(value) || bytes.length == 0 || bytes.length > 256 * 1024) throw new IllegalArgumentException();
                try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw new IllegalArgumentException();
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input); String format = reader.getFormatName();
                        if (!("png".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format))
                                || reader.getWidth(0) > 1600 || reader.getHeight(0) > 1600) throw new IllegalArgumentException();
                        if (reader.read(0) == null) throw new IllegalArgumentException();
                    } finally { reader.dispose(); }
                }
                return;
            }
            if (value == null || value.isEmpty() || reference(value)) return;
            URI uri = new URI(value);
            if (value.length() > 1000 || uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) throw new IllegalArgumentException();
        } catch (Exception error) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "请选择有效的 JPG 或 PNG 图片（压缩后不超过 256KB）");
        }
    }
    static void requireExisting(ProductWriteRequest request, String previous) throws StoreServiceException {
        if (request.getImageData() == null && reference(request.getImageUrl()) && !request.getImageUrl().equals(previous))
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "图片引用已失效，请重新选择图片");
    }
}
