package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.EmptyStatePanel;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;

/** Demo 模式的统一业务占位页，不包含静态业务记录或操作按钮。 */
public final class ServiceRequiredPage extends BasePage {
    public ServiceRequiredPage(ClientSession session, ModuleId module) {
        super(session, title(session, module), "");
        if (session != null && session.isAuthenticated()) {
            setHeaderContext(session.getActiveRole().getDisplayName());
        }
        addBlock(new EmptyStatePanel("需要连接服务端", "", null));
    }

    private static String title(ClientSession session, ModuleId module) {
        if (session != null && session.isAuthenticated() && module != null) {
            return RoleWorkspace.navigationLabel(session.getActiveRole(), module);
        }
        return module == null ? "校园服务" : module.getDisplayName();
    }
}
