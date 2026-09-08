package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 学籍模块的网络页面，根据当前职责组合学生自助或学籍管理能力。 */
public final class RealStudentRecordPage extends BasePage {
    public RealStudentRecordPage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.STUDENT_RECORD),
                "");
        Role role = session.getActiveRole();
        setHeaderContext(role.getDisplayName());
        TaskTabs tabs = new TaskTabs();
        if (role == Role.STUDENT) {
            tabs.addTask("我的档案", new StudentOwnPanel(this, services.student()));
        } else if (role == Role.REGISTRAR) {
            tabs.addTask("学生档案", new StudentRegistrarPanel(this, services.student()));
        } else {
            warning("当前角色没有学籍页面。");
        }
        if (tabs.getTabCount() > 0) addBlock(tabs);
    }
}
