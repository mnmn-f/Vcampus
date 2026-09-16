package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.academic.*;
import edu.seu.vcampus.server.security.SessionContext;
import java.util.*;

/** Read-only joins and record projections over authorized business responses. */
public final class ToolDataViews {
    public static Object get(Object value, String name) {
        if (value == null) return null;
        try { return value.getClass().getMethod(name).invoke(value); }
        catch (ReflectiveOperationException ex) { return null; }
    }
    public static String text(Object value, String getter) {
        Object result = get(value, getter); return result == null ? "" : result.toString();
    }
    public static List<Object> items(Object value) {
        List<Object> out = new ArrayList<Object>();
        if (value instanceof Iterable<?>) for (Object item : (Iterable<?>) value) out.add(item);
        else for (String name : new String[]{"getItems", "getCourses", "getRecords", "getEntries"}) {
            Object child = get(value, name);
            if (child instanceof Iterable<?>) { for (Object item : (Iterable<?>) child) out.add(item); break; }
        }
        return out;
    }
    public static List<Object> readAll(AiTool tool, String json, ToolBridge bridge, SessionContext session) {
        List<Object> out = new ArrayList<Object>();
        Set<String> pageSignatures = new HashSet<String>();
        for (int page = 1; page <= 20; page++) {
            String source = json == null ? "{}" : json;
            String args = source.substring(0, source.length() - 1)
                    + (source.length() > 2 ? "," : "") + "\"page\":" + page + "}";
            Object result = bridge.execute(tool, args, session);
            List<Object> batch = items(result);
            String signature = batch.isEmpty() ? "" : text(batch.get(0), "getId") + ":" + text(batch.get(batch.size()-1), "getId") + ":" + batch.size();
            if (page > 1 && !pageSignatures.add(signature)) break;
            pageSignatures.add(signature); out.addAll(batch);
            Object total = get(result, "getTotal");
            Object size = get(result, "getPageSize");
            if (!(total instanceof Number) || !(size instanceof Number) || batch.isEmpty()
                    || out.size() >= ((Number) total).longValue()) return out;
        }
        throw new edu.seu.vcampus.server.ai.service.AiServiceException(
                edu.seu.vcampus.common.protocol.ResultCodes.INVALID_INPUT,
                "结果超过安全读取范围，请补充名称或更具体的查询条件。");
    }
    public static Object filter(String tool, Object value, String question) {
        String q = question == null ? "" : question;
        if ("academic.enrollments.read".equals(tool) && !q.contains("历史") && !q.contains("全部")) {
            List<Object> active = new ArrayList<Object>();
            for (Object item : items(value)) {
                Object enrollment = get(item, "getEnrollment");
                if ("ENROLLED".equals(text(enrollment, "getStatus"))) active.add(item);
            }
            return active;
        }
        if (q.contains("未支付") || q.contains("未付款") || q.contains("待支付") || q.contains("未缴")) {
            List<Object> matching = new ArrayList<Object>();
            for (Object item : items(value)) if (Arrays.asList("UNPAID", "CREATED", "PENDING_PAYMENT", "PARTIAL")
                    .contains(text(item, "getStatus"))) matching.add(item);
            return matching;
        }
        return value;
    }
    public static List<Object> unselected(List<Object> courses, Object enrollments) {
        Set<Long> excluded = new HashSet<Long>();
        for (Object value : items(enrollments)) if (value instanceof StudentEnrollmentDto) {
            StudentEnrollmentDto row = (StudentEnrollmentDto) value;
            if (!"DROPPED".equals(row.getEnrollment().getStatus())) excluded.add(row.getCourse().getId());
        }
        List<Object> result = new ArrayList<Object>();
        for (Object value : courses) if (value instanceof CourseDto) {
            CourseDto course = (CourseDto) value;
            if (!excluded.contains(course.getId()) && "PUBLISHED".equals(course.getStatus())) result.add(course);
        }
        return result;
    }
    public static String schedule(Object value) {
        StringBuilder out = new StringBuilder("【课表】\n课程\t学期\t星期\t节次\t教室\t日期范围\n");
        for (Object item : items(value)) if (item instanceof CourseDto) {
            CourseDto course = (CourseDto) item;
            for (CourseScheduleDto slot : course.getSchedules()) {
                Object room = slot.getClassroom();
                out.append(cell(course.getCourseName())).append('\t').append(cell(course.getSemesterCode()))
                        .append('\t').append(slot.getWeekday()).append('\t')
                        .append(slot.getStartPeriod()).append('-').append(slot.getEndPeriod()).append('\t')
                        .append(cell(text(room, "getBuildingName") + " " + text(room, "getRoomNo"))).append('\t')
                        .append(slot.getStartDate()).append(" 至 ").append(slot.getEndDate()).append('\n');
            }
        }
        return out.toString().endsWith("日期范围\n") ? "【课表】\n当前没有已排定的上课时段。" : out.toString();
    }
    public static String cell(String text) { return text == null ? "" : text.replace('\t', ' ').replace('\n',' ').replace('\r',' '); }
}
