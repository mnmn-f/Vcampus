package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 系统管理员只读系统监控快照，不包含业务写入口。 */
public final class IdentityMonitorPanel extends SectionCard {
    private final BasePage page; private final IdentityClientService service;
    private final JLabel database = UiFactory.body("加载中…"); private final JLabel sessions = UiFactory.body("--");
    private final JLabel users = UiFactory.body("--"); private final JLabel checked = UiFactory.body("--");
    private final JLabel message = UiFactory.muted(" ");

    public IdentityMonitorPanel(BasePage page, IdentityClientService service) {
        super("系统监控", "数据库健康度、在线会话和用户规模。"); this.page = page; this.service = service;
        JPanel fields = new JPanel(new GridLayout(2, 2, 12, 8)); fields.setOpaque(false);
        fields.add(item("数据库", database)); fields.add(item("在线会话", sessions)); fields.add(item("用户数", users)); fields.add(item("检查时间", checked));
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false); body.add(fields, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton refresh = new PrimaryButton("刷新监控"); refresh.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(); }
        }); actions.add(message); actions.add(refresh); body.add(actions, BorderLayout.SOUTH); setContent(body); load();
    }

    private JPanel item(String label, JLabel value) { JPanel panel = new JPanel(new BorderLayout(0, 4)); panel.setOpaque(false); panel.add(UiFactory.muted(label), BorderLayout.NORTH); panel.add(value, BorderLayout.CENTER); return panel; }

    private void load() {
        message.setText("正在读取监控…");
        AsyncTask.run(new AsyncTask.Work<MonitorSnapshotDto>() {
            @Override public MonitorSnapshotDto run() throws Exception { return service.monitor(); }
        }, new AsyncTask.Callback<MonitorSnapshotDto>() {
            @Override public void onSuccess(MonitorSnapshotDto value) {
                database.setText(value.isDatabaseHealthy() ? "正常" : "异常"); sessions.setText(String.valueOf(value.getActiveSessionCount()));
                users.setText(String.valueOf(value.getUserCount())); checked.setText(RealUi.dateTime(value.getCheckedAt())); message.setText(RealUi.text(value.getMessage())); page.showSuccess("系统监控已刷新。");
            }
            @Override public void onFailure(Throwable error) { message.setText("读取失败"); page.showError(AsyncTask.message(error)); }
        });
    }
}
