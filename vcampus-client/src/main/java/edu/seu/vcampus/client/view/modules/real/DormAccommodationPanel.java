package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 学生当前住宿、入住/调宿/退宿申请及申请进度。 */
public final class DormAccommodationPanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final JLabel current = UiFactory.muted("住宿信息加载中…");
    private final JComboBox<RealUi.CodeOption> type = new JComboBox<RealUi.CodeOption>(
            RealUi.options("CHECK_IN", "TRANSFER", "CHECK_OUT"));
    private final JTextField recordId = UiFactory.textField(8);
    private final JTextField bedId = UiFactory.textField(8);
    private final JTextField reason = UiFactory.textField(18);
    private final AsyncPagedTable<AccommodationRequestDto> requests;
    private long accommodationId;

    public DormAccommodationPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        add(currentCard()); requests = requests(); add(requestForm()); add(requests); loadCurrent();
    }

    public void reload() { loadCurrent(); requests.reload(); }

    private JPanel currentCard() {
        SectionCard card = new SectionCard("我的住宿", "仅显示本人住宿关系。");
        JPanel content = new JPanel(new BorderLayout()); content.setOpaque(false); content.add(current, BorderLayout.CENTER); card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private JPanel requestForm() {
        SectionCard card = new SectionCard("住宿申请", "入住、调宿和退宿统一提交申请；退宿操作需要确认。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("申请类型", type)); fields.add(UiFactory.labelledField("当前记录编号", recordId));
        fields.add(UiFactory.labelledField("目标床位编号", bedId)); fields.add(UiFactory.labelledField("申请原因", reason));
        JPanel actions = UiFactory.horizontal(8); JButton submit = new PrimaryButton("提交申请"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton checkout = new DangerButton("确认退宿申请"); checkout.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { checkout(); }
        }); actions.add(submit); actions.add(checkout);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.NORTH); content.add(actions, BorderLayout.SOUTH); card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private AsyncPagedTable<AccommodationRequestDto> requests() {
        return new AsyncPagedTable<AccommodationRequestDto>("我的申请", "仅展示你的申请记录。", "按申请类型或原因搜索",
                new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"},
                new String[]{"编号", "类型", "目标床位", "原因", "状态", "提交时间"},
                new AsyncPagedTable.Loader<AccommodationRequestDto>() {
                    @Override public PageSlice<AccommodationRequestDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.requests(new DormPageQuery(p, 20, keyword, requestStatus(filter), null, null), null));
                    }
                }, new AsyncPagedTable.RowMapper<AccommodationRequestDto>() {
                    @Override public Object[] values(AccommodationRequestDto row) { return new Object[]{row.getId(), RealUi.status(row.getRequestType()), RealUi.text(row.getRequestedBedId()),
                            RealUi.text(row.getReason()), RealUi.status(row.getStatus()), RealUi.dateTime(row.getCreatedAt())}; }
                }, null);
    }

    private void loadCurrent() {
        current.setText("住宿信息加载中…");
        AsyncTask.run(new AsyncTask.Work<AccommodationDto>() {
            @Override public AccommodationDto run() throws Exception { return service.myAccommodation(); }
        }, new AsyncTask.Callback<AccommodationDto>() {
            @Override public void onSuccess(AccommodationDto value) { accommodationId = value == null ? 0L : value.getId(); current.setText(value == null ? "当前暂无在住记录。" : "住宿：" + RealUi.text(value.getBuildingName()) + " " + RealUi.text(value.getRoomNo()) + " / 床位 " + RealUi.text(value.getBedNo()) + "　起始 " + RealUi.date(value.getStartDate()) + "　状态：" + RealUi.status(value.getStatus())); if (value != null && recordId.getText().trim().isEmpty()) recordId.setText(String.valueOf(value.getId())); }
            @Override public void onFailure(Throwable error) { current.setText("住宿加载失败：" + AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
        });
    }

    private void submit() {
        String selected = RealUi.code(type.getSelectedItem());
        try {
            Long currentRecord = RealUi.number(recordId.getText()); Long targetBed = RealUi.number(bedId.getText());
            if (selected == null || "--".equals(selected)) throw new IllegalArgumentException("请选择申请类型");
            if ("TRANSFER".equals(selected) && targetBed == null) throw new IllegalArgumentException("调宿申请需要目标床位编号");
            final String finalSelected = selected; final Long finalCurrentRecord = currentRecord; final Long finalTargetBed = targetBed;
            AsyncTask.run(new AsyncTask.Work<AccommodationRequestDto>() {
                @Override public AccommodationRequestDto run() throws Exception { return service.submitRequest(new AccommodationRequest(finalSelected, finalCurrentRecord, finalTargetBed, RealUi.optional(reason.getText()))); }
            }, new AsyncTask.Callback<AccommodationRequestDto>() {
                @Override public void onSuccess(AccommodationRequestDto value) { page.showSuccess("住宿申请已提交。"); requests.reload(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private void checkout() {
        type.setSelectedItem(RealUi.option("CHECK_OUT"));
        if (accommodationId == 0L) { page.showWarning("当前没有可退宿的住宿记录。"); return; }
        if (!RealUi.confirm(this, "确认提交退宿申请？")) return;
        submit();
    }

    private static PageSlice<AccommodationRequestDto> slice(DormPage<AccommodationRequestDto> value) { return RealUi.page(value); }
    private static String requestStatus(String filter) { if ("待审批".equals(filter)) return "PENDING"; if ("已通过".equals(filter)) return "APPROVED"; if ("已驳回".equals(filter)) return "REJECTED"; if ("已取消".equals(filter)) return "CANCELLED"; return null; }
}
