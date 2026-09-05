package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

/** 连续未归预警阈值设置。 */
final class DormExtSettingsWarningPanel extends JPanel {
    private final BasePage page;
    private final DormExtClientService service;
    private final JTextField warnDays = UiFactory.textField(6);
    private final JTextField notifyDays = UiFactory.textField(6);
    private final JComboBox<String> exempt = new JComboBox<String>(new String[]{"豁免", "不豁免"});

    DormExtSettingsWarningPanel(BasePage page, DormExtClientService service) {
        this.page = page; this.service = service; setOpaque(false);
        setLayout(new java.awt.BorderLayout());
        exempt.setFont(DesignTokens.regular(15));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("预警天数", warnDays));
        fields.add(UiFactory.labelledField("通知天数", notifyDays));
        fields.add(UiFactory.labelledField("请假期间", exempt));
        add(DormExtSettingsSupport.wrap(new SectionCard("未归预警阈值",
                        DormExtSettingsSupport.wrapText(
                                "达到预警天数记「一般」，达到通知天数升「严重」；每日 08:00 的自动扫描用这组阈值。",
                                240)), fields,
                DormExtSettingsSupport.button("保存阈值", true, this::save),
                DormExtSettingsSupport.button("重新读取", false, this::reload)), java.awt.BorderLayout.CENTER);
        reload();
    }

    void reload() {
        AsyncTask.run(() -> service.warningConfig(), new AsyncTask.Callback<WarningConfigDto>() {
            @Override public void onSuccess(WarningConfigDto value) {
                warnDays.setText(String.valueOf(value.getWarnDays()));
                notifyDays.setText(String.valueOf(value.getNotifyDays()));
                exempt.setSelectedItem(value.isExemptOnLeave() ? "豁免" : "不豁免");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void save() {
        final WarningConfigRequest request;
        try { request = new WarningConfigRequest(positive(warnDays.getText(), "预警天数"), positive(notifyDays.getText(), "通知天数"), "豁免".equals(exempt.getSelectedItem())); }
        catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); return; }
        AsyncTask.run(() -> service.saveWarningConfig(request), new AsyncTask.Callback<WarningConfigDto>() {
            @Override public void onSuccess(WarningConfigDto value) { reload(); page.showSuccess("预警阈值已保存。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static int positive(String text, String label) {
        Long value = RealUi.number(RealUi.required(text, label));
        if (value == null || value.longValue() <= 0L) throw new IllegalArgumentException(label + "必须是正整数。");
        return value.intValue();
    }
}
