package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.module.ModuleId;

/** 系统管理员身份运营页面；不混入任何业务写入口。 */
public final class RealIdentityAdminPage extends BasePage {
    public RealIdentityAdminPage(ClientSession session, IdentityClientService service, boolean system) {
        super(session, system ? ModuleId.SYSTEM.getDisplayName() : ModuleId.USER_ADMIN.getDisplayName(), "");
        setHeaderContext(session.getActiveRole().getDisplayName());
        TaskTabs tabs = new TaskTabs();
        if (system) {
            tabs.addTask("运行概览", new IdentityMonitorPanel(this, service));
            tabs.addTask("登录审计", new IdentityAuditPanel(this, service));
        } else {
            tabs.addTask("账号与角色", new IdentityUsersPanel(this, service));
            tabs.addTask("登录会话", new IdentitySessionsPanel(this, service));
            tabs.addTask("注销审批", new IdentityCancellationPanel(this, service, true));
            tabs.addTask("安全记录", new IdentityAuditPanel(this, service));
        }
        addBlock(tabs);
    }
}
