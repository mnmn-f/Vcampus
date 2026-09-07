package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormAlertStatus;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** 宿管员未归预警和卫生检查处理。 */
public final class DormManagerGovernancePanel extends JPanel {
    private final BasePage page; private final DormClientService service;
    private final AsyncPagedTable<LateReturnAlertDto> alerts; private final AsyncPagedTable<HygieneInspectionDto> hygiene;
    private final JTextField alertNote = UiFactory.textField(16); private final JTextField room = UiFactory.textField(8); private final JTextField score = UiFactory.textField(8); private final JComboBox<RealUi.CodeOption> result = new JComboBox<RealUi.CodeOption>(RealUi.options("PASS", "FAIL")); private final JTextField issue = UiFactory.textField(14); private final JTextField rectification = UiFactory.textField(14); private final JComboBox<RealUi.CodeOption> hygieneStatus = new JComboBox<RealUi.CodeOption>(RealUi.options("NORMAL", "RECTIFICATION_REQUIRED", "RECTIFIED")); private long hygieneId;

    public DormManagerGovernancePanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; result.setFont(DesignTokens.regular(13)); hygieneStatus.setFont(DesignTokens.regular(13)); alerts = alertTable(); hygiene = hygieneTable(); add(alerts); add(alertActions()); add(hygiene); add(hygieneForm());
    }

    public void reload() { alerts.reload(); hygiene.reload(); }

    private AsyncPagedTable<LateReturnAlertDto> alertTable() {
        return new AsyncPagedTable<LateReturnAlertDto>("未归预警", "按日期和学生筛选异常记录。", "搜索学生或日期", new String[]{"全部状态", "待处理", "已确认", "已清除", "已忽略"}, new String[]{"编号", "学生", "日期", "检测时间", "状态", "备注"}, new AsyncPagedTable.Loader<LateReturnAlertDto>() {
            @Override public PageSlice<LateReturnAlertDto> load(int p, String k, String f) throws Exception { return alertSlice(service.alerts(new DormPageQuery(p, 20, k, alertStatus(f), null, null), null)); }
        }, new AsyncPagedTable.RowMapper<LateReturnAlertDto>() {
            @Override public Object[] values(LateReturnAlertDto row) { return new Object[]{row.getId(), row.getStudentUserId(), RealUi.date(row.getAlertDate()), RealUi.dateTime(row.getDetectedAt()), alertLabel(row.getStatus()), RealUi.text(row.getNote())}; }
        }, null);
    }

    private JPanel alertActions() {
        SectionCard card = new SectionCard("处理未归", "选择预警后确认、清除或忽略。");
        JPanel line = UiFactory.horizontal(8); line.add(UiFactory.body("备注")); line.add(alertNote);
        JButton confirm = new PrimaryButton("确认预警"); confirm.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { handle(DormAlertStatus.CONFIRMED); } }); JButton clear = new SecondaryButton("标记已清除"); clear.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { handle(DormAlertStatus.CLEARED); } }); JButton ignore = new SecondaryButton("忽略"); ignore.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { handle(DormAlertStatus.IGNORED); } }); line.add(confirm); line.add(clear); line.add(ignore);
        card.setContent(line); JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private AsyncPagedTable<HygieneInspectionDto> hygieneTable() {
        return new AsyncPagedTable<HygieneInspectionDto>("卫生检查", "记录房间评分、问题和整改状态。", "搜索房间或检查结果", new String[]{"全部状态", "正常", "待整改", "已整改"}, new String[]{"编号", "房间", "评分", "结果", "问题", "状态", "检查时间"}, new AsyncPagedTable.Loader<HygieneInspectionDto>() {
            @Override public PageSlice<HygieneInspectionDto> load(int p, String k, String f) throws Exception { return hygieneSlice(service.hygiene(new DormPageQuery(p, 20, k, hygieneStatus(f), null, null))); }
        }, new AsyncPagedTable.RowMapper<HygieneInspectionDto>() {
            @Override public Object[] values(HygieneInspectionDto row) { return new Object[]{row.getId(), row.getRoomId(), RealUi.text(row.getScore()), RealUi.status(row.getResult()), RealUi.text(row.getIssueDescription()), RealUi.status(row.getStatus()), RealUi.dateTime(row.getInspectedAt())}; }
        }, new AsyncPagedTable.SelectionListener<HygieneInspectionDto>() {
            @Override public void onSelected(HygieneInspectionDto value) { showHygiene(value); }
        });
    }

    private JPanel hygieneForm() {
        SectionCard card = new SectionCard("卫生检查与整改", "新建或编辑检查记录。"); hygieneStatus.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false); fields.add(UiFactory.labelledField("房间编号", room)); fields.add(UiFactory.labelledField("评分（0-100）", score)); fields.add(UiFactory.labelledField("检查结果", result)); fields.add(UiFactory.labelledField("问题描述", issue)); fields.add(UiFactory.labelledField("状态", hygieneStatus)); fields.add(UiFactory.labelledField("整改备注", rectification));
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { clearHygiene(); } }); JButton save = new PrimaryButton("保存检查"); save.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveHygiene(); } }); actions.add(clear); actions.add(save);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH); card.setContent(content); JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); clearHygiene(); return wrap;
    }

    private void handle(DormAlertStatus status) {
        final LateReturnAlertDto value = alerts.selectedItem(); if (value == null) { page.showWarning("请先选择未归预警。"); return; }
        final DormAlertStatus finalStatus = status;
        AsyncTask.run(new AsyncTask.Work<LateReturnAlertDto>() { @Override public LateReturnAlertDto run() throws Exception { return service.handleAlert(new LateReturnHandleRequest(value.getId(), finalStatus.name(), RealUi.optional(alertNote.getText()))); } }, new AsyncTask.Callback<LateReturnAlertDto>() { @Override public void onSuccess(LateReturnAlertDto result) { page.showSuccess("未归预警已处理。"); alerts.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } });
    }

    private void showHygiene(HygieneInspectionDto value) { if (value == null) { clearHygiene(); return; } hygieneId = value.getId(); room.setText(String.valueOf(value.getRoomId())); score.setText(RealUi.input(value.getScore())); result.setSelectedItem(RealUi.option(value.getResult())); issue.setText(RealUi.input(value.getIssueDescription())); rectification.setText(RealUi.input(value.getRectificationNote())); hygieneStatus.setSelectedItem(RealUi.option(value.getStatus())); }
    private void clearHygiene() { hygieneId = 0L; room.setText(""); score.setText(""); result.setSelectedItem(RealUi.option("PASS")); issue.setText(""); rectification.setText(""); hygieneStatus.setSelectedItem(RealUi.option("NORMAL")); }
    private void saveHygiene() {
        try {
            Long roomId = RealUi.number(RealUi.required(room.getText(), "房间编号")); if (roomId == null) throw new IllegalArgumentException("房间编号必须是整数"); BigDecimal points = new BigDecimal(RealUi.required(score.getText(), "评分"));
            final HygieneInspectionRequest request = new HygieneInspectionRequest(hygieneId, roomId.longValue(), points, RealUi.code(result.getSelectedItem()), RealUi.optional(issue.getText()), RealUi.code(hygieneStatus.getSelectedItem()), RealUi.optional(rectification.getText()));
            AsyncTask.run(new AsyncTask.Work<HygieneInspectionDto>() { @Override public HygieneInspectionDto run() throws Exception { return service.saveHygiene(request); } }, new AsyncTask.Callback<HygieneInspectionDto>() { @Override public void onSuccess(HygieneInspectionDto value) { page.showSuccess("卫生检查已保存。"); hygiene.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } });
        } catch (NumberFormatException ex) { page.showWarning("房间编号或评分格式不正确。"); } catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private static PageSlice<LateReturnAlertDto> alertSlice(DormPage<LateReturnAlertDto> value) { return RealUi.page(value); }
    private static PageSlice<HygieneInspectionDto> hygieneSlice(DormPage<HygieneInspectionDto> value) { return RealUi.page(value); }
    private static String alertStatus(String filter) { if ("待处理".equals(filter)) return "OPEN"; if ("已确认".equals(filter)) return "CONFIRMED"; if ("已清除".equals(filter)) return "CLEARED"; if ("已忽略".equals(filter)) return "IGNORED"; return null; }
    private static String alertLabel(String value) { if ("OPEN".equalsIgnoreCase(value)) return "待处理"; if ("CONFIRMED".equalsIgnoreCase(value)) return "已确认"; if ("CLEARED".equalsIgnoreCase(value)) return "已清除"; if ("IGNORED".equalsIgnoreCase(value)) return "已忽略"; return RealUi.text(value); }
    private static String hygieneStatus(String filter) { if ("正常".equals(filter)) return "NORMAL"; if ("待整改".equals(filter)) return "RECTIFICATION_REQUIRED"; if ("已整改".equals(filter)) return "RECTIFIED"; return null; }
}
