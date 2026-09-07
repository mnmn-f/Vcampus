package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseInstructorDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/** Student timetable rendered as a weekday-by-period grid. */
public final class StudentSchedulePanel extends SectionCard {
    private static final String[] DAYS = {"节次", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final int DEFAULT_PERIODS = 12;
    private final BasePage page;
    private final AcademicClientService service;
    private final JTextField semester = UiFactory.textField(12);
    private final JLabel state = UiFactory.muted("等待查询");
    private final JLabel detail = UiFactory.body("点击课表中的课程查看教师、教室和日期。");
    private final DefaultTableModel model = model();
    private final JTable table = new JTable(model);
    private final Map<String, CourseDto> cells = new HashMap<String, CourseDto>();

    public StudentSchedulePanel(BasePage page, AcademicClientService service) {
        super("我的课表", "按周次布局本人当前已选课程，点击课程查看详细安排。");
        if (page == null || service == null) throw new IllegalArgumentException("schedule dependencies are required");
        this.page = page; this.service = service; setContent(content()); reload();
    }

    public void reload() {
        state.setText("正在加载课表…");
        AsyncTask.run(new AsyncTask.Work<StudentScheduleDto>() {
            @Override public StudentScheduleDto run() throws Exception {
                return service.studentSchedule(new StudentScheduleQuery(semester.getText()));
            }
        }, new AsyncTask.Callback<StudentScheduleDto>() {
            @Override public void onSuccess(StudentScheduleDto value) { render(value); }
            @Override public void onFailure(Throwable error) {
                state.setText("课表加载失败"); page.showError(AsyncTask.message(error));
            }
        });
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel filter = UiFactory.horizontal(8);
        filter.add(UiFactory.body("学期编号（留空为全部）")); filter.add(semester);
        javax.swing.JButton query = new PrimaryButton("查询课表");
        query.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reload(); }
        });
        filter.add(query); filter.add(state); root.add(filter, BorderLayout.NORTH);
        configureTable(); JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(900, 420)); root.add(scroll, BorderLayout.CENTER);
        root.add(detail, BorderLayout.SOUTH);
        semester.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reload(); }
        });
        return root;
    }

    private void configureTable() {
        DormTables.style(table); table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true); table.setRowHeight(42);
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) { if (!e.getValueIsAdjusting()) showSelected(); }
        });
        table.getColumnModel().getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) { if (!e.getValueIsAdjusting()) showSelected(); }
        });
    }

    private void render(StudentScheduleDto value) {
        cells.clear(); int periods = DEFAULT_PERIODS;
        for (CourseDto course : value.getCourses()) for (CourseScheduleDto schedule : course.getSchedules()) {
            periods = Math.max(periods, schedule.getEndPeriod());
        }
        model.setRowCount(0);
        for (int period = 1; period <= periods; period++) {
            Object[] row = new Object[DAYS.length]; row[0] = period + "";
            model.addRow(row);
        }
        for (CourseDto course : value.getCourses()) for (CourseScheduleDto schedule : course.getSchedules()) {
            if (schedule.getWeekday() < 1 || schedule.getWeekday() > 7) continue;
            for (int period = schedule.getStartPeriod(); period <= schedule.getEndPeriod(); period++) {
                int row = period - 1, column = schedule.getWeekday();
                model.setValueAt(cellText(course, schedule), row, column);
                cells.put(key(row, column), course);
            }
        }
        state.setText(value.getCourses().isEmpty() ? "当前学期暂无课程" : "共 " + value.getCourses().size() + " 门课程");
        detail.setText("点击课表中的课程查看教师、教室和日期。");
    }

    private void showSelected() {
        CourseDto course = cells.get(key(table.getSelectedRow(), table.getSelectedColumn()));
        if (course == null) return;
        StringBuilder text = new StringBuilder("<html><b>").append(RealUi.text(course.getCourseName()))
                .append("</b>　").append(RealUi.text(course.getCourseCode())).append("<br>教师：")
                .append(teachers(course));
        for (CourseScheduleDto schedule : course.getSchedules()) text.append("<br>")
                .append(DAYS[schedule.getWeekday()]).append(" ").append(schedule.getStartPeriod())
                .append("-").append(schedule.getEndPeriod()).append(" 节　")
                .append(room(schedule.getClassroom())).append("　")
                .append(RealUi.date(schedule.getStartDate())).append(" 至 ")
                .append(RealUi.date(schedule.getEndDate()));
        detail.setText(text.append("</html>").toString());
    }

    private static String cellText(CourseDto course, CourseScheduleDto schedule) {
        return RealUi.text(course.getCourseName()) + " / " + room(schedule.getClassroom());
    }
    private static String teachers(CourseDto course) {
        StringBuilder value = new StringBuilder();
        for (CourseInstructorDto teacher : course.getInstructors()) {
            if (value.length() > 0) value.append("、"); value.append(RealUi.text(teacher.getDisplayName()));
        }
        return value.length() == 0 ? "待定" : value.toString();
    }
    private static String room(ClassroomDto room) { return room == null ? "教室待定" : RealUi.text(room.getBuildingName()) + " " + RealUi.text(room.getRoomNo()); }
    private static String key(int row, int column) { return row + ":" + column; }
    private static DefaultTableModel model() { return new DefaultTableModel(DAYS, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } }; }
}
