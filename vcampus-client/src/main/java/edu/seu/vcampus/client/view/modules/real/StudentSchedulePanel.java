package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;

import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/** 学生课表的异步实时查询卡片。 */
public final class StudentSchedulePanel extends SectionCard {
    private final BasePage page;
    private final AcademicClientService service;
    private final JTextArea content = UiFactory.textArea(6, 60);

    public StudentSchedulePanel(BasePage page, AcademicClientService service) {
        super("我的课表", "已选课程及上课时段。");
        this.page = page; this.service = service; content.setEditable(false);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setOpaque(false); wrapper.add(new JScrollPane(content), BorderLayout.CENTER); setContent(wrapper);
        content.setText("正在加载课表…"); load();
    }

    private void load() {
        AsyncTask.run(new AsyncTask.Work<StudentScheduleDto>() {
            @Override public StudentScheduleDto run() throws Exception { return service.studentSchedule(); }
        }, new AsyncTask.Callback<StudentScheduleDto>() {
            @Override public void onSuccess(StudentScheduleDto value) {
                if (value.getCourses().isEmpty()) { content.setText("暂无已选课程"); page.showInfo("当前没有已选课程。"); return; }
                StringBuilder text = new StringBuilder();
                for (CourseDto course : value.getCourses()) text.append(course.getCourseCode()).append("　")
                        .append(course.getCourseName()).append("　时段 ").append(course.getSchedules().size()).append(" 条\n");
                content.setText(text.toString()); page.showSuccess("课表已加载。");
            }
            @Override public void onFailure(Throwable error) { content.setText("课表加载失败：" + AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
        });
    }
}
