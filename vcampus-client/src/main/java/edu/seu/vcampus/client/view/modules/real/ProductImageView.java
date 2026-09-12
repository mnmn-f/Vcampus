package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import java.awt.Dimension;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** 商品图片异步加载器：服务器图片引用或历史 HTTP(S) 地址，限制大小并复用缓存。 */
public final class ProductImageView extends JLabel {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final java.util.concurrent.atomic.AtomicInteger PENDING = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.ExecutorService DOWNLOADS = new java.util.concurrent.ThreadPoolExecutor(
            3, 3, 30, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<Runnable>(100),
            task -> { Thread thread = new Thread(task, "product-image"); thread.setDaemon(true); return thread; });
    private static final Map<String, ImageIcon> CACHE = new LinkedHashMap<String, ImageIcon>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> e) { return size() > 48; }
    };
    private static final ImageIcon PLACEHOLDER = placeholder();
    private String currentUrl;
    private final int imageWidth;
    private final int imageHeight;
    private boolean initialized;
    private final edu.seu.vcampus.client.service.store.StoreClientService service;

    public ProductImageView() {
        this(180, 140);
    }

    ProductImageView(int width, int height) {
        this(width, height, null);
    }
    ProductImageView(int width, int height, edu.seu.vcampus.client.service.store.StoreClientService service) {
        this.service = service;
        imageWidth = width; imageHeight = height;
        setHorizontalAlignment(CENTER); setVerticalAlignment(CENTER);
        setPreferredSize(new Dimension(width, height)); setMinimumSize(new Dimension(width, height));
        setOpaque(true); setBackground(new java.awt.Color(0xF2, 0xF5, 0xF3)); setIcon(PLACEHOLDER);
        setForeground(DesignTokens.TEXT_SECONDARY);
    }

    public void load(final String url) {
        String normalized = normalize(url);
        if (initialized && java.util.Objects.equals(currentUrl, normalized)) return;
        initialized = true; currentUrl = normalized; setIcon(PLACEHOLDER);
        if (currentUrl == null) { setText("暂无图片"); return; }
        final String target = currentUrl; ImageIcon hit;
        synchronized (CACHE) { hit = CACHE.get(target); }
        if (hit != null) { apply(target, hit); return; }
        setText("图片加载中…");
        PENDING.incrementAndGet();
        try {
            DOWNLOADS.execute(() -> {
                ImageIcon loaded = null;
                try { loaded = read(target); synchronized (CACHE) { CACHE.put(target, loaded); } }
                catch (Exception ignored) { }
                final ImageIcon result = loaded;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        if (result != null) apply(target, result);
                        else if (target.equals(currentUrl)) setText("图片暂不可用");
                    } finally { PENDING.decrementAndGet(); }
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException full) {
            PENDING.decrementAndGet(); setText("图片暂不可用");
        }
    }
    public static boolean isIdle() { return PENDING.get() == 0; }

    private void apply(String target, ImageIcon icon) {
        if (!target.equals(currentUrl)) return;
        setText(""); setIcon(fit(icon.getImage(), icon.getIconWidth(), icon.getIconHeight(), imageWidth, imageHeight));
    }

    void showBytes(byte[] bytes) {
        currentUrl = null; initialized = false;
        try { ImageIcon icon = decode(bytes); setText(""); setIcon(fit(icon.getImage(), icon.getIconWidth(), icon.getIconHeight(), imageWidth, imageHeight)); }
        catch (Exception error) { setIcon(PLACEHOLDER); setText("图片无法预览"); }
    }

    private ImageIcon read(String value) throws Exception {
        if (value.startsWith("store-image:")) {
            if (service == null) throw new java.io.IOException("图片服务未连接");
            return decode(service.getProductImage(value));
        }
        HttpURLConnection connection = open(value);
        try {
        int length = connection.getContentLength(); if (length > MAX_BYTES) throw new IllegalArgumentException("图片过大");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int total = 0, count;
        try (java.io.InputStream in = connection.getInputStream()) { while ((count = in.read(buffer)) >= 0) { total += count; if (total > MAX_BYTES) throw new IllegalArgumentException("图片过大"); out.write(buffer, 0, count); } }
        return decode(out.toByteArray());
        } finally { connection.disconnect(); }
    }

    private static ImageIcon decode(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("图片内容无效");
        try (javax.imageio.stream.ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("图片格式不支持");
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if ((long) width * height > 12000000L) throw new IllegalArgumentException("图片尺寸过大");
                return fit(reader.read(0), width, height, 180, 140);
            } finally { reader.dispose(); }
        }
    }

    private static HttpURLConnection open(String target) throws Exception {
        for (int redirects = 0; redirects <= 3; redirects++) {
            HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
            connection.setConnectTimeout(3000); connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) return connection;
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (status < 300 || status >= 400 || location == null) throw new java.io.IOException("图片下载失败");
            String next = normalize(new URI(target).resolve(location).toString());
            if (next == null || (target.startsWith("https:") && !next.startsWith("https:")))
                throw new java.io.IOException("图片跳转地址无效");
            target = next;
        }
        throw new java.io.IOException("图片跳转过多");
    }

    private static ImageIcon fit(Image image, int width, int height, int maxWidth, int maxHeight) {
        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        return new ImageIcon(image.getScaledInstance(Math.max(1, (int) (width * scale)),
                Math.max(1, (int) (height * scale)), Image.SCALE_SMOOTH));
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.matches("store-image:[0-9a-fA-F-]{36}")) return value;
        try { URI uri = new URI(value.trim()); String scheme = uri.getScheme(); if (uri.getHost() == null || uri.getUserInfo() != null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return null; return uri.toString(); }
        catch (Exception ex) { return null; }
    }
    private static ImageIcon placeholder() { return new ImageIcon(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)); }
}
