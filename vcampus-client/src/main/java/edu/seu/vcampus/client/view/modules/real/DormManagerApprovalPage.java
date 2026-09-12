package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.threeten.bp.LocalDateTime;

/**
 * 宿管的申请与审批：住宿、请假、来访三类合并成一张待办表。
 *
 * <p>对宿管来说这三类是同一件事——「有人提了申请，等我点头」。分成三张表意味着一天
 * 要在三处之间来回扫，而且没有任何一处能回答「今天一共有多少待办」。</p>
 *
 * <p>合并在客户端做：三类各自有独立的分页接口，服务端合并要么新造一张联合视图，
 * 要么在网关层拼三次查询——都比这里做贵。代价是每类只取最近若干条，页面上写明了
 * 这一点；待办本来就该是个短列表，长到要翻页就说明积压了。</p>
 */
public final class DormManagerApprovalPage extends JPanel {
    private static final long serialVersionUID = 1L;
    /** 每类最多合并这么多条：三类加起来仍是一屏能扫完的量。 */
    private static final int PER_KIND = 50;

    private static final String KIND_ACCOMMODATION = "住宿";
    private static final String KIND_LEAVE = "请假";
    private static final String KIND_VISITOR = "来访";

    private final BasePage page;
    private final DormClientService service;
    private final DormExtClientService ext;
    private final AsyncPagedTable<ApprovalRow> table;
    private final JTextField remark = UiFactory.textField(26);

