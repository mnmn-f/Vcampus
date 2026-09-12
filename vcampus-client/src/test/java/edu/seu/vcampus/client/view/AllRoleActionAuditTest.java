package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.controller.WorkspaceController;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
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
                    inspect(page, role + "/" + module, failures);
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
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) inspect(child, path, failures);
    }
    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
