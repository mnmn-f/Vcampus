package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.SchedulingTeacherDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MySQL snapshot and atomic-write adapter for the scheduling solver. */
public final class MySqlSchedulingRepository implements SchedulingRepository {
    @Override public AutoSchedulingSolver.Problem loadProblem(Connection c) throws Exception {
        Map<Long, CourseSeed> courses = courses(c);
        loadTeachers(c, courses); loadGroups(c, courses);
        List<AutoSchedulingSolver.Room> rooms = rooms(c);
        List<AutoSchedulingSolver.Fixed> fixed = fixed(c, courses);
        List<AutoSchedulingSolver.Preference> preferences = preferenceModels(c);
        List<AutoSchedulingSolver.Session> sessions = new ArrayList<AutoSchedulingSolver.Session>();
        for (CourseSeed course : courses.values()) {
            int desired = Math.max(1, Math.min(3, (course.totalHours + 31) / 32));
            int remaining = Math.max(0, desired - course.fixedCount);
            String requiredType = requiredRoomType(course.description);
            String teacherNames = join(course.teacherNames);
            for (int i = 0; i < remaining; i++) sessions.add(new AutoSchedulingSolver.Session(
                    course.code + "-" + (course.fixedCount + i + 1), course.id, course.code,
                    course.name, course.teacherIds, teacherNames, course.groups,
                    course.capacity, requiredType));
        }
        return new AutoSchedulingSolver.Problem(sessions, rooms, fixed, preferences, slots());
    }

