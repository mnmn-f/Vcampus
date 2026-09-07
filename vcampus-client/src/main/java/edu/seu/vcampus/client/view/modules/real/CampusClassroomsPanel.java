package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 教室查询、学生/教师申请与本人记录、教务审批。 */
public final class CampusClassroomsPanel extends JPanel {
    private final BasePage page; private final CampusClientService service; private final Role role;
    private final AsyncPagedTable<CampusClassroomDto> rooms; private final AsyncPagedTable<ClassroomReservationDto> requests;
    private final CampusClassroomApplyPanel apply; private final JComboBox<RealUi.CodeOption> reviewStatus;
    private final JTextField reviewRemark; private final JLabel detail = UiFactory.muted("选择教室查看详情。");

    public CampusClassroomsPanel(BasePage page, CampusClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        rooms = roomTable(); add(rooms); add(detail);
        boolean reserve = role == Role.STUDENT || role == Role.TEACHER;
        apply = reserve ? new CampusClassroomApplyPanel(new CampusClassroomApplyPanel.Listener() {
            @Override public void onApply(ClassroomReservationRequest request) { apply(request); }
        }) : null;
        requests = reserve ? reservationTable(false) : role == Role.ACADEMIC_ADMIN ? reservationTable(true) : null;
        reviewStatus = role == Role.ACADEMIC_ADMIN ? new JComboBox<RealUi.CodeOption>(RealUi.options("APPROVED", "REJECTED", "CANCELLED")) : null;
        reviewRemark = role == Role.ACADEMIC_ADMIN ? UiFactory.textField(18) : null;
        if (apply != null) add(apply); if (requests != null) add(requests); if (reviewStatus != null) add(reviewCard());
    }

    private AsyncPagedTable<CampusClassroomDto> roomTable() {
        AsyncPagedTable<CampusClassroomDto> value = new AsyncPagedTable<CampusClassroomDto>("教室查询",
                "查看可申请教室的状态、容量和设备信息。", "搜索楼栋、教室或类型",
                new String[]{"全部状态", "空闲", "维护中"}, new String[]{"楼栋", "教室", "类型", "容量", "设备", "状态"},
                new AsyncPagedTable.Loader<CampusClassroomDto>() {
                    @Override public PageSlice<CampusClassroomDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(service.classrooms(new CampusPageQuery(p, 20, keyword, roomStatus(filter))));
                    }
                }, new AsyncPagedTable.RowMapper<CampusClassroomDto>() {
                    @Override public Object[] values(CampusClassroomDto row) { return new Object[]{row.getBuildingName(), row.getRoomNo(), RealUi.status(row.getClassroomType()), row.getCapacity(),
                            RealUi.text(row.getEquipmentDescription()), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<CampusClassroomDto>() {
                    @Override public void onSelected(CampusClassroomDto row) { selectRoom(row); }
                });
        return value;
    }

    private AsyncPagedTable<ClassroomReservationDto> reservationTable(final boolean manager) {
        AsyncPagedTable<ClassroomReservationDto> value = new AsyncPagedTable<ClassroomReservationDto>(
                manager ? "教室申请审批" : "我的教室申请", manager ? "教务老师审批、驳回或撤销申请。" : "只显示当前用户提交的申请记录。",
                "搜索教室、申请人或用途", new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"},
                new String[]{"教室", "申请人", "用途", "时间", "状态"},
                new AsyncPagedTable.Loader<ClassroomReservationDto>() {
                    @Override public PageSlice<ClassroomReservationDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(manager ? service.classroomReservations(query(p, keyword, filter))
                                : service.myClassroomReservations(query(p, keyword, filter)));
                    }
                }, new AsyncPagedTable.RowMapper<ClassroomReservationDto>() {
                    @Override public Object[] values(ClassroomReservationDto row) { return new Object[]{row.getBuildingName() + " " + row.getRoomNo(),
                            manager ? row.getApplicantId() : "本人", row.getPurpose(), RealUi.dateTime(row.getStartAt()) + " - " + RealUi.dateTime(row.getEndAt()),
                            RealUi.status(row.getStatus())}; }
                }, null);
        if (!manager) { JButton cancel = new DangerButton("撤销申请"); cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancel(); }
        }); value.addAction(cancel); }
        return value;
    }

    private JPanel reviewCard() {
        SectionCard card = new SectionCard("教室申请处理", "选择申请后审批或撤销。");
        JPanel content = UiFactory.horizontal(8); reviewStatus.setFont(edu.seu.vcampus.client.ui.DesignTokens.regular(13));
        content.add(UiFactory.body("结果")); content.add(reviewStatus); content.add(UiFactory.labelledField("备注", reviewRemark));
        JButton submit = new PrimaryButton("提交处理"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(); }
        }); content.add(submit); card.setContent(content); return card;
    }

    private CampusPageQuery query(int p, String keyword, String filter) {
        String status = "待审批".equals(filter) ? "PENDING" : "已通过".equals(filter) ? "APPROVED" : "已驳回".equals(filter) ? "REJECTED" : "已取消".equals(filter) ? "CANCELLED" : null;
        return new CampusPageQuery(p, 20, keyword, status);
    }

    private void selectRoom(CampusClassroomDto value) {
        if (value == null) { detail.setText("选择教室查看详情。"); return; }
        detail.setText("教室详情：" + RealUi.text(value.getBuildingName()) + " " + RealUi.text(value.getRoomNo())
                + "　容量 " + value.getCapacity() + "　设备：" + RealUi.text(value.getEquipmentDescription()));
        if (apply != null) apply.selectRoom(value.getId());
    }

    private void apply(final ClassroomReservationRequest request) {
        AsyncTask.run(new AsyncTask.Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run() throws Exception { return service.applyClassroom(request); }
        }, new AsyncTask.Callback<ClassroomReservationDto>() {
            @Override public void onSuccess(ClassroomReservationDto value) { page.showSuccess("教室申请已提交。"); if (requests != null) requests.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void cancel() {
        if (requests == null || requests.selectedItem() == null) { page.showWarning("请先选择本人申请。"); return; }
        final ClassroomReservationDto value = requests.selectedItem(); if (!RealUi.confirm(this, "确认撤销这条教室申请？")) return;
        AsyncTask.run(new AsyncTask.Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run() throws Exception { return service.cancelClassroom(value.getId()); }
        }, new AsyncTask.Callback<ClassroomReservationDto>() {
            @Override public void onSuccess(ClassroomReservationDto result) { page.showSuccess("申请已撤销。"); requests.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void review() {
        if (requests == null || requests.selectedItem() == null) { page.showWarning("请先选择待处理申请。"); return; }
        final ClassroomReservationDto value = requests.selectedItem(); final String status = RealUi.code(reviewStatus.getSelectedItem());
        AsyncTask.run(new AsyncTask.Work<ClassroomReservationDto>() {
            @Override public ClassroomReservationDto run() throws Exception { return service.reviewClassroom(new ClassroomReviewRequest(value.getId(), status, RealUi.optional(reviewRemark.getText()))); }
        },
                new AsyncTask.Callback<ClassroomReservationDto>() {
                    @Override public void onSuccess(ClassroomReservationDto result) { page.showSuccess("教室申请处理结果已保存。"); requests.reload(); }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private static String roomStatus(String filter) { return "空闲".equals(filter) ? "AVAILABLE" : "维护中".equals(filter) ? "MAINTENANCE" : null; }
}
