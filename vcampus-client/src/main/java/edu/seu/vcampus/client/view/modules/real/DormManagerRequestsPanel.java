package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;

import javax.swing.JButton;

/** 宿管员审批入住、调宿和退宿申请。 */
public final class DormManagerRequestsPanel extends javax.swing.JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final AsyncPagedTable<AccommodationRequestDto> requests;

    public DormManagerRequestsPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; requests = table(); add(requests);
    }

    public void reload() { requests.reload(); }

    private AsyncPagedTable<AccommodationRequestDto> table() {
        AsyncPagedTable<AccommodationRequestDto> table = new AsyncPagedTable<AccommodationRequestDto>("住宿申请审批", "处理住宿申请。", "搜索学生或申请原因", new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"}, new String[]{"编号", "学生", "类型", "目标床位", "原因", "状态", "提交时间"}, new AsyncPagedTable.Loader<AccommodationRequestDto>() {
            @Override public PageSlice<AccommodationRequestDto> load(int p, String k, String f) throws Exception { return slice(service.requests(new DormPageQuery(p, 20, k, requestStatus(f), null, null), null)); }
        }, new AsyncPagedTable.RowMapper<AccommodationRequestDto>() {
            @Override public Object[] values(AccommodationRequestDto row) { return new Object[]{row.getId(), row.getStudentUserId(), RealUi.status(row.getRequestType()), RealUi.text(row.getRequestedBedId()), RealUi.text(row.getReason()), RealUi.status(row.getStatus()), RealUi.dateTime(row.getCreatedAt())}; }
        }, null);
        JButton approve = new PrimaryButton("通过申请"); approve.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(true); } }); JButton reject = new DangerButton("驳回申请"); reject.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(false); } }); table.addAction(approve); table.addAction(reject); return table;
    }

    private void review(final boolean approved) {
        final AccommodationRequestDto value = requests.selectedItem(); if (value == null) { page.showWarning("请先选择申请。"); return; }
        AsyncTask.run(new AsyncTask.Work<AccommodationRequestDto>() { @Override public AccommodationRequestDto run() throws Exception { return service.approveRequest(new DormApprovalRequest(value.getId(), approved, null)); } }, new AsyncTask.Callback<AccommodationRequestDto>() {
            @Override public void onSuccess(AccommodationRequestDto result) { page.showSuccess(approved ? "申请已通过。" : "申请已驳回。"); requests.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static PageSlice<AccommodationRequestDto> slice(DormPage<AccommodationRequestDto> value) { return RealUi.page(value); }
    private static String requestStatus(String filter) { if ("待审批".equals(filter)) return "PENDING"; if ("已通过".equals(filter)) return "APPROVED"; if ("已驳回".equals(filter)) return "REJECTED"; if ("已取消".equals(filter)) return "CANCELLED"; return null; }
}
