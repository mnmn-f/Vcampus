package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 宿舍服务网络页面，按当前职责组合学生自助或宿管治理能力。 */
public final class RealDormPage extends BasePage {
    public RealDormPage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.DORMITORY), "");
        Role role = session.getActiveRole(); setHeaderContext(role.getDisplayName());
        if (role == Role.STUDENT) buildStudent(services);
        else if (role == Role.DORM_MANAGER) buildManager(services);
        else warning("当前角色没有宿舍服务页面。");
    }

    private void buildStudent(ClientBusinessServices services) {
        TaskTabs tabs = new TaskTabs();
        tabs.addTask("我的住宿", new DormAccommodationPanel(this, services.dorm()));
        tabs.addTask("请假申请", new DormStudentLeavePanel(this, services.dorm()));
        tabs.addTask("报修服务", new DormStudentRepairsPanel(this, services.dorm()),
                new DormStudentRepairEvaluationPanel(this, services.dorm()));
        tabs.addTask("水电账单", new DormStudentBillsPanel(this, services.dorm()));
        tabs.addTask("宿舍公告", new DormAnnouncementsPanel(this, services.dorm(), Role.STUDENT));
        addBlock(tabs);
    }

    private void buildManager(ClientBusinessServices services) {
        TaskTabs tabs = new TaskTabs();
        tabs.addTask("住宿与空间", new DormManagerSpacePanel(this, services.dorm()));
        tabs.addTask("申请审批", new DormManagerRequestsPanel(this, services.dorm()),
                new DormManagerLeavePanel(this, services.dorm()));
        tabs.addTask("日常治理", new DormManagerGovernancePanel(this, services.dorm()));
        tabs.addTask("报修处理", new DormManagerRepairsPanel(this, services.dorm()));
        tabs.addTask("水电账单", new DormManagerBillingPanel(this, services.dorm()));
        tabs.addTask("宿舍公告", new DormAnnouncementsPanel(this, services.dorm(), Role.DORM_MANAGER));
        addBlock(tabs);
    }
}
