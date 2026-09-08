package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 教务管理员选中课程后的排课行内维护区。 */
public final class CourseScheduleEditorPanel extends SectionCard {
    private static final String[] WEEKDAYS = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private final BasePage page; private final AcademicClientService service;
    private final Runnable changed;
    private final JLabel course = UiFactory.body("请先选择课程。"), error = UiFactory.muted(" ");
    private final JTextField scheduleId = UiFactory.textField(10), startDate = UiFactory.textField(10), endDate = UiFactory.textField(10), classroom = UiFactory.textField(10);
    private final JComboBox<String> weekday = new JComboBox<String>(WEEKDAYS);
    private final JSpinner startPeriod = spinner(), endPeriod = spinner();
    private final DefaultTableModel model = model(); private final JTable table = new JTable(model);
    private final JButton newButton = new SecondaryButton("新建时段"), saveButton = new PrimaryButton("保存时段"), deleteButton = new DangerButton("删除时段");
    private List<CourseScheduleDto> schedules = Collections.emptyList();
    private long courseId; private long selectedId; private boolean busy;
    public CourseScheduleEditorPanel(BasePage page, AcademicClientService service) {
        this(page, service, null);
    }
    public CourseScheduleEditorPanel(BasePage page, AcademicClientService service,
                                     Runnable changed) {
        super("排课维护", "维护当前课程的上课时段；冲突时提示。");
        this.page = page; this.service = service; this.changed = changed; scheduleId.setEditable(false);
        scheduleId.setToolTipText("上课时段编号由系统生成"); startDate.setToolTipText("格式：yyyy-MM-dd，可留空"); endDate.setToolTipText("格式：yyyy-MM-dd，可留空");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(36); table.setFillsViewportHeight(true); table.setShowGrid(false);
        table.setFont(DesignTokens.regular(13)); table.setForeground(DesignTokens.TEXT_PRIMARY);
        table.setSelectionBackground(DesignTokens.PRIMARY_LIGHT); table.setSelectionForeground(DesignTokens.TEXT_PRIMARY);
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) select(table.getSelectedRow());
            }
        });
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setPreferredSize(new Dimension(800, 160)); scroll.setMinimumSize(new Dimension(0, 120));
        table.getTableHeader().setFont(DesignTokens.medium(13)); table.getTableHeader().setForeground(DesignTokens.TEXT_PRIMARY);
        table.getTableHeader().setBackground(DesignTokens.PRIMARY_LIGHT); table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "时段编号", scheduleId); add(fields, "星期", weekday); add(fields, "开始节次", startPeriod); add(fields, "结束节次", endPeriod);
        add(fields, "起始日期（可选）", startDate); add(fields, "结束日期（可选）", endDate); add(fields, "教室 ID（可选）", classroom);
        JPanel actions = UiFactory.horizontal(8); newButton.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { newSchedule(); }
        }); saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); deleteButton.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { delete(); }
        });
        actions.add(newButton); actions.add(saveButton); actions.add(deleteButton); actions.add(error);
        JPanel lower = new JPanel(new BorderLayout(0, 10)); lower.setOpaque(false); lower.add(scroll, BorderLayout.NORTH); lower.add(fields, BorderLayout.CENTER); lower.add(actions, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(course, BorderLayout.NORTH); content.add(lower, BorderLayout.CENTER); setContent(content);
        setEditorEnabled(false); clearForm();
    }
    public void showCourse(CourseDto value) {
        courseId = value == null ? 0L : value.getId(); schedules = value == null ? Collections.<CourseScheduleDto>emptyList() : new ArrayList<CourseScheduleDto>(value.getSchedules());
        course.setText(value == null ? "请先选择课程。" : "当前课程：" + RealUi.text(value.getCourseCode()) + "　" + RealUi.text(value.getCourseName()) + "（" + schedules.size() + " 条时段）");
        refreshRows(); setEditorEnabled(courseId > 0L); if (courseId > 0L) newSchedule(); else clearForm();
    }
    private void refreshRows() {
        model.setRowCount(0); for (CourseScheduleDto value : schedules) model.addRow(new Object[]{value.getId(), day(value.getWeekday()), period(value), dates(value), classroom(value.getClassroom())});
    }
    private void select(int row) {
        if (row < 0 || row >= schedules.size()) return; CourseScheduleDto value = schedules.get(row); selectedId = value.getId(); scheduleId.setText(String.valueOf(selectedId));
        weekday.setSelectedIndex(Math.max(0, Math.min(WEEKDAYS.length - 1, value.getWeekday() - 1))); startPeriod.setValue(Integer.valueOf(value.getStartPeriod())); endPeriod.setValue(Integer.valueOf(value.getEndPeriod()));
        startDate.setText(RealUi.input(value.getStartDate())); endDate.setText(RealUi.input(value.getEndDate())); ClassroomDto room = value.getClassroom(); classroom.setText(room == null ? "" : String.valueOf(room.getId())); clearError(); updateButtons();
    }
    private void newSchedule() {
        if (courseId <= 0L) { setError("请先选择一门课程。"); return; } selectedId = 0L; scheduleId.setText("新建"); weekday.setSelectedIndex(0); startPeriod.setValue(Integer.valueOf(1)); endPeriod.setValue(Integer.valueOf(2));
        startDate.setText(""); endDate.setText(""); classroom.setText(""); clearError(); updateButtons(); table.clearSelection();
    }
    private void save() {
        try {
            if (courseId <= 0L) throw new IllegalArgumentException("请先选择一门课程"); LocalDate from = date(startDate.getText()); LocalDate to = date(endDate.getText());
            if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException("结束日期不能早于起始日期"); Long room = roomId();
            int start = ((Number) startPeriod.getValue()).intValue(), end = ((Number) endPeriod.getValue()).intValue();
            if (end < start) throw new IllegalArgumentException("结束节次不能早于开始节次");
            final boolean updating = selectedId > 0L;
            final ScheduleSaveRequest request = updating ? ScheduleSaveRequest.update(selectedId, courseId, weekday.getSelectedIndex() + 1, start, end, from, to, room) : ScheduleSaveRequest.create(courseId, weekday.getSelectedIndex() + 1, start, end, from, to, room);
            setBusy(true); AsyncTask.run(new AsyncTask.Work<CourseScheduleDto>() {
                @Override public CourseScheduleDto run() throws Exception { return updating ? service.updateSchedule(request) : service.createSchedule(request); }
            }, new AsyncTask.Callback<CourseScheduleDto>() {
                @Override public void onSuccess(CourseScheduleDto value) { replace(value); if (changed != null) changed.run(); page.showSuccess("课程时段已保存。"); setBusy(false); }
                @Override public void onFailure(Throwable cause) { setError(AsyncTask.message(cause)); setBusy(false); }
            });
        } catch (IllegalArgumentException ex) { setError(ex.getMessage()); }
    }
    private void delete() {
        if (selectedId <= 0L) { setError("请先选择要删除的已有时段。"); return; }
        if (!RealUi.confirm(this, "确认删除时段 " + selectedId + "？此操作不可恢复。")) return; final long deleting = selectedId; setBusy(true);
        AsyncTask.run(new AsyncTask.Work<Boolean>() {
            @Override public Boolean run() throws Exception { service.deleteSchedule(deleting); return Boolean.TRUE; }
        }, new AsyncTask.Callback<Boolean>() {
            @Override public void onSuccess(Boolean value) { remove(deleting); if (changed != null) changed.run(); page.showSuccess("课程时段已删除。"); setBusy(false); }
            @Override public void onFailure(Throwable cause) { setError(AsyncTask.message(cause)); setBusy(false); }
        });
    }
    private void replace(CourseScheduleDto value) {
        List<CourseScheduleDto> copy = new ArrayList<CourseScheduleDto>(schedules);
        for (int i = 0; i < copy.size(); i++) if (copy.get(i).getId() == value.getId()) { copy.set(i, value); schedules = copy; refreshRows(); select(i); return; }
        copy.add(value); schedules = copy; refreshRows(); select(copy.size() - 1);
    }
    private void remove(long id) { List<CourseScheduleDto> copy = new ArrayList<CourseScheduleDto>(); for (CourseScheduleDto value : schedules) if (value.getId() != id) copy.add(value); schedules = copy; refreshRows(); newSchedule(); }
    private void setEditorEnabled(boolean enabled) {
        table.setEnabled(enabled); scheduleId.setEnabled(enabled); weekday.setEnabled(enabled); startPeriod.setEnabled(enabled); endPeriod.setEnabled(enabled); startDate.setEnabled(enabled); endDate.setEnabled(enabled); classroom.setEnabled(enabled); updateButtons();
    }
    private void setBusy(boolean value) { busy = value; updateButtons(); }
    private void updateButtons() { boolean enabled = courseId > 0L && !busy; newButton.setEnabled(enabled); saveButton.setEnabled(enabled); deleteButton.setEnabled(enabled && selectedId > 0L); }
    private void clearForm() { selectedId = 0L; scheduleId.setText("--"); weekday.setSelectedIndex(0); startPeriod.setValue(Integer.valueOf(1)); endPeriod.setValue(Integer.valueOf(2)); startDate.setText(""); endDate.setText(""); classroom.setText(""); clearError(); updateButtons(); }
    private void setError(String text) { error.setForeground(DesignTokens.ERROR); error.setText(text == null ? "请求失败，请稍后重试。" : text); }
    private void clearError() { error.setForeground(DesignTokens.TEXT_SECONDARY); error.setText(" "); }
    private static void add(JPanel panel, String label, java.awt.Component field) { panel.add(UiFactory.labelledField(label, field)); }
    private static JSpinner spinner() { JSpinner value = new JSpinner(new SpinnerNumberModel(1, 1, 255, 1)); value.setFont(DesignTokens.regular(14)); return value; }
    private static DefaultTableModel model() { return new DefaultTableModel(new String[]{"时段编号", "星期", "节次", "起止日期", "教室"}, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } }; }
    private static String day(int value) { return value >= 1 && value <= 7 ? WEEKDAYS[value - 1] : "星期" + value; }
    private static String period(CourseScheduleDto value) { return value.getStartPeriod() + "—" + value.getEndPeriod() + "节"; }
    private static String dates(CourseScheduleDto value) { return RealUi.date(value.getStartDate()) + " 至 " + RealUi.date(value.getEndDate()); }
    private static String classroom(ClassroomDto value) { return value == null ? "--" : "ID " + value.getId() + " " + RealUi.text(value.getBuildingName()) + "-" + RealUi.text(value.getRoomNo()); }
    private static LocalDate date(String value) { String text = RealUi.optional(value); if (text == null) return null; try { return LocalDate.parse(text); } catch (DateTimeParseException ex) { throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd"); } }
    private static Long roomId(String value) { String text = RealUi.optional(value); if (text == null) return null; Long id = RealUi.number(text); if (id == null || id.longValue() <= 0L) throw new IllegalArgumentException("教室 ID 必须是正整数"); return id; }
    private Long roomId() { return roomId(classroom.getText()); }
}
