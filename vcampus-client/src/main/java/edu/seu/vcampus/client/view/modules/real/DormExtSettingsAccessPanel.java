package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;
import org.threeten.bp.LocalTime;

/** 门禁落锁和开门时段设置。 */
final class DormExtSettingsAccessPanel extends JPanel {
    private final BasePage page;
    private final DormExtClientService service;
    private final JTextField curfew = UiFactory.textField(8);
    private final JTextField dawn = UiFactory.textField(8);

    DormExtSettingsAccessPanel(BasePage page, DormExtClientService service) {
        this.page = page; this.service = service; setOpaque(false);
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("落锁时间（HH:mm）", curfew));
        fields.add(UiFactory.labelledField("开门时间（HH:mm）", dawn));
        add(DormExtSettingsSupport.wrap(new SectionCard("门禁时段", "改动即时生效。"), fields,
                DormExtSettingsSupport.button("保存门禁时段", true, this::save),
                DormExtSettingsSupport.button("重新读取", false, this::reload)));
        reload();
    }

    void reload() {
        AsyncTask.run(() -> service.accessPolicy(), new AsyncTask.Callback<AccessPolicyDto>() {
            @Override public void onSuccess(AccessPolicyDto value) {
                curfew.setText(RealUi.time(value.getCurfewTime())); dawn.setText(RealUi.time(value.getDawnTime()));
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void save() {
        final AccessPolicyRequest request;
        try { request = new AccessPolicyRequest(time(curfew.getText(), "落锁时间"), time(dawn.getText(), "开门时间")); }
        catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); return; }
        AsyncTask.run(() -> service.saveAccessPolicy(request), new AsyncTask.Callback<AccessPolicyDto>() {
            @Override public void onSuccess(AccessPolicyDto value) { reload(); page.showSuccess("门禁时段已保存。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static LocalTime time(String text, String label) {
        try { return LocalTime.parse(RealUi.required(text, label).trim()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 HH:mm。"); }
    }
}
