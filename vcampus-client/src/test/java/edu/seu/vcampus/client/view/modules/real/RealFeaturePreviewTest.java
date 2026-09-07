package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.store.StoreSalesDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Container;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 离屏生成关键真实页面预览，供 1100x720 与 1280x820 视觉核对。 */
public final class RealFeaturePreviewTest {
    @Test
    public void renderFeaturePreviews() throws Exception {
        renderStore(1100, 720); renderStore(1280, 820);
        renderLeave(1100, 720, true); renderLeave(1280, 820, true);
        renderLeave(1100, 720, false); renderLeave(1280, 820, false);
        renderManager(1100, 720, true); renderManager(1280, 820, true);
        renderManager(1100, 720, false); renderManager(1280, 820, false);
        renderSpaceEditor(1100, 720); renderSpaceEditor(1280, 820);
    }

    private static void renderStore(int width, int height) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        final StoreClientService service = storeService(ready);
        render("store-sales-" + width + "x" + height, width, height, Role.STORE_MANAGER,
                new ComponentFactory() { @Override public JComponent create(PreviewPage page) { return new StoreSalesPanel(page, service); } }, ready);
    }

    private static void renderLeave(final int width, final int height, final boolean student) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        final DormClientService service = dormService(ready);
        render((student ? "student-leave-" : "student-repair-evaluation-") + width + "x" + height,
                width, height, Role.STUDENT, new ComponentFactory() {
                    @Override public JComponent create(PreviewPage page) {
                        return student ? new DormStudentLeavePanel(page, service) : new DormStudentRepairEvaluationPanel(page, service);
                    }
                }, ready);
    }

    private static void renderManager(final int width, final int height, final boolean leave) throws Exception {
        CountDownLatch ready = new CountDownLatch(leave ? 1 : 3);
        final DormClientService service = dormService(ready);
        render((leave ? "manager-leave-" : "manager-space-") + width + "x" + height,
                width, height, Role.DORM_MANAGER, new ComponentFactory() {
                    @Override public JComponent create(PreviewPage page) {
                        return leave ? new DormManagerLeavePanel(page, service) : new DormManagerSpacePanel(page, service);
                    }
                }, ready);
    }

    private static void renderSpaceEditor(int width, int height) throws Exception {
        render("manager-space-editor-" + width + "x" + height, width, height, Role.DORM_MANAGER,
                new ComponentFactory() { @Override public JComponent create(PreviewPage page) {
                    return new DormSpaceEditorPanel(page, dormService(new CountDownLatch(0)), new Runnable() {
                        @Override public void run() { }
                    });
                } }, new CountDownLatch(0));
    }

    private static void render(final String name, final int width, final int height, final Role role,
                               final ComponentFactory factory, final CountDownLatch ready) throws Exception {
        final PreviewPage[] page = new PreviewPage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            PreviewPage value = new PreviewPage(session(role));
            value.addPreview(factory.create(value)); page[0] = value;
            }
        });
        if (!ready.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("preview data did not load: " + name);
        Thread.sleep(100L);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            PreviewPage value = page[0]; value.setSize(width, height); value.validate(); layout(value);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); value.printAll(graphics); graphics.dispose();
            try { File dir = new File("target/ui-previews"); dir.mkdirs(); ImageIO.write(image, "png", new File(dir, name + ".png")); }
            catch (Exception ex) { throw new PreviewFailure(ex); }
            }
        });
    }

    private static StoreClientService storeService(final CountDownLatch ready) {
        return (StoreClientService) Proxy.newProxyInstance(StoreClientService.class.getClassLoader(),
                new Class<?>[]{StoreClientService.class}, new java.lang.reflect.InvocationHandler() {
                    @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    if ("salesReport".equals(method.getName())) { ready.countDown(); return sales(); }
                    return null;
                    }
                });
    }

    private static DormClientService dormService(final CountDownLatch ready) {
        return (DormClientService) Proxy.newProxyInstance(DormClientService.class.getClassLoader(),
                new Class<?>[]{DormClientService.class}, new java.lang.reflect.InvocationHandler() {
                    @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    String name = method.getName();
                    if ("myLeaves".equals(name) || "managerLeaves".equals(name)) { ready.countDown(); return leaves(); }
                    if ("repairs".equals(name)) { ready.countDown(); return repairs(); }
                    if ("buildings".equals(name)) { ready.countDown(); return buildings(); }
                    if ("rooms".equals(name)) { ready.countDown(); return rooms(); }
                    if ("beds".equals(name)) { ready.countDown(); return beds(); }
                    return null;
                    }
                });
    }

    private static StoreSalesPage sales() {
        StoreSalesDto row = new StoreSalesDto(11L, "SKU-11", "校园咖啡", 42L, new BigDecimal("210.00"));
        return new StoreSalesPage(Collections.singletonList(row), 1, 20, 1L, 42L, new BigDecimal("210.00"));
    }
    private static DormPage<LeaveRequestDto> leaves() {
        LeaveRequestDto row = new LeaveRequestDto(31L, 1001L, "PERSONAL", at(9), at(18), "参加校外活动",
                "PENDING", null, null, null, at(1));
        return new DormPage<LeaveRequestDto>(1, 20, 1L, Collections.singletonList(row));
    }
    private static DormPage<RepairOrderDto> repairs() {
        RepairOrderDto row = new RepairOrderDto(41L, 301L, 1001L, "WATER", "洗手池漏水", "NORMAL",
                "COMPLETED", 2001L, at(1), at(2), at(3), null, null);
        return new DormPage<RepairOrderDto>(1, 20, 1L, Collections.singletonList(row));
    }
    private static DormPage<DormBuildingDto> buildings() {
        return new DormPage<DormBuildingDto>(1, 20, 1L, Collections.singletonList(
                new DormBuildingDto(1L, "B1", "东大一号楼", "校内北区", "MIXED", "OPEN")));
    }
    private static DormPage<DormRoomDto> rooms() {
        return new DormPage<DormRoomDto>(1, 20, 1L, Collections.singletonList(new DormRoomDto(
                301L, 1L, "B1", "东大一号楼", "301", 3, 4, "STANDARD", "AVAILABLE", null, 1)));
    }
    private static DormPage<DormBedDto> beds() {
        return new DormPage<DormBedDto>(1, 20, 1L, Collections.singletonList(new DormBedDto(
                401L, 301L, "B1", "301", "01", "OCCUPIED", 1001L)));
    }
    private static LocalDateTime at(int hour) { return LocalDateTime.of(2026, 8, 29, hour, 0); }
    private static ClientSession session(Role role) { ClientSession value = new ClientSession(); value.open(new LoginResult(1001L, "preview", "预览用户", role, "preview-token")); return value; }
    private static void layout(Component value) { value.doLayout(); if (value instanceof Container) for (Component child : ((Container) value).getComponents()) layout(child); }

    private interface ComponentFactory { JComponent create(PreviewPage page); }
    private static final class PreviewPage extends BasePage {
        PreviewPage(ClientSession session) { super(session, "真实页面预览", "东大绿网络业务页面视觉核对"); }
        void addPreview(JComponent component) { addBlock(component); }
    }
    private static final class PreviewFailure extends RuntimeException {
        PreviewFailure(Exception cause) { super(cause); }
    }
}
