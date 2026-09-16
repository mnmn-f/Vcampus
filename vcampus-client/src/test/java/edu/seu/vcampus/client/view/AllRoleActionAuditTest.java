package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.controller.WorkspaceController;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.ResponsiveGridLayout;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** 全角色页面静态操作入口审计；业务成功/失败路径由纵切测试另行覆盖。 */
public class AllRoleActionAuditTest {
    @Test public void allVisibleRoleModulesHaveWiredButtonsAndLockedTableHeaders() throws Exception {
        List<String> failures = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (Role role : Role.values()) {
                ClientSession session = new ClientSession(); session.open(new LoginResult(7L, "audit", "测试", role, "audit"));
                ClientBusinessServices services = new ClientBusinessServices(new NetworkClientService(new EmptyGateway()), session);
                WorkspaceController controller = new WorkspaceController(session, null, services);
                for (ModuleId module : ModuleId.values()) if (module.isVisibleTo(role)) {
                    BasePage page = controller.pageFor(module, ignored -> { });
                    for (int[] size : new int[][]{{960, 640}, {1200, 760}, {1440, 900}}) {
                        page.setSize(size[0], size[1]);
                        layout(page); layout(page);
                        inspect(page, role + "/" + module + "@" + size[0] + "x" + size[1], failures);
                    }
                }
            }
        });
        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    private static void inspect(Component component, String path, List<String> failures) {
        if (component instanceof JButton) {
            JButton button = (JButton) component;
            if (button.getText() != null && !button.getText().trim().isEmpty()
                    && button.getActionListeners().length == 0) failures.add(path + ": 未接线按钮 " + button.getText());
        }
        if (component instanceof JTable && ((JTable) component).getTableHeader() != null
                && ((JTable) component).getTableHeader().getReorderingAllowed()) failures.add(path + ": 表头可拖动");
        if (isActionButton(component) && component.getHeight() > 0
                && component.getHeight() > component.getPreferredSize().height + 8) {
            failures.add(path + ": 操作按钮高度异常 "
                    + ((javax.swing.AbstractButton) component).getText() + "=" + component.getHeight());
        }
        if (isActionButton(component) && component.getWidth() > 0
                && component.getWidth() > component.getPreferredSize().width + 120) {
            failures.add(path + ": 操作按钮宽度异常 "
                    + ((javax.swing.AbstractButton) component).getText() + "=" + component.getWidth());
        }
        if (component instanceof JTextField && component.getHeight() > 56) {
            JTextField field = (JTextField) component;
            failures.add(path + ": 单行输入框高度异常=" + component.getHeight()
                    + ", preferred=" + component.getPreferredSize().height
                    + ", name=" + field.getAccessibleContext().getAccessibleName()
                    + ", parent=" + (field.getParent() == null ? "--"
                    : field.getParent().getClass().getSimpleName())
                    + ", owner=" + owner(field));
        }
        if (component instanceof Container) inspectMixedFormGrid((Container) component, path, failures);
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) inspect(child, path, failures);
    }

    private static String owner(Component component) {
        Container current = component.getParent();
        while (current != null) {
            String name = current.getClass().getSimpleName();
            if (!"JPanel".equals(name) && !"JViewport".equals(name)
                    && !"JScrollPane".equals(name)) return name;
            current = current.getParent();
        }
        return "--";
    }

    private static boolean isActionButton(Component component) {
        return component instanceof PrimaryButton || component instanceof SecondaryButton
                || component instanceof DangerButton;
    }

    private static void layout(Component component) {
        component.doLayout();
        if (component instanceof Container) for (Component child
                : ((Container) component).getComponents()) layout(child);
    }

    private static void inspectMixedFormGrid(Container container, String path, List<String> failures) {
        if (!(container.getLayout() instanceof GridLayout)
                && !(container.getLayout() instanceof ResponsiveGridLayout)) return;
        boolean hasField = false;
        for (Component child : container.getComponents()) {
            if (!(child instanceof javax.swing.AbstractButton)) hasField = true;
        }
        if (!hasField) return;
        for (Component child : container.getComponents()) {
            if (child instanceof javax.swing.AbstractButton) {
                failures.add(path + ": 表单网格直接拉伸按钮 "
                        + ((javax.swing.AbstractButton) child).getText());
            }
        }
    }
    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
