package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/**
 * 宿舍服务网络页面，按当前职责组合学生自助或宿管治理能力。
 *
 * <p>标签按「一件事一个入口」组织，而不是按实现它的面板个数：同一件事涉及的多个
 * 面板用 {@code TaskTabs.addTask(标题, 面板...)} 叠在一个标签页里。之前水电账单和
 * 抄表出账、宿舍公告和公告设置各占一个标签，用起来要在两个标签间来回跳，实际是
 * 同一件事的两半。</p>
 *
 * <p>参数设置和不可逆操作（门禁时段、预警阈值、删除房间）集中在「设置」里：它们
 * 调一次管很久，混在天天要用的页面上只会增加误触。</p>
 *
 * <p>各标签页内容量差得多，而 {@code JTabbedPane} 默认按最高的一页定尺寸，会让矮的页面
 * 拖出一大片空白，所以统一交给 {@link DormExtTabHeights} 按当前页伸缩。</p>
 */
public final class RealDormPage extends BasePage {
    public RealDormPage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.DORMITORY), "");
        Role role = session.getActiveRole(); setHeaderContext(role.getDisplayName());
        if (role == Role.STUDENT) buildStudent(services);
        else if (role == Role.DORM_MANAGER) buildManager(services);
        else warning("当前角色没有宿舍服务页面。");
    }

    private void buildStudent(ClientBusinessServices services) {
        DormExtClientService ext = services.dormExt();
        TaskTabs tabs = new TaskTabs();
        tabs.addTask("我的住宿", new DormAccommodationPanel(this, services.dorm()));
        // 请假和外来登记都是「向宿管提交一件事再等审批」，放在一起找得到。
        tabs.addTask("申请与登记",
                new DormStudentLeavePanel(this, services.dorm()),
                new DormExtVisitorPanel(this, ext));
        // 入内授权本来就是报修单的一个属性，不该单独占一个标签。
        tabs.addTask("报修服务",
                new DormStudentRepairsPanel(this, services.dorm()),
                new DormExtPermitPanel(this, ext),
                new DormStudentRepairEvaluationPanel(this, services.dorm()));
        tabs.addTask("水电账单", new DormStudentBillsPanel(this, services.dorm()));
        tabs.addTask("在宿门禁", new DormExtStayPanel(this, ext));
        // 学生端只保留带类型/范围/置顶的这一份：它是 main 那份的超集，
        // 两份并列只会让人以为是两批公告。
        tabs.addTask("宿舍公告", new DormExtNoticePanel(this, ext, false));
        DormExtTabHeights.fitToSelectedTab(tabs);
        addBlock(tabs);
    }

    private void buildManager(ClientBusinessServices services) {
        DormExtClientService ext = services.dormExt();
        TaskTabs tabs = new TaskTabs();
        tabs.addTask("住宿与空间", new DormManagerSpacePanel(this, services.dorm()));
        // 住宿申请、请假、来访三类审批合并：对宿管来说都是「待我处理的申请」。
        tabs.addTask("申请与审批",
                new DormManagerRequestsPanel(this, services.dorm()),
                new DormManagerLeavePanel(this, services.dorm()),
                new DormExtVisitorAuditPanel(this, ext));
        tabs.addTask("报修处理", new DormManagerRepairsPanel(this, services.dorm()));
        // 账单是抄表的结果，两者必须并排看才判断得出哪个房间还没出账。
        tabs.addTask("水电",
                new DormManagerBillingPanel(this, services.dorm()),
                new DormExtBillingPanel(this, ext));
        // 未归与卫生是两件不相干的事，main 把它们放在同一个面板里，这里拆开分两页。
        // 晚归（回来了但超时）与连续未归（一直没回来）是两套数据，互补而非重复；
        // 卫生同理：一边是检查记录，一边是五项分项与待检任务。
        DormExtGovernanceSplit governance =
                new DormExtGovernanceSplit(new DormManagerGovernancePanel(this, services.dorm()));
        tabs.addTask("未归管理",
                governance.absencePart(),
                new DormExtWarningPanel(this, ext),
                new DormExtStayAdminPanel(this, ext));
        tabs.addTask("卫生管理",
                new DormExtHygienePanel(this, ext),
                governance.hygienePart());
        tabs.addTask("宿舍公告",
                new DormExtNoticePanel(this, ext, true));
        tabs.addTask("设置", new DormExtSettingsPanel(this, ext));
        DormExtTabHeights.fitToSelectedTab(tabs);
        addBlock(tabs);
    }
}
