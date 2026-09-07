package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** SRTP 学生本人查询/提交与教务老师维护、审核。 */
public final class CampusSrtpPanel extends JPanel {
    private final BasePage page; private final CampusClientService service; private final Role role;
    private final AsyncPagedTable<SrtpRecordDto> table; private final CampusSrtpEditorPanel editor;
    private final JComboBox<RealUi.CodeOption> reviewStatus = new JComboBox<RealUi.CodeOption>(RealUi.options("APPROVED", "REJECTED", "CANCELLED"));
    private final JTextField remark = UiFactory.textField(18); private final JLabel detail = UiFactory.muted("选择 SRTP 记录查看详情。");

    public CampusSrtpPanel(BasePage page, CampusClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role; boolean admin = role == Role.ACADEMIC_ADMIN;
        editor = new CampusSrtpEditorPanel(admin, new CampusSrtpEditorPanel.Listener() {
            @Override public void onSave(SrtpSaveRequest request) { save(request); }
        }); table = createTable(); add(table); add(editor); add(detail);
        if (admin) add(reviewCard());
    }

    private AsyncPagedTable<SrtpRecordDto> createTable() {
        final boolean admin = role == Role.ACADEMIC_ADMIN;
        String[] columns = admin ? new String[]{"学生编号", "项目编号", "项目名称", "学分", "状态", "提交时间"}
                : new String[]{"项目编号", "项目名称", "学分", "状态", "提交时间"};
        AsyncPagedTable<SrtpRecordDto> value = new AsyncPagedTable<SrtpRecordDto>("SRTP 项目",
                admin ? "教务老师维护项目并审核学生记录。" : "只显示当前学生的 SRTP 记录，可在下方提交项目。", "搜索项目编号或名称",
                new String[]{"全部状态", "已提交", "已通过", "已驳回"}, columns,
                new AsyncPagedTable.Loader<SrtpRecordDto>() {
                    @Override public PageSlice<SrtpRecordDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(admin ? service.listSrtp(query(p, keyword, filter)) : service.mySrtp(query(p, keyword, filter)));
                    }
                }, new AsyncPagedTable.RowMapper<SrtpRecordDto>() {
                    @Override public Object[] values(SrtpRecordDto row) { return admin ? new Object[]{row.getStudentUserId(), row.getProjectCode(), row.getTitle(), RealUi.text(row.getCredits()),
                            RealUi.status(row.getStatus()), RealUi.dateTime(row.getSubmittedAt())} : new Object[]{row.getProjectCode(), row.getTitle(),
                            RealUi.text(row.getCredits()), RealUi.status(row.getStatus()), RealUi.dateTime(row.getSubmittedAt())}; }
                }, new AsyncPagedTable.SelectionListener<SrtpRecordDto>() {
                    @Override public void onSelected(SrtpRecordDto row) { select(row); }
                });
        return value;
    }

    private JPanel reviewCard() {
        SectionCard card = new SectionCard("SRTP 审核", "选中待审核记录后选择结果并提交审核意见。");
        JPanel content = UiFactory.horizontal(8); reviewStatus.setFont(edu.seu.vcampus.client.ui.DesignTokens.regular(13));
        content.add(UiFactory.body("审核结果")); content.add(reviewStatus); content.add(UiFactory.labelledField("备注", remark));
        JButton submit = new PrimaryButton("提交审核"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(); }
        }); content.add(submit); card.setContent(content); return card;
    }

    private CampusPageQuery query(int pageNumber, String keyword, String filter) {
        String status = "已提交".equals(filter) ? "SUBMITTED" : "已通过".equals(filter) ? "APPROVED" : "已驳回".equals(filter) ? "REJECTED" : null;
        return new CampusPageQuery(pageNumber, 20, keyword, status);
    }

    private void select(SrtpRecordDto value) {
        if (value == null) { detail.setText("选择 SRTP 记录查看详情。"); editor.startNew(); return; }
        editor.showRecord(value); detail.setText("详情：" + RealUi.text(value.getTitle()) + "　" + RealUi.text(value.getDescription()));
    }

    private void save(SrtpSaveRequest request) {
        final SrtpSaveRequest finalRequest = request;
        AsyncTask.run(new AsyncTask.Work<SrtpRecordDto>() {
            @Override public SrtpRecordDto run() throws Exception { return service.saveSrtp(finalRequest); }
        }, new AsyncTask.Callback<SrtpRecordDto>() {
            @Override public void onSuccess(SrtpRecordDto value) {
                page.showSuccess("SRTP 项目已保存。"); editor.showRecord(value);
                detail.setText("详情：" + RealUi.text(value.getTitle()) + "　" + RealUi.text(value.getDescription()));
                table.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void review() {
        final SrtpRecordDto value = table.selectedItem(); if (value == null) { page.showWarning("请先选择待审核项目。"); return; }
        final String status = RealUi.code(reviewStatus.getSelectedItem());
        AsyncTask.run(new AsyncTask.Work<SrtpRecordDto>() {
            @Override public SrtpRecordDto run() throws Exception { return service.reviewSrtp(new SrtpStatusRequest(value.getId(), status, RealUi.optional(remark.getText()))); }
        },
                new AsyncTask.Callback<SrtpRecordDto>() {
                    @Override public void onSuccess(SrtpRecordDto result) { page.showSuccess("SRTP 审核结果已保存。"); table.reload(); }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }
}
