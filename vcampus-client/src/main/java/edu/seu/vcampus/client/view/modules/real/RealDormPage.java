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
        else if (role == Role.REPAIR_WORKER) buildRepairWorker(services);
        else warning("当前角色没有宿舍服务页面。");
    }

    private void buildStudent(ClientBusinessServices services) {
        DormExtClientService ext = services.dormExt();
        TaskTabs tabs = DormTabs.create();
        tabs.addTask("我的住宿", new DormAccommodationPanel(this, services.dorm(), ext));
        // 请假和外来登记都是「向宿管提交一件事再等审批」，放在一起找得到。
        // 上下排而不是左右分栏：这两块各自带一张记录表，横着切开之后每张表只剩半屏
        // 宽，列全被压成省略号。中间那条深色分隔线负责说明「换了一件事」。
        tabs.addTask("申请与登记",
                new DormStudentLeavePanel(this, services.dorm()),
                new DormExtVisitorPanel(this, ext));
        // 报修、入内授权、评价说的都是同一张工单，所以是一页里的三块，不是三张表。
        tabs.addTask("报修服务", new DormStudentRepairPage(this, services.dorm(), ext));
        tabs.addTask("水电账单", new DormStudentBillsPanel(this, services.dorm(), ext));
        tabs.addTask("在宿门禁", new DormExtStayPanel(this, services.dorm(), ext));
        // 学生端只保留带类型/范围/置顶的这一份：它是 main 那份的超集，
        // 两份并列只会让人以为是两批公告。
        tabs.addTask("宿舍公告", new DormExtNoticePanel(this, ext, services.dorm(), false));
        DormExtTabHeights.fitToSelectedTab(tabs);
        DormUi.flatten(tabs);
        addBlock(tabs);
    }

    /**
     * 维修员工作台。
     *
     * <p>两个标签按一次派工的时间顺序排：在「我的工单」里推进度，做完了在「处理记录」
     * 回看评价。</p>
     *
     * <p>没有「待接工单」：派单权在宿管手上——他知道谁手头压了几单，也知道哪一单更急。
     * 再留一个让维修员自己抢单的队列，等于同一件事有两个入口，先到先得还会把急件留给
     * 没人挑的那一堆。</p>
     *
     * <p>也没有单独的「入内授权」页：能不能进门是工单自己的属性，两张工单表都已经带
     * 了这一列，再开一页就是把同一批数据换个列序再列一遍。</p>
     *
     * <p>维修员只有 {@code DORM_REPAIR_WORK} 一条宿舍权限，所以这里不挂任何住宿、
     * 水电、卫生的面板——那些页面对他来说会全部返回无权限，摆上去只是摆几个报错。</p>
     */
    private void buildRepairWorker(ClientBusinessServices services) {
        DormExtClientService ext = services.dormExt();
        TaskTabs tabs = DormTabs.create();
        tabs.addTask("我的工单", new DormRepairWorkPanel(this, ext, DormRepairWorkPanel.View.ASSIGNED));
        tabs.addTask("处理记录", new DormRepairWorkPanel(this, ext, DormRepairWorkPanel.View.HISTORY));
        DormExtTabHeights.fitToSelectedTab(tabs);
        DormUi.flatten(tabs);
        addBlock(tabs);
    }

    private void buildManager(ClientBusinessServices services) {
        DormExtClientService ext = services.dormExt();
        TaskTabs tabs = DormTabs.create();
        tabs.addTask("住宿与空间", new DormManagerSpacePanel(this, services.dorm()));
        // 住宿申请单独成块并且带房间平面：它和请假、来访不是一类事——后两者点个头就完了，
        // 住宿申请点头之后还得真的把人放到某张床上，而哪张床空着只有看图才知道。
        // 平面图只长在这里：在「住宿与空间」里也摆一张，等于同一件事有两个入口，
        // 而那一页要回答的是「一共有多少楼、多少房、多少床」，不是「这个人放哪儿」。
        // 下面那张合并表仍然是三类的总览，回答「今天一共有多少待办」。
        tabs.addTask("申请与审批",
                new DormManagerHousingRequestPanel(this, services.dorm()),
                new DormManagerApprovalPage(this, services.dorm(), ext));
        // 报修处理的主线是派单，所以右栏放维修员名单而不是又一组状态按钮。
        tabs.addTask("报修处理", new DormManagerRepairPage(this, services.dorm(), ext));
        // 账单是抄表的结果，两者必须并排看才判断得出哪个房间还没出账。
        tabs.addTask("水电",
                new DormManagerBillingPanel(this, services.dorm()),
                new DormExtBillingPanel(this, ext));
        // 未归与卫生是两件不相干的事，各占一页。
        // 晚归（回来了但超时）与连续未归（一直没回来）是两套数据，互补而非重复；
        // 卫生同理：一边是待检任务与分项打分，一边是历史检查记录。
        tabs.addTask("未归管理",
                new DormExtWarningPanel(this, ext),
                new DormManagerAbsencePanel(this, services.dorm()),
                new DormExtStayAdminPanel(this, ext));
        tabs.addTask("卫生管理",
                new DormExtHygienePanel(this, ext),
                new DormManagerHygieneRecordPanel(this, services.dorm()));
        tabs.addTask("宿舍公告", new DormExtNoticePanel(this, ext, services.dorm(), true));
        tabs.addTask("设置", new DormExtSettingsPanel(this, ext));
        DormExtTabHeights.fitToSelectedTab(tabs);
        DormUi.flatten(tabs);
        addBlock(tabs);
    }
}
