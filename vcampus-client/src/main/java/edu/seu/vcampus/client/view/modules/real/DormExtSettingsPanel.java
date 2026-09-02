package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.threeten.bp.LocalTime;

/**
 * 宿舍模块的参数设置与危险操作。
 *
 * <p>门禁时段、未归预警阈值这类东西调一次能管一学期，房间删除更是不可逆；把它们
 * 和天天要用的抄表、审批、打分放在一起，除了让主界面变长之外只会增加误触的机会。
 * 集中到这一页，主流程页面就只剩当天真正要做的事。</p>
 */
public final class DormExtSettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /**
     * 可手动触发的任务，顺序与服务端登记顺序一致。
     *
     * <p>写成常量而不是从服务端拉一份任务名清单：多加一条命令只为填一个下拉不划算，
     * 而且真正的权威仍是服务端——填错了名字它会直接拒绝。</p>
     */
    private static final String[] TASKS = {"全部", "absenceScan", "warningNotify",
            "hygieneTaskGenerate", "noticeExpireScan", "billGenerate"};

    private final BasePage page;
    private final DormExtClientService service;

    private final JTextField curfew = UiFactory.textField(8);
    private final JTextField dawn = UiFactory.textField(8);
    private final JTextField warnDays = UiFactory.textField(6);
    private final JTextField notifyDays = UiFactory.textField(6);
    private final JComboBox<String> exemptOnLeave =
            new JComboBox<String>(new String[]{"豁免", "不豁免"});
    private final JTextField roomId = UiFactory.textField(8);
    private final JLabel schedulerState = UiFactory.body("—");
    private final JComboBox<String> task = new JComboBox<String>(TASKS);
    private final DefaultTableModel taskModel = new DefaultTableModel(
            new String[]{"任务", "周期", "最近一次", "累计"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public DormExtSettingsPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        exemptOnLeave.setFont(DesignTokens.regular(13));
        task.setFont(DesignTokens.regular(13));
        add(statusCard());
        add(accessCard());
        add(warningCard());
        add(roomCard());
        reload();
    }

    public void reload() {
        loadStatus();
        loadPolicy();
        loadWarningConfig();
    }

    // ---------- 服务端运行状态 ----------

    private JPanel statusCard() {
        SectionCard card = new SectionCard("服务端运行状态",
                "定时任务由服务端按周期自动执行，这里看的是它们的运行台账。"
                        + "最早的一个也要等到次日 08:00，想当场确认链路通不通就用「立即执行」。");
        JTable table = new JTable(taskModel);
        table.setRowHeight(28);
        table.setFont(DesignTokens.regular(13));
        table.setEnabled(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(0, 160));

        JPanel top = UiFactory.horizontal(8);
        top.add(UiFactory.body("调度器"));
        top.add(schedulerState);

        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("任务"));
        line.add(task);
        line.add(button("立即执行", true, new Runnable() {
            @Override public void run() { runTask(); }
        }));
        line.add(button("刷新状态", false, new Runnable() {
            @Override public void run() { loadStatus(); }
        }));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void loadStatus() {
        AsyncTask.run(new AsyncTask.Work<DormExtStatusDto>() {
            @Override public DormExtStatusDto run() throws Exception { return service.status(); }
        }, new AsyncTask.Callback<DormExtStatusDto>() {
            @Override public void onSuccess(DormExtStatusDto value) { showStatus(value); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void runTask() {
        final String selected = String.valueOf(task.getSelectedItem());
        final String taskName = "全部".equals(selected) ? null : selected;
        AsyncTask.run(new AsyncTask.Work<DormExtStatusDto>() {
            @Override public DormExtStatusDto run() throws Exception {
                return service.runScheduledTask(taskName);
            }
        }, new AsyncTask.Callback<DormExtStatusDto>() {
            @Override public void onSuccess(DormExtStatusDto value) {
                showStatus(value);
                page.showSuccess("已执行 " + selected + "，结果见上表。");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void showStatus(DormExtStatusDto value) {
        schedulerState.setText(value.isSchedulerRunning()
                ? "运行中 · " + RealUi.text(value.getModuleVersion())
                        + " · 服务端时间 " + RealUi.dateTime(value.getServerTime())
                : "未启动 · " + RealUi.text(value.getModuleVersion()));
        taskModel.setRowCount(0);
        List<String> lines = value.getScheduledTasks();
        if (lines.isEmpty()) {
            taskModel.addRow(new Object[]{"（调度器未启动）", "", "", ""});
            return;
        }
        for (String line : lines) {
            // 服务端把每个任务拼成「任务 | 周期 | 最近一次 | 累计」四段，这里拆回四列。
            String[] parts = line.split(" \\| ", 4);
            Object[] row = new Object[]{"", "", "", ""};
            System.arraycopy(parts, 0, row, 0, Math.min(parts.length, 4));
            taskModel.addRow(row);
        }
    }

    // ---------- 门禁时段 ----------

    private JPanel accessCard() {
        SectionCard card = new SectionCard("门禁时段",
                "落锁时间到次日开门时间之间归宿记为晚归。改动即时生效，"
                        + "学生端的历史进出记录会按新时段重新判定——流水本身不会被改写。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("落锁时间（HH:mm）", curfew));
        fields.add(UiFactory.labelledField("开门时间（HH:mm）", dawn));
        return wrap(card, fields, button("保存门禁时段", true, new Runnable() {
            @Override public void run() { savePolicy(); }
        }), button("重新读取", false, new Runnable() {
            @Override public void run() { loadPolicy(); }
        }));
    }

    private void loadPolicy() {
        AsyncTask.run(new AsyncTask.Work<AccessPolicyDto>() {
            @Override public AccessPolicyDto run() throws Exception { return service.accessPolicy(); }
        }, new AsyncTask.Callback<AccessPolicyDto>() {
            @Override public void onSuccess(AccessPolicyDto value) { showPolicy(value); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void showPolicy(AccessPolicyDto value) {
        curfew.setText(RealUi.time(value.getCurfewTime()));
        dawn.setText(RealUi.time(value.getDawnTime()));
    }

    private void savePolicy() {
        final LocalTime curfewValue;
        final LocalTime dawnValue;
        try {
            curfewValue = parseTime(curfew.getText(), "落锁时间");
            dawnValue = parseTime(dawn.getText(), "开门时间");
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        AsyncTask.run(new AsyncTask.Work<AccessPolicyDto>() {
            @Override public AccessPolicyDto run() throws Exception {
                return service.saveAccessPolicy(new AccessPolicyRequest(curfewValue, dawnValue));
            }
        }, new AsyncTask.Callback<AccessPolicyDto>() {
            @Override public void onSuccess(AccessPolicyDto value) {
                showPolicy(value);
                page.showSuccess("门禁时段已保存。");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    // ---------- 未归预警阈值 ----------

    private JPanel warningCard() {
        SectionCard card = new SectionCard("未归预警阈值",
                "连续未归达到预警天数记「一般」，达到通知天数升「严重」；"
                        + "已批准的离校请假可以豁免。每日 08:00 的自动扫描用的就是这组阈值。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("预警天数", warnDays));
        fields.add(UiFactory.labelledField("通知天数", notifyDays));
        fields.add(UiFactory.labelledField("请假期间", exemptOnLeave));
        return wrap(card, fields, button("保存阈值", true, new Runnable() {
            @Override public void run() { saveWarningConfig(); }
        }), button("重新读取", false, new Runnable() {
            @Override public void run() { loadWarningConfig(); }
        }));
    }

    private void loadWarningConfig() {
        AsyncTask.run(new AsyncTask.Work<WarningConfigDto>() {
            @Override public WarningConfigDto run() throws Exception { return service.warningConfig(); }
        }, new AsyncTask.Callback<WarningConfigDto>() {
            @Override public void onSuccess(WarningConfigDto value) { showWarningConfig(value); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void showWarningConfig(WarningConfigDto value) {
        warnDays.setText(String.valueOf(value.getWarnDays()));
        notifyDays.setText(String.valueOf(value.getNotifyDays()));
        exemptOnLeave.setSelectedItem(value.isExemptOnLeave() ? "豁免" : "不豁免");
    }

    private void saveWarningConfig() {
        final WarningConfigRequest request;
        try {
            request = new WarningConfigRequest(positive(warnDays.getText(), "预警天数"),
                    positive(notifyDays.getText(), "通知天数"),
                    "豁免".equals(exemptOnLeave.getSelectedItem()));
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        AsyncTask.run(new AsyncTask.Work<WarningConfigDto>() {
            @Override public WarningConfigDto run() throws Exception {
                return service.saveWarningConfig(request);
            }
        }, new AsyncTask.Callback<WarningConfigDto>() {
            @Override public void onSuccess(WarningConfigDto value) {
                showWarningConfig(value);
                page.showSuccess("预警阈值已保存，下次扫描起生效。");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    // ---------- 房间删除 ----------

    private JPanel roomCard() {
        SectionCard card = new SectionCard("删除房间",
                "只能删除既没有在住学生、也没有任何历史记录（住宿、账单、检查、工单、"
                        + "抄表、任务、预警、来访）的房间。删除不可撤销。");
        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("房间编号"));
        line.add(roomId);
        line.add(button("删除房间", false, new Runnable() {
            @Override public void run() { deleteRoom(); }
        }));
        card.setContent(line);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void deleteRoom() {
        Long id = RealUi.number(roomId.getText());
        if (id == null || id.longValue() <= 0L) {
            page.showWarning("房间编号必须是正整数。");
            return;
        }
        if (!RealUi.confirm(this, "删除房间 " + id + " 后不可恢复，确定继续吗？")) {
            return;
        }
        final RoomDeleteRequest request = new RoomDeleteRequest(id.longValue());
        AsyncTask.run(new AsyncTask.Work<Long>() {
            @Override public Long run() throws Exception { return service.deleteRoom(request); }
        }, new AsyncTask.Callback<Long>() {
            @Override public void onSuccess(Long value) {
                page.showSuccess("房间 " + value + " 已删除。");
                roomId.setText("");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    // ---------- 公共零件 ----------

    private JPanel wrap(SectionCard card, JPanel fields, JButton primary, JButton secondary) {
        JPanel line = UiFactory.horizontal(8);
        line.add(secondary);
        line.add(primary);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private static JButton button(String label, boolean primary, final Runnable action) {
        JButton button = primary ? new PrimaryButton(label) : new SecondaryButton(label);
        button.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
        return button;
    }

    private static LocalTime parseTime(String text, String label) {
        String value = RealUi.required(text, label);
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(label + "格式应为 HH:mm。");
        }
    }

    private static int positive(String text, String label) {
        Long value = RealUi.number(RealUi.required(text, label));
        if (value == null || value.longValue() <= 0L) {
            throw new IllegalArgumentException(label + "必须是正整数。");
        }
        return value.intValue();
    }
}
