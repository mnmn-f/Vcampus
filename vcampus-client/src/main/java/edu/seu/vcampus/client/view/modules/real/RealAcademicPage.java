package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 教务模块的实时网络页面。 */
public final class RealAcademicPage extends BasePage {
    public RealAcademicPage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.ACADEMIC), "");
        Role role = session.getActiveRole(); setHeaderContext(role.getDisplayName());
        TaskTabs tabs = new TaskTabs();
        tabs.addTask(courseTab(role), new AcademicCoursesPanel(this, services.academic(), role));
        if (role == Role.STUDENT) {
            tabs.addTask("我的选课", new StudentEnrollmentsPanel(this, services.academic()));
            tabs.addTask("我的课表", new StudentSchedulePanel(this, services.academic()));
            tabs.addTask("我的成绩", new StudentGradesPanel(services.student()));
        }
        if (role == Role.TEACHER) tabs.addTask("成绩登记",
                new TeacherGradePanel(this, services.academic(), services.student()));
        tabs.addTask("教务公告", new CampusAnnouncementsPanel(this, services.campus(), role));
        if (role == Role.STUDENT || role == Role.ACADEMIC_ADMIN) {
            tabs.addTask("竞赛活动", new CampusCompetitionsPanel(this, services.campus(), role));
            tabs.addTask("实践项目", new CampusSrtpPanel(this, services.campus(), role));
        }
        if (role == Role.STUDENT || role == Role.TEACHER || role == Role.ACADEMIC_ADMIN) {
            tabs.addTask("教室使用", new CampusClassroomsPanel(this, services.campus(), role));
        }
        addBlock(tabs);
    }

    private static String courseTab(Role role) {
        if (role == Role.STUDENT) return "选课中心";
        if (role == Role.TEACHER) return "我的课程";
        return "课程与排课";
    }

}
