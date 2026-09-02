package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import java.awt.Dimension;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** 商品图片异步加载器：仅允许 http/https，限制大小并复用进程缓存。 */
public final class ProductImageView extends JLabel {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Map<String, ImageIcon> CACHE = new LinkedHashMap<String, ImageIcon>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> e) { return size() > 48; }
    };
    private static final ImageIcon PLACEHOLDER = placeholder();
    private String currentUrl;

    public ProductImageView() {
        setHorizontalAlignment(CENTER); setVerticalAlignment(CENTER);
        setPreferredSize(new Dimension(180, 140)); setMinimumSize(new Dimension(120, 100));
        setOpaque(true); setBackground(new java.awt.Color(0xF2, 0xF5, 0xF3)); setIcon(PLACEHOLDER);
        setForeground(DesignTokens.TEXT_SECONDARY);
    }

    public void load(final String url) {
        currentUrl = normalize(url); setIcon(PLACEHOLDER);
        if (currentUrl == null) { setText("暂无图片"); return; }
        final String target = currentUrl; ImageIcon hit;
        synchronized (CACHE) { hit = CACHE.get(target); }
        if (hit != null) { apply(target, hit); return; }
        setText("图片加载中…");
        AsyncTask.run(new AsyncTask.Work<ImageIcon>() {
            @Override public ImageIcon run() throws Exception { return read(target); }
        }, new AsyncTask.Callback<ImageIcon>() {
            @Override public void onSuccess(ImageIcon value) { synchronized (CACHE) { CACHE.put(target, value); } apply(target, value); }
            @Override public void onFailure(Throwable error) { if (target.equals(currentUrl)) setText("图片暂不可用"); }
        });
    }

    private void apply(String target, ImageIcon icon) {
        if (!target.equals(currentUrl)) return;
        setText(""); setIcon(icon);
    }

    private static ImageIcon read(String value) throws Exception {
        URLConnection connection = new URL(value).openConnection();
        connection.setConnectTimeout(3000); connection.setReadTimeout(3000);
        if (connection instanceof HttpURLConnection) ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
        int length = connection.getContentLength(); if (length > MAX_BYTES) throw new IllegalArgumentException("图片过大");
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int total = 0, count;
        try (java.io.InputStream in = connection.getInputStream()) { while ((count = in.read(buffer)) >= 0) { total += count; if (total > MAX_BYTES) throw new IllegalArgumentException("图片过大"); out.write(buffer, 0, count); } }
        java.awt.image.BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        if (image == null) throw new IllegalArgumentException("图片格式不支持");
        Image scaled = image.getScaledInstance(180, 140, Image.SCALE_SMOOTH); return new ImageIcon(scaled);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { URI uri = new URI(value.trim()); String scheme = uri.getScheme(); if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return null; return uri.toString(); }
        catch (Exception ex) { return null; }
    }
    private static ImageIcon placeholder() { return new ImageIcon(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)); }
}
