package edu.seu.vcampus.server.academic.repository.mysql;

/** 教务表查询片段集中定义，避免各 DAO 重复拼接字段。 */
final class MySqlCourseSql {
    static final String COURSE_COLUMNS =
            "c.id,c.course_code,c.course_name,c.course_type,c.credits,c.total_hours,"
                    + "c.capacity,c.description,c.status,"
                    + "(SELECT COUNT(*) FROM enrollments e WHERE e.course_id=c.id "
                    + "AND e.status='ENROLLED') AS enrolled_count";
    static final String COURSE_FROM = " FROM courses c ";
    static final String SCHEDULE_SQL =
            "SELECT s.id,s.course_id,s.weekday,s.start_period,s.end_period,"
                    + "s.start_date,s.end_date,r.id AS room_id,r.building_name,r.room_no,"
                    + "r.classroom_type,r.capacity AS room_capacity,r.status AS room_status "
                    + "FROM course_schedules s LEFT JOIN classrooms r ON r.id=s.classroom_id "
                    + "WHERE s.course_id=? ORDER BY s.weekday,s.start_period,s.id";
    static final String INSTRUCTOR_SQL =
            "SELECT ci.teacher_user_id,u.display_name,ci.instructor_role "
                    + "FROM course_instructors ci JOIN users u ON u.id=ci.teacher_user_id "
                    + "WHERE ci.course_id=? ORDER BY ci.instructor_role,ci.teacher_user_id";

    private MySqlCourseSql() {
    }
}
