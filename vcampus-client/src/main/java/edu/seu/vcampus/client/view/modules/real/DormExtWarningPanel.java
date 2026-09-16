package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormTeacherDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.threeten.bp.LocalDate;
/**
 * 宿管员连续未归预警：扫描、通知辅导员与核实。
 *
 * <p>一条预警只有三个状态，顺着走：待处理 → 已通知 → 已核实。
 * 「通知辅导员」记下通知了谁、什么时候，预警变成已通知；「标记已核实」表示宿管已经
 * 弄清这个学生的去向（联系上本人/辅导员、或确认有假条），这条预警就此关闭。
 * 通知不是必经步骤——能直接联系上学生就可以跳过通知直接核实；但已核实的预警不能再通知。</p>
 *
 * <p>辅导员从名单里选，不再手填账号编号：和报修派单选维修员是同一种交互。系统里
 * 没有学生到辅导员的映射，所以名单是全部启用中的教师账号。</p>
 */
public final class DormExtWarningPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<AbsenceWarningDto> warnings;

    private final JTextField scanDate = UiFactory.textField(10);
    private final JComboBox<TeacherOption> teacher = new JComboBox<TeacherOption>();
    private final JTextField note = UiFactory.textField(16);
    public DormExtWarningPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.warnings = warningTable();
        add(scanCard());
        add(javax.swing.Box.createVerticalStrut(20));
        add(DormUi.split(warnings, actions(), 430));
        loadTeachers();
    }
    public void reload() { warnings.reload(); loadTeachers(); }

    private void loadTeachers() {
        AsyncTask.run(new AsyncTask.Work<java.util.List<DormTeacherDto>>() {
            @Override public java.util.List<DormTeacherDto> run() throws Exception { return service.warningTeachers(); }
        }, new AsyncTask.Callback<java.util.List<DormTeacherDto>>() {
            @Override public void onSuccess(java.util.List<DormTeacherDto> value) { fillTeachers(value); }
            @Override public void onFailure(Throwable error) { fillTeachers(null); }
        });
    }

    private void fillTeachers(java.util.List<DormTeacherDto> value) {
        Object current = teacher.getSelectedItem();
        long keep = current instanceof TeacherOption ? ((TeacherOption) current).id : 0L;
        teacher.removeAllItems();
        if (value == null || value.isEmpty()) {
            teacher.addItem(new TeacherOption(0L, value == null ? "辅导员名单加载失败" : "暂无可通知的教师账号"));
            return;
        }
        teacher.addItem(new TeacherOption(0L, "请选择辅导员"));
        for (DormTeacherDto item : value) {
            TeacherOption option = new TeacherOption(item.getUserId(), item.summary());
            teacher.addItem(option);
            if (option.id == keep) teacher.setSelectedItem(option);
        }
    }
    /** 手动补扫指定日期；自动扫描由服务端完成。 */
    private JPanel scanCard() {
        JPanel line = UiFactory.horizontal(9);
        line.add(UiFactory.body("补扫日期（留空为今天）"));
        line.add(scanDate);
        JButton scan = new PrimaryButton("立即扫描");
        scan.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { scan(); }
        });
        line.add(scan);
        JPanel box = DormUi.panel();
        box.add(line, BorderLayout.CENTER);
        box.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 70));
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("连续未归预警",
                "每日 08:00 自动扫描；这里可以补扫某一天。预警阈值在「设置」里调。", null, false));
        column.add(box);
        return column;
    }
    /** 预警表每页几条：五条一页，表格高度固定。 */
    private static final int PAGE_ROWS = 5;

    private AsyncPagedTable<AbsenceWarningDto> warningTable() {
        AsyncPagedTable<AbsenceWarningDto> table = new AsyncPagedTable<AbsenceWarningDto>("连续未归预警",
                "按扫描日记录，同一学生同一扫描日只留一条。",
                "搜索房间或楼栋",
                new String[]{"全部状态", "待处理", "已通知", "已核实"},
                new String[]{"编号", "学生", "楼栋", "房间", "扫描日", "未归天数", "级别", "状态", "通知辅导员"},
                new AsyncPagedTable.Loader<AbsenceWarningDto>() {
                    @Override
                    public PageSlice<AbsenceWarningDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<AbsenceWarningDto> value = service.warnings(
                                new DormPageQuery(p, PAGE_ROWS, keyword, statusCode(filter), null, null));
                        return RealUi.page(value);
                    }
                },
                new AsyncPagedTable.RowMapper<AbsenceWarningDto>() {
                    @Override
                    public Object[] values(AbsenceWarningDto row) {
                        return new Object[]{row.getId(), RealUi.text(row.getStudentLabel()),
                                RealUi.text(row.getBuildingCode()), RealUi.text(row.getRoomNo()),
                                RealUi.date(row.getScanDate()), Integer.valueOf(row.getAbsenceDays()),
                                levelLabel(row.getWarningLevel()), statusLabel(row.getHandleStatus()),
                                row.getNotifiedTeacherId() == null ? "未通知"
                                        : (row.getNotifiedTeacherLabel() == null ? "已通知" : "已通知 " + row.getNotifiedTeacherLabel())};
                    }
                }, null);
        table.setPageRows(PAGE_ROWS);
        return table;
    }
    /** 右栏：选中一条预警后在这里通知辅导员或标记已核实。 */
    private JPanel actions() {
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        teacher.setFont(edu.seu.vcampus.client.ui.DesignTokens.regular(13));
        fields.add(labelled("通知给哪位辅导员", teacher));
        fields.add(javax.swing.Box.createVerticalStrut(11));
        fields.add(labelled("处理备注", note));
        fields.add(javax.swing.Box.createVerticalStrut(11));
        fields.add(hint("通知辅导员：记下已把这条预警转给谁，状态变为「已通知」。"));
        fields.add(hint("标记已核实：已弄清学生去向（联系上本人/辅导员或确认有假条），预警就此关闭。"));

        JButton notify = new PrimaryButton("通知辅导员");
        notify.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { notifyTeacher(); }
        });
        JButton verify = new SecondaryButton("标记已核实");
        verify.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { verify(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(notify);
        buttons.add(verify);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(fields);
        rows.add(javax.swing.Box.createVerticalStrut(14));
        rows.add(buttons);
        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("处理预警",
                "先在左表选中一条。流程：待处理 → 已通知 → 已核实；能直接联系上学生的可以跳过通知直接核实。", null, false));
        column.add(box);
        return column;
    }

    private static javax.swing.JLabel hint(String text) {
        javax.swing.JLabel label = UiFactory.muted(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel labelled(String label, java.awt.Component control) {
        JPanel holder = new JPanel(new BorderLayout(0, 5));
        holder.setOpaque(false);
        holder.add(DormUi.caption(label), BorderLayout.NORTH);
        holder.add(control, BorderLayout.CENTER);
        holder.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 62));
        return holder;
    }
    private void scan() {
        final LocalDate date;
        try {
            String text = RealUi.optional(scanDate.getText());
            date = text == null ? null : LocalDate.parse(text.trim());
        } catch (RuntimeException ex) {
            page.showWarning("扫描日期格式应为 yyyy-MM-dd。");
            return;
        }
        AsyncTask.run(new AsyncTask.Work<WarningScanResultDto>() {
            @Override public WarningScanResultDto run() throws Exception {
                return service.scanAbsences(new WarningScanRequest(date));
            }
        }, new AsyncTask.Callback<WarningScanResultDto>() {
            @Override public void onSuccess(WarningScanResultDto value) {
                page.showSuccess("扫描 " + value.getScanDate() + "：在住 "
                        + value.getResidentsScanned() + " 人，预警 " + value.getWarningCount()
                        + " 条（一般 " + value.getNormalCount() + " / 严重 " + value.getSevereCount()
                        + " / 已豁免 " + value.getExemptCount() + "）。");
                warnings.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
    private void notifyTeacher() {
        final AbsenceWarningDto selected = warnings.selectedItem();
        if (selected == null) { page.showWarning("请先选择一条预警。"); return; }
        Object option = teacher.getSelectedItem();
        if (!(option instanceof TeacherOption) || ((TeacherOption) option).id <= 0L) {
            page.showWarning("请先选择要通知的辅导员。");
            return;
        }
        final Long teacherId = Long.valueOf(((TeacherOption) option).id);
        handle(new WarningHandleRequest(selected.getId(), teacherId, RealUi.optional(note.getText())),
                true, "已通知 " + option + "，预警转为「已通知」。");
    }
    private void verify() {
        final AbsenceWarningDto selected = warnings.selectedItem();
        if (selected == null) { page.showWarning("请先选择一条预警。"); return; }
        handle(new WarningHandleRequest(selected.getId(), null, RealUi.optional(note.getText())),
                false, "预警已核实并关闭。");
    }
    private void handle(final WarningHandleRequest request, final boolean notify, final String ok) {
        AsyncTask.run(new AsyncTask.Work<AbsenceWarningDto>() {
            @Override public AbsenceWarningDto run() throws Exception {
                return notify ? service.notifyWarning(request) : service.verifyWarning(request);
            }
        }, new AsyncTask.Callback<AbsenceWarningDto>() {
            @Override public void onSuccess(AbsenceWarningDto value) {
                page.showSuccess(ok);
                note.setText("");
                warnings.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
    private static String statusCode(String filter) {
        if ("待处理".equals(filter)) return AbsenceWarningDto.STATUS_PENDING;
        if ("已通知".equals(filter)) return AbsenceWarningDto.STATUS_NOTIFIED;
        if ("已核实".equals(filter)) return AbsenceWarningDto.STATUS_VERIFIED;
        return null;
    }
    private static String statusLabel(String value) {
        if (AbsenceWarningDto.STATUS_NOTIFIED.equals(value)) return "已通知";
        if (AbsenceWarningDto.STATUS_VERIFIED.equals(value)) return "已核实";
        return "待处理";
    }
    private static final class TeacherOption {
        private final long id; private final String label;
        TeacherOption(long id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
    private static String levelLabel(String value) {
        if (AbsenceWarningDto.LEVEL_SEVERE.equals(value)) return "严重";
        if (AbsenceWarningDto.LEVEL_EXEMPT.equals(value)) return "已豁免";
        return "一般";
    }
}