    public DormManagerApprovalPage(BasePage page, DormClientService service, DormExtClientService ext) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.ext = ext;
        table = table();
        add(DormUi.header("待我处理的申请",
                "住宿、请假、来访三类合并；每类取最近 " + PER_KIND + " 条，按提交时间倒序。",
                null, false));
        add(table);
        add(Box.createVerticalStrut(16));
        add(reviewBar());
    }

    public void reload() { table.reload(); }

    private AsyncPagedTable<ApprovalRow> table() {
        AsyncPagedTable<ApprovalRow> value = new AsyncPagedTable<ApprovalRow>("", "",
                "搜索学生或内容",
                new String[]{"待审批", "全部状态", "仅住宿", "仅请假", "仅来访"},
                new String[]{"类别", "学生", "位置", "内容", "提交时间", "状态"},
                new AsyncPagedTable.Loader<ApprovalRow>() {
                    @Override public PageSlice<ApprovalRow> load(int p, String keyword, String filter) throws Exception {
                        return slice(collect(keyword, filter), p);
                    }
                }, new AsyncPagedTable.RowMapper<ApprovalRow>() {
                    @Override public Object[] values(ApprovalRow row) {
                        return new Object[]{row.kind, "申请人", row.where, row.content,
                                RealUi.dateTime(row.submittedAt), RealUi.status(row.status)};
                    }
                }, null);
        JButton approve = new PrimaryButton("通过选中");
        approve.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(true); }
        });
        JButton reject = new DangerButton("驳回选中");
        reject.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(false); }
        });
        value.addAction(approve);
        value.addAction(reject);
        return value;
    }

    private JPanel reviewBar() {
        JPanel box = DormUi.panel();
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        field.add(DormUi.caption("审批备注（应用于选中项，可留空）"), BorderLayout.NORTH);
        field.add(remark, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(420, 60));
        row.add(field, BorderLayout.WEST);
        row.add(DormUi.sub("通过与驳回按类别分别调用各自的审批接口，备注会一并写进对应的审核记录。"),
                BorderLayout.CENTER);
        box.add(row, BorderLayout.CENTER);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        return box;
    }

    // ---------- 合并三类待办 ----------

    private List<ApprovalRow> collect(String keyword, String filter) throws Exception {
        boolean pendingOnly = !"全部状态".equals(filter);
        String status = pendingOnly ? "PENDING" : null;
        List<ApprovalRow> rows = new ArrayList<ApprovalRow>();
        if (wants(filter, KIND_ACCOMMODATION)) {
            DormPage<AccommodationRequestDto> value = service.requests(
                    new DormPageQuery(1, PER_KIND, keyword, status, null, null), null);
            if (value != null && value.getItems() != null) {
                for (AccommodationRequestDto item : value.getItems()) rows.add(from(item));
            }
        }
        if (wants(filter, KIND_LEAVE)) {
            DormPage<LeaveRequestDto> value = service.managerLeaves(
                    new LeaveQuery(1, PER_KIND, status, (Long) null, null, null));
            if (value != null && value.getItems() != null) {
                for (LeaveRequestDto item : value.getItems()) rows.add(from(item));
            }
        }
        if (wants(filter, KIND_VISITOR)) {
            DormPage<VisitorRegistrationDto> value = ext.visitors(
                    new DormPageQuery(1, PER_KIND, keyword, pendingOnly ? "PENDING" : null, null, null));
            if (value != null && value.getItems() != null) {
                for (VisitorRegistrationDto item : value.getItems()) rows.add(from(item));
            }
        }
        // 请假接口没有关键字查询，合并后统一在客户端过滤一次，三类的搜索行为才一致。
        if (keyword != null && keyword.trim().length() > 0) {
            String needle = keyword.trim();
            List<ApprovalRow> matched = new ArrayList<ApprovalRow>();
            for (ApprovalRow row : rows) if (row.matches(needle)) matched.add(row);
            rows = matched;
        }
        Collections.sort(rows, new Comparator<ApprovalRow>() {
            @Override public int compare(ApprovalRow left, ApprovalRow right) {
                if (left.submittedAt == null && right.submittedAt == null) return 0;
                if (left.submittedAt == null) return 1;
                if (right.submittedAt == null) return -1;
                return right.submittedAt.compareTo(left.submittedAt);
            }
        });
        return rows;
    }

    private static boolean wants(String filter, String kind) {
        if (filter == null || filter.startsWith("待审批") || "全部状态".equals(filter)) return true;
        return ("仅" + kind).equals(filter);
    }

    private static PageSlice<ApprovalRow> slice(List<ApprovalRow> rows, int pageNumber) {
        int size = 20;
        int from = Math.min((pageNumber - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new PageSlice<ApprovalRow>(new ArrayList<ApprovalRow>(rows.subList(from, to)),
                rows.size(), pageNumber, size);
    }

    private static ApprovalRow from(AccommodationRequestDto item) {
        ApprovalRow row = new ApprovalRow();
        row.kind = KIND_ACCOMMODATION;
        row.id = item.getId();
        row.where = item.getRequestedBedId() == null ? "—" : "床位 " + item.getRequestedBedId();
        row.content = RealUi.status(item.getRequestType())
                + (item.getReason() == null || item.getReason().trim().isEmpty()
                        ? "" : " · " + item.getReason().trim());
        row.submittedAt = item.getCreatedAt();
        row.status = item.getStatus();
        return row;
    }

    private static ApprovalRow from(LeaveRequestDto item) {
        ApprovalRow row = new ApprovalRow();
        row.kind = KIND_LEAVE;
        row.id = item.getId();
        row.where = "—";
        row.content = RealUi.status(item.getLeaveType()) + " " + RealUi.dateTime(item.getStartAt())
                + " → " + RealUi.dateTime(item.getEndAt());
        row.submittedAt = item.getCreatedAt();
        row.status = item.getStatus();
        return row;
    }

    private static ApprovalRow from(VisitorRegistrationDto item) {
        ApprovalRow row = new ApprovalRow();
        row.kind = KIND_VISITOR;
        row.id = item.getId();
        row.where = RealUi.text(item.getBuildingCode()) + " " + RealUi.text(item.getRoomNo());
        row.content = RealUi.text(item.getVisitorName()) + " · " + RealUi.text(item.getVisitorIdCardMasked())
                + " · " + RealUi.text(item.getVisitReason());
        row.submittedAt = item.getSubmittedAt();
        row.status = item.getAuditStatus();
        return row;
    }

    // ---------- 审批 ----------

    private void review(final boolean approved) {
        final ApprovalRow row = table.selectedItem();
        if (row == null) { page.showWarning("请先选中一条申请。"); return; }
        if (!"PENDING".equalsIgnoreCase(row.status)) { page.showWarning("只有待审批的申请可以处理。"); return; }
        if (!RealUi.confirm(this, (approved ? "确认通过这条" : "确认驳回这条") + row.kind + "申请？")) return;
        final String note = RealUi.optional(remark.getText());
        AsyncTask.run(new AsyncTask.Work<Object>() {
            @Override public Object run() throws Exception {
                if (KIND_ACCOMMODATION.equals(row.kind)) {
                    return service.approveRequest(new DormApprovalRequest(row.id, approved, note));
                }
                if (KIND_LEAVE.equals(row.kind)) {
                    return service.reviewLeave(new LeaveReviewRequest(row.id, approved, note));
                }
                return ext.auditVisitor(new VisitorAuditRequest(row.id, approved, note));
            }
        }, new AsyncTask.Callback<Object>() {
            @Override public void onSuccess(Object value) {
                page.showSuccess(row.kind + "申请已" + (approved ? "通过" : "驳回") + "。");
                remark.setText("");
                table.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    /** 三类申请在这一页里的统一形状；只带表格要显示和审批要用到的字段。 */
    private static final class ApprovalRow {
        private String kind;
        private long id;
        private String where;
        private String content;
        private LocalDateTime submittedAt;
        private String status;

        boolean matches(String needle) {
            return (content != null && content.contains(needle))
                    || (where != null && where.contains(needle));
        }
    }
}
