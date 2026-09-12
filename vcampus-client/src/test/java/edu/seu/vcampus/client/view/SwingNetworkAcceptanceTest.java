package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.NetworkAuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.controller.WorkspaceController;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.protocol.Message;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** 可选：真实 TCP/MySQL 页面读链路与截图，不提交任何业务写操作。 */
public class SwingNetworkAcceptanceTest {
    @Test public void allDemoRolesLoadEveryVisibleModuleThroughRealNetwork() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.ui.acceptance"));
        SwingUtilities.invokeAndWait(edu.seu.vcampus.client.ui.UiFactory::configureLookAndFeel);
        String[][] accounts = {{"demo_student", "student123"}, {"demo_teacher", "teacher123"},
                {"demo_registrar", "registrar123"}, {"demo_academic", "academic123"},
                {"demo_librarian", "library123"}, {"demo_store", "store123"},
                {"demo_dorm", "dorm123"}, {"demo_repair", "repair123"},
                {"demo_ai", "ai123"}, {"demo_system", "system123"}};
        List<String> errors = new ArrayList<>();
        for (String[] account : accounts) {
            RecordingGateway gateway = new RecordingGateway();
            NetworkAuthClientService auth = new NetworkAuthClientService(gateway);
            try {
                ClientSession session = new ClientSession(); session.open(auth.login(account[0], account[1]));
                ClientBusinessServices services = new ClientBusinessServices(new NetworkClientService(gateway), session);
                WorkspaceController controller = new WorkspaceController(session, null, services);
                for (ModuleId module : ModuleId.values()) if (module.isVisibleTo(session.getActiveRole())) {
                    AtomicReference<BasePage> page = new AtomicReference<>();
                    SwingUtilities.invokeAndWait(() -> page.set(controller.pageFor(module, ignored -> { })));
                    awaitIdle(gateway, page.get());
                    if (module != ModuleId.AI_ASSISTANT) {
                        snapshot(page.get(), account[0] + "-" + module);
                        awaitIdle(gateway, page.get());
                        snapshot(page.get(), account[0] + "-" + module);
                        TaskTabs tabs = findTabs(page.get());
                        if (tabs != null) for (int tab = 1; tab < tabs.getTabCount(); tab++) {
                            final int selected = tab; SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(selected));
                            snapshot(page.get(), account[0] + "-" + module + "-tab" + tab);
                            awaitIdle(gateway, page.get());
                            snapshot(page.get(), account[0] + "-" + module + "-tab" + tab);
                        }
                    }
                }
                errors.addAll(gateway.errors);
            } finally { auth.logout(); gateway.close(); }
        }
        assertTrue(String.join("\n", errors), errors.isEmpty());
    }

    private static void awaitIdle(RecordingGateway gateway, BasePage page) throws Exception {
        int last = -1, quiet = 0;
        for (int i = 0; i < 1200; i++) {
            Thread.sleep(50); java.util.concurrent.atomic.AtomicBoolean loading = new java.util.concurrent.atomic.AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> loading.set(loading(page)));
            int count = gateway.calls.get();
            quiet = !loading.get() && edu.seu.vcampus.client.view.modules.real.AsyncTask.isIdle()
                    && edu.seu.vcampus.client.view.modules.real.ProductImageView.isIdle()
                    && gateway.active.get() == 0 && last == count ? quiet + 1 : 0; last = count;
            if (quiet >= 6) return;
        }
        fail("页面网络请求未完成");
    }
    private static boolean loading(Component component) {
        if (component instanceof edu.seu.vcampus.client.view.modules.real.AsyncPagedTable
                && ((edu.seu.vcampus.client.view.modules.real.AsyncPagedTable<?>) component).isLoading()) return true;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) if (loading(child)) return true;
        return false;
    }
    private static void snapshot(BasePage page, String name) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                File directory = new File("target/network-ui-review"); assertTrue(directory.isDirectory() || directory.mkdirs());
                for (int width : new int[]{960, 1280}) {
                    page.setSize(width, 760); for (int i = 0; i < 4; i++) layout(page);
                    BufferedImage image = new BufferedImage(width, 760, BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D graphics = image.createGraphics(); page.printAll(graphics); graphics.dispose();
                    ImageIO.write(image, "png", new File(directory, name + "-" + width + ".png"));
                }
            } catch (Throwable failure) { error.set(failure); }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
    private static void layout(Component component) {
        component.doLayout(); if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) layout(child);
    }
    private static TaskTabs findTabs(Component component) {
        if (component instanceof TaskTabs) return (TaskTabs) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            TaskTabs tabs = findTabs(child); if (tabs != null) return tabs;
        }
        return null;
    }
    private static final class RecordingGateway implements ClientGateway {
        private final SocketClientGateway delegate = new SocketClientGateway("127.0.0.1",
                Integer.getInteger("vcampus.test.server.port", 8991), 3000, 15000);
        private final AtomicInteger calls = new AtomicInteger(), active = new AtomicInteger();
        private final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
        @Override public Message send(Message request) throws IOException, ClassNotFoundException {
            active.incrementAndGet();
            try {
                Message response = delegate.send(request);
                if (!response.isSuccess()) errors.add(request.getCommand() + ": " + response.getResultCode());
                return response;
            } finally { calls.incrementAndGet(); active.decrementAndGet(); }
        }
        @Override public boolean isConnected() { return delegate.isConnected(); }
        @Override public void close() { delegate.close(); }
    }
}
