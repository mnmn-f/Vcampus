package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormAlertStatus;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 晚归记录及其处理。
 *
 * <p>和「连续未归预警」是两回事：晚归是人回来了但过了门禁点，连续未归是一直没回来。
 * 两者互补，所以同页上下排，中间有分隔线；合成一张表会让「今晚有几个人没回来」这个
 * 问题再也问不出来。</p>
 */
public final class DormManagerAbsencePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormClientService service;
    private final AsyncPagedTable<LateReturnAlertDto> alerts;
    private final JTextField note = UiFactory.textField(18);

    public DormManagerAbsencePanel(BasePage page, DormClientService service) {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());
        this.page = page;
        this.service = service;
        alerts = table();
        add(DormUi.split(left(), actions(), 430), BorderLayout.CENTER);
    }

    public void reload() { alerts.reload(); }

    private JPanel left() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("晚归记录",
                "人回来了但过了门禁点；与「连续未归」是两套数据，互补而非重复。", null, false));
        column.add(alerts);
        return column;
    }

    private AsyncPagedTable<LateReturnAlertDto> table() {
        return new AsyncPagedTable<LateReturnAlertDto>("", "", "搜索学生或日期",
                new String[]{"全部状态", "待处理", "已确认", "已清除", "已忽略"},
                new String[]{"编号", "学生", "日期", "检测时间", "状态", "备注"},
                new AsyncPagedTable.Loader<LateReturnAlertDto>() {
                    @Override public PageSlice<LateReturnAlertDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.alerts(new DormPageQuery(p, 20, k, alertStatus(f), null, null), null));
                    }
                }, new AsyncPagedTable.RowMapper<LateReturnAlertDto>() {
                    @Override public Object[] values(LateReturnAlertDto row) {
                        return new Object[]{Long.valueOf(row.getId()), "晚归学生",
                                RealUi.date(row.getAlertDate()), RealUi.dateTime(row.getDetectedAt()),
                                alertLabel(row.getStatus()), RealUi.text(row.getNote())};
                    }
                }, null);
    }

    private JPanel actions() {
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        field.add(DormUi.caption("处理备注"), BorderLayout.NORTH);
        field.add(note, BorderLayout.CENTER);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(button("确认", DormAlertStatus.CONFIRMED, true));
        buttons.add(button("已清除", DormAlertStatus.CLEARED, false));
        buttons.add(button("忽略", DormAlertStatus.IGNORED, false));

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(field);
        rows.add(Box.createVerticalStrut(14));
        rows.add(buttons);
        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("处理晚归", "选中一条记录后确认、清除或忽略。", null, false));
        column.add(box);
        return column;
    }

    private JButton button(String label, final DormAlertStatus status, boolean primary) {
        JButton button = primary ? new PrimaryButton(label) : new SecondaryButton(label);
        button.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handle(status); }
        });
        return button;
    }

    private void handle(final DormAlertStatus status) {
        final LateReturnAlertDto value = alerts.selectedItem();
        if (value == null) { page.showWarning("请先选择一条晚归记录。"); return; }
        AsyncTask.run(new AsyncTask.Work<LateReturnAlertDto>() {
            @Override public LateReturnAlertDto run() throws Exception {
                return service.handleAlert(new LateReturnHandleRequest(value.getId(), status.name(),
                        RealUi.optional(note.getText())));
            }
        }, new AsyncTask.Callback<LateReturnAlertDto>() {
            @Override public void onSuccess(LateReturnAlertDto result) {
                page.showSuccess("晚归记录已处理。");
                note.setText("");
                alerts.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String alertStatus(String filter) {
        if ("待处理".equals(filter)) return "OPEN";
        if ("已确认".equals(filter)) return "CONFIRMED";
        if ("已清除".equals(filter)) return "CLEARED";
        if ("已忽略".equals(filter)) return "IGNORED";
        return null;
    }

    private static String alertLabel(String value) {
        if ("OPEN".equalsIgnoreCase(value)) return "待处理";
        if ("CONFIRMED".equalsIgnoreCase(value)) return "已确认";
        if ("CLEARED".equalsIgnoreCase(value)) return "已清除";
        if ("IGNORED".equalsIgnoreCase(value)) return "已忽略";
        return RealUi.text(value);
    }
}
