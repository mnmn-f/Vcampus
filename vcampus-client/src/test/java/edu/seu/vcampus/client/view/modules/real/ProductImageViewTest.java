package edu.seu.vcampus.client.view.modules.real;

import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductImageViewTest {
    private HttpServer server;
    private String base;
    private final AtomicInteger reads = new AtomicInteger();

    @Before public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB), "png", bytes);
        server.createContext("/image", exchange -> { reads.incrementAndGet(); exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.size()); exchange.getResponseBody().write(bytes.toByteArray()); exchange.close(); });
        server.createContext("/redirect", exchange -> { exchange.getResponseHeaders().set("Location", "/image"); exchange.sendResponseHeaders(302, -1); exchange.close(); });
        server.createContext("/loop", exchange -> { exchange.getResponseHeaders().set("Location", "/loop"); exchange.sendResponseHeaders(302, -1); exchange.close(); });
        server.start(); base = "http://127.0.0.1:" + server.getAddress().getPort();
    }
    @After public void stop() { server.stop(0); }

    @Test public void redirectsLoadAndKeepAspectRatioAndReuseCache() throws Exception {
        ProductImageView view = new ProductImageView();
        AsyncPagedTableRefreshTest.edt(() -> view.load(base + "/redirect"));
        AsyncPagedTableRefreshTest.await(() -> view.getIcon().getIconWidth() > 1);
        assertEquals(180, view.getIcon().getIconWidth()); assertEquals(90, view.getIcon().getIconHeight());
        ProductImageView thumbnail = new ProductImageView(76, 56);
        AsyncPagedTableRefreshTest.edt(() -> thumbnail.load(base + "/redirect"));
        assertEquals(76, thumbnail.getIcon().getIconWidth()); assertEquals(38, thumbnail.getIcon().getIconHeight());
        assertEquals(1, reads.get());
    }
    @Test public void invalidSchemeAndRedirectLoopHaveVisibleFallback() throws Exception {
        ProductImageView view = new ProductImageView();
        AsyncPagedTableRefreshTest.edt(() -> view.load("file:///private.png"));
        assertEquals("暂无图片", view.getText()); assertEquals(0, reads.get());
        AsyncPagedTableRefreshTest.edt(() -> view.load(base + "/loop"));
        AsyncPagedTableRefreshTest.await(() -> "图片暂不可用".equals(view.getText()));
        assertEquals(1, view.getIcon().getIconWidth());
    }
    @Test public void clearingSelectionClearsPreviousProductPhoto() throws Exception {
        ProductImageView view = new ProductImageView();
        AsyncPagedTableRefreshTest.edt(() -> view.load(base + "/image"));
        AsyncPagedTableRefreshTest.await(() -> view.getIcon().getIconWidth() > 1);
        AsyncPagedTableRefreshTest.edt(() -> view.load(null));
        assertEquals(1, view.getIcon().getIconWidth()); assertEquals("暂无图片", view.getText());
    }
}
