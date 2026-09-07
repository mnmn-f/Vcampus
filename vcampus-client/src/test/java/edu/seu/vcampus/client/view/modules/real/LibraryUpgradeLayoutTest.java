package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.service.library.DemoLibraryClientService;
import edu.seu.vcampus.client.service.library.DemoPdfClientService;
import edu.seu.vcampus.client.service.library.DemoPdfStore;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/** 无显示器下验证窄宽布局与完整页面截图，按实际组件边界检测裁剪。 */
public final class LibraryUpgradeLayoutTest {
    @Test public void reservationControlsFitAtNarrowAndWideWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int width : new int[]{390, 560, 1000}) {
                ReservationFormPanel form = new ReservationFormPanel(request -> { }); form.selectRoom(1);
                settle(form, width); assertControls(form);
                render(form, "reservation-" + width);
            }
            CalendarDateField date = new CalendarDateField();
            date.setDate(org.threeten.bp.LocalDate.of(2028, 2, 29)); assertEquals("2028-02-29", date.getDate().toString());
        });
    }
    @Test public void renderBookAndResourceWorkspaces() throws Exception {
        ClientSession student = session(Role.STUDENT);
        ClientSession admin = new ClientSession(); admin.open(new LoginResult(2, "demo_librarian", "图书管理员", Role.LIBRARIAN, "admin-token"));
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("vcampus-preview-");
        DemoPdfStore store = new DemoPdfStore(root); DemoPdfClientService service = new DemoPdfClientService(student, store);
        DemoPdfClientService reviewer = new DemoPdfClientService(admin, store);
        java.nio.file.Path file = root.resolve("学习资料.pdf");
        java.nio.file.Files.write(file, "%PDF-1.7 sample preview".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        edu.seu.vcampus.common.dto.library.PdfResourceView resource = new edu.seu.vcampus.client.service.library.PdfFileTransfers(service)
                .upload(file, "Java 程序设计学习资料", "课程学习与复习资料。", (n, t) -> { });
        reviewer.review(new edu.seu.vcampus.common.dto.library.PdfReviewRequest(resource.getId(), "APPROVED", null));
        new edu.seu.vcampus.client.service.library.PdfFileTransfers(service).upload(file, "计算机网络复习提纲", "期末复习材料。", (n, t) -> { });
        final PdfResourcesPanel[] panels = new PdfResourcesPanel[2];
        SwingUtilities.invokeAndWait(() -> {
            try {
                BookDetail book = new DemoLibraryClientService().bookDetail(101);
                for (int width : new int[]{460, 860}) {
                    LibraryBookDetailsPanel panel = new LibraryBookDetailsPanel(); panel.showBook(book); settle(panel, width); render(panel, "book-details-" + width);
                }
                panels[0] = new PdfResourcesPanel(new PreviewPage(student), service, Role.STUDENT);
                panels[1] = new PdfResourcesPanel(new PreviewPage(admin), reviewer, Role.LIBRARIAN);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        Thread.sleep(250);
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < panels.length; i++) { settle(panels[i], 960); render(panels[i], i == 0 ? "pdf-student" : "pdf-review"); }
        });
    }
    @Test public void realLibraryTracksViewportWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RealLibraryPage page = RealLibraryPage.demo(session(Role.STUDENT));
            for (int width : new int[]{760, 1200}) {
                page.setSize(width, 820); for (int i = 0; i < 6; i++) layout(page);
                for (Component component : descendants(page)) if (component instanceof edu.seu.vcampus.client.ui.WidthTrackingPanel)
                    assertTrue("page must track viewport", component.getWidth() <= component.getParent().getWidth());
                render(page, "library-page-" + width);
            }
        });
    }
    private static void settle(JComponent component, int width) {
        component.setSize(width, 800); for (int i = 0; i < 8; i++) { layout(component); component.setSize(width, component.getPreferredSize().height); }
        layout(component);
    }
    private static void assertControls(Component component) {
        if (component instanceof javax.swing.JTextField || component instanceof javax.swing.JComboBox || component instanceof javax.swing.JButton) {
            assertTrue("left edge: " + component, component.getX() >= 0);
            assertTrue("right edge: " + component, component.getX() + component.getWidth() <= component.getParent().getWidth());
            assertTrue("bottom edge: " + component, component.getY() + component.getHeight() <= component.getParent().getHeight());
            assertTrue("usable width", component.getWidth() > 20);
        }
        if (component instanceof javax.swing.JComboBox) return;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) if (child.isVisible()) assertControls(child);
    }
    private static java.util.List<Component> descendants(Component component) {
        java.util.List<Component> values = new java.util.ArrayList<Component>(); values.add(component);
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) values.addAll(descendants(child)); return values;
    }
    private static void layout(Component component) {
        if (!(component instanceof Container)) return; ((Container) component).doLayout();
        for (Component child : ((Container) component).getComponents()) layout(child);
    }
    private static void render(JComponent component, String name) {
        try {
            BufferedImage image = new BufferedImage(component.getWidth(), Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            component.printAll(graphics); graphics.dispose(); File dir = new File("target/library-previews"); dir.mkdirs(); javax.imageio.ImageIO.write(image, "png", new File(dir, name + ".png"));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    private static ClientSession session(Role role) {
        ClientSession s = new ClientSession(); s.open(new LoginResult(1, "demo_student", "演示学生", role, "preview-token")); return s;
    }
    private static final class PreviewPage extends BasePage {
        PreviewPage(ClientSession session) { super(session, "图书馆", ""); }
    }
}