    @Override public SchedulingOverviewDto overview(Connection c) throws Exception {
        List<SchedulingTeacherDto> teachers = new ArrayList<SchedulingTeacherDto>();
        String sql = "SELECT tp.user_id,u.display_name,tp.employee_no FROM teacher_profiles tp "
                + "JOIN users u ON u.id=tp.user_id WHERE tp.status='ACTIVE' ORDER BY u.display_name";
        try (PreparedStatement statement = c.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) teachers.add(new SchedulingTeacherDto(rs.getLong(1), rs.getString(2), rs.getString(3)));
        }
        return new SchedulingOverviewDto(teachers, preferences(c));
    }

    @Override public TeacherTimePreferenceDto savePreference(Connection c,
            TeacherTimePreferenceDto value) throws Exception {
        String sql = "INSERT INTO teacher_time_preferences(teacher_user_id,weekday,start_period,end_period,preference_type) "
                + "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE preference_type=VALUES(preference_type),id=LAST_INSERT_ID(id)";
        long id;
        try (PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, value.getTeacherUserId()); statement.setInt(2, value.getWeekday());
            statement.setInt(3, value.getStartPeriod()); statement.setInt(4, value.getEndPeriod());
            statement.setString(5, value.getPreferenceType()); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new IllegalStateException("preference id unavailable"); id = keys.getLong(1); }
        }
        return new TeacherTimePreferenceDto(id, value.getTeacherUserId(), value.getWeekday(),
                value.getStartPeriod(), value.getEndPeriod(), value.getPreferenceType());
    }

    @Override public boolean deletePreference(Connection c, long id) throws Exception {
        try (PreparedStatement statement = c.prepareStatement("DELETE FROM teacher_time_preferences WHERE id=?")) {
            statement.setLong(1, id); return statement.executeUpdate() == 1;
        }
    }

    @Override public void lockSchedules(Connection c) throws Exception {
        try (PreparedStatement statement = c.prepareStatement("SELECT id FROM course_schedules FOR UPDATE"); ResultSet ignored = statement.executeQuery()) {
            while (ignored.next()) { /* lock complete schedule snapshot */ }
        }
    }

    @Override public int savePlan(Connection c, List<AutoScheduleEntryDto> entries) throws Exception {
        String sql = "INSERT INTO course_schedules(course_id,weekday,start_period,end_period,classroom_id) VALUES(?,?,?,?,?)";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            for (AutoScheduleEntryDto value : entries) {
                statement.setLong(1, value.getCourseId()); statement.setInt(2, value.getWeekday());
                statement.setInt(3, value.getStartPeriod()); statement.setInt(4, value.getEndPeriod());
                statement.setLong(5, value.getClassroomId()); statement.addBatch();
            }
            int count = 0; for (int value : statement.executeBatch()) if (value != Statement.EXECUTE_FAILED) count++;
            return count;
        }
    }

    private static Map<Long, CourseSeed> courses(Connection c) throws Exception {
        Map<Long, CourseSeed> result = new LinkedHashMap<Long, CourseSeed>();
        String sql = "SELECT id,course_code,course_name,capacity,COALESCE(total_hours,32),description "
                + "FROM courses WHERE status IN ('DRAFT','PUBLISHED') ORDER BY id";
        try (PreparedStatement statement = c.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) { CourseSeed seed = new CourseSeed(); seed.id=rs.getLong(1); seed.code=rs.getString(2);
                seed.name=rs.getString(3); seed.capacity=rs.getInt(4); seed.totalHours=rs.getInt(5);
                seed.description=rs.getString(6); result.put(Long.valueOf(seed.id), seed); }
        }
        return result;
    }

    private static void loadTeachers(Connection c, Map<Long, CourseSeed> courses) throws Exception {
        String sql = "SELECT ci.course_id,ci.teacher_user_id,u.display_name FROM course_instructors ci "
                + "JOIN users u ON u.id=ci.teacher_user_id ORDER BY ci.course_id,ci.instructor_role";
        try (PreparedStatement statement = c.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) { CourseSeed seed=courses.get(Long.valueOf(rs.getLong(1))); if(seed!=null){seed.teacherIds.add(Long.valueOf(rs.getLong(2)));seed.teacherNames.add(rs.getString(3));} }
        }
    }

    private static void loadGroups(Connection c, Map<Long, CourseSeed> courses) throws Exception {
        String sql = "SELECT DISTINCT e.course_id,sp.class_name,e.student_user_id "
                + "FROM enrollments e JOIN student_profiles sp ON sp.user_id=e.student_user_id "
                + "WHERE e.status='ENROLLED'";
        try (PreparedStatement statement = c.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                CourseSeed seed=courses.get(Long.valueOf(rs.getLong(1)));
                if(seed!=null){if(rs.getString(2)!=null)seed.groups.add("CLASS:"+rs.getString(2));
                    seed.groups.add("STUDENT:"+rs.getLong(3));}
            }
        }
        for (CourseSeed seed : courses.values()) if (seed.groups.isEmpty()) seed.groups.add("COURSE:" + seed.id);
    }

    private static List<AutoSchedulingSolver.Room> rooms(Connection c) throws Exception {
        List<AutoSchedulingSolver.Room> result = new ArrayList<AutoSchedulingSolver.Room>();
        String sql="SELECT id,building_name,room_no,classroom_type,capacity FROM classrooms WHERE status='AVAILABLE' ORDER BY capacity,id";
        try(PreparedStatement statement=c.prepareStatement(sql);ResultSet rs=statement.executeQuery()){
            while(rs.next())result.add(new AutoSchedulingSolver.Room(rs.getLong(1),rs.getString(2)+" "+rs.getString(3),rs.getString(4),rs.getInt(5)));
        } return result;
    }

    private static List<AutoSchedulingSolver.Fixed> fixed(Connection c, Map<Long, CourseSeed> courses) throws Exception {
        List<AutoSchedulingSolver.Fixed> result=new ArrayList<AutoSchedulingSolver.Fixed>();
        String sql="SELECT course_id,COALESCE(classroom_id,0),weekday,start_period,end_period FROM course_schedules";
        try(PreparedStatement statement=c.prepareStatement(sql);ResultSet rs=statement.executeQuery()){
            while(rs.next()){CourseSeed seed=courses.get(Long.valueOf(rs.getLong(1)));if(seed==null)continue;seed.fixedCount++;
                result.add(new AutoSchedulingSolver.Fixed(seed.teacherIds,seed.groups,rs.getLong(2),new AutoSchedulingSolver.TimeSlot(rs.getInt(3),rs.getInt(4),rs.getInt(5))));}
        } return result;
    }

    private static List<TeacherTimePreferenceDto> preferences(Connection c) throws Exception {
        List<TeacherTimePreferenceDto> result=new ArrayList<TeacherTimePreferenceDto>();
        try(PreparedStatement statement=c.prepareStatement("SELECT id,teacher_user_id,weekday,start_period,end_period,preference_type FROM teacher_time_preferences ORDER BY teacher_user_id,weekday,start_period");ResultSet rs=statement.executeQuery()){
            while(rs.next())result.add(new TeacherTimePreferenceDto(rs.getLong(1),rs.getLong(2),rs.getInt(3),rs.getInt(4),rs.getInt(5),rs.getString(6)));
        } return result;
    }
    private static List<AutoSchedulingSolver.Preference> preferenceModels(Connection c) throws Exception {
        List<AutoSchedulingSolver.Preference> result=new ArrayList<AutoSchedulingSolver.Preference>();
        for(TeacherTimePreferenceDto value:preferences(c))result.add(new AutoSchedulingSolver.Preference(value.getTeacherUserId(),new AutoSchedulingSolver.TimeSlot(value.getWeekday(),value.getStartPeriod(),value.getEndPeriod()),TimePreferenceType.valueOf(value.getPreferenceType())));
        return result;
    }
    private static List<AutoSchedulingSolver.TimeSlot> slots(){List<AutoSchedulingSolver.TimeSlot> r=new ArrayList<AutoSchedulingSolver.TimeSlot>();int[][] blocks={{1,2},{3,4},{5,6},{7,8},{9,10}};for(int d=1;d<=5;d++)for(int[]b:blocks)r.add(new AutoSchedulingSolver.TimeSlot(d,b[0],b[1]));return r;}
    private static String requiredRoomType(String description){if(description==null)return null;if(description.contains("【机房】")||description.contains("实验室"))return "LAB";if(description.contains("【会议室】"))return "MEETING";return null;}
    private static String join(List<String> values){StringBuilder r=new StringBuilder();for(String value:values){if(r.length()>0)r.append("、");r.append(value);}return r.toString();}
    private static final class CourseSeed{long id;String code,name,description;int capacity,totalHours,fixedCount;final List<Long>teacherIds=new ArrayList<Long>();final List<String>teacherNames=new ArrayList<String>();final List<String>groups=new ArrayList<String>();}
}
