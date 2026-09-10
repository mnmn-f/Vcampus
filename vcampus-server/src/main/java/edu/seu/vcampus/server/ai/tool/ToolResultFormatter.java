package edu.seu.vcampus.server.ai.tool;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 将已授权的业务 DTO 转成适合聊天框阅读的有界纯文本摘要。 */
public final class ToolResultFormatter {
    private static final int MAX_LENGTH = 5000;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<String, String> LABELS = labels();

    public String format(Object value) {
        return format(value, null);
    }

    /** 根据问题中的字段词裁剪结果；没有明确字段或要求详情时保持完整结果。 */
    public String format(Object value, String question) {
        Focus focus = Focus.from(question);
        StringBuilder out = new StringBuilder();
        Object items = getter(value, "getItems");
        if (items instanceof Iterable<?>) {
            Object total = first(value, "getTotal", "getTotalElements");
            if (total != null) out.append("共 ").append(total).append(" 条结果\n\n");
            appendList(out, (Iterable<?>) items, 0, focus);
        } else append(out, value, 0, focus);
        if (out.length() > MAX_LENGTH) return out.substring(0, MAX_LENGTH) + "\n结果已截断";
        return out.length() == 0 ? "操作已完成。" : out.toString().trim();
    }

    private void append(StringBuilder out, Object value, int depth, Focus focus) {
        if (value == null) { out.append("暂无数据"); return; }
        String scalar = scalar(value);
        if (scalar != null) { out.append(scalar); return; }
        if (value instanceof Map<?, ?>) {
            appendMap(out, (Map<?, ?>) value, depth, focus); return;
        }
        if (value instanceof Iterable<?>) {
            appendList(out, (Iterable<?>) value, depth, focus); return;
        }
        if (value.getClass().isArray()) {
            int length = Math.min(10, Array.getLength(value));
            for (int i = 0; i < length; i++) {
                if (i > 0) out.append('\n');
                out.append("第 ").append(i + 1).append(" 项\n");
                append(out, Array.get(value, i), depth + 1, focus);
            }
            return;
        }
        if (depth >= 3) { out.append(String.valueOf(value)); return; }
        appendObject(out, value, depth, focus);
    }

    private void appendObject(StringBuilder out, Object value, int depth, Focus focus) {
        Method[] methods = value.getClass().getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method left, Method right) {
                int priority = fieldOrder(left.getName()) - fieldOrder(right.getName());
                return priority == 0 ? left.getName().compareTo(right.getName()) : priority;
            }
        });
        int fields = 0;
        Set<String> seen = new HashSet<String>();
        for (Method method : methods) {
            String name = method.getName();
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterTypes().length != 0
                    || !name.startsWith("get") || "getClass".equals(name) || sensitive(name)
                    || alias(name) || !focus.includes(name)) continue;
            try {
                Object child = method.invoke(value);
                if (child == null) continue;
                String field = label(name);
                if (!seen.add(field)) continue;
                if (fields++ > 0) out.append('\n');
                indent(out, depth);
                out.append(field).append("：");
                if ("状态".equals(field)) out.append(status(child));
                else append(out, child, depth + 1, focus);
                if (fields >= 12 || out.length() > MAX_LENGTH) break;
            } catch (Exception ignored) { }
        }
        if (fields == 0) out.append(String.valueOf(value));
    }

    private void appendMap(StringBuilder out, Map<?, ?> values, int depth, Focus focus) {
        int fields = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (fields++ > 0) out.append('\n');
            indent(out, depth);
            out.append(String.valueOf(entry.getKey())).append("：");
            append(out, entry.getValue(), depth + 1, focus);
            if (fields >= 12 || out.length() > MAX_LENGTH) break;
        }
        if (fields == 0) out.append("暂无数据");
    }

    private void appendList(StringBuilder out, Iterable<?> values, int depth, Focus focus) {
        Iterator<?> it = values.iterator();
        int index = 1;
        while (it.hasNext() && index <= 10) {
            if (index > 1) out.append("\n\n");
            out.append("第 ").append(index++).append(" 条\n");
            append(out, it.next(), depth + 1, focus);
        }
        if (index == 1) out.append("暂无数据");
        else if (it.hasNext()) out.append("\n\n仅展示前 10 条");
    }

    private String scalar(Object value) {
        if (value instanceof LocalDateTime) return DATE_TIME.format((LocalDateTime) value);
        if (value instanceof LocalDate) return DATE.format((LocalDate) value);
        if (value instanceof LocalTime) return TIME.format((LocalTime) value);
        if (value instanceof OffsetDateTime || value instanceof ZonedDateTime) return value.toString();
        if (value instanceof java.time.LocalDateTime) {
            return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .format((java.time.LocalDateTime) value);
        }
        if (value instanceof java.time.LocalDate) return value.toString();
        if (value instanceof java.time.LocalTime) {
            return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .format((java.time.LocalTime) value);
        }
        if (value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date) return value.toString();
        if (value instanceof Boolean) return ((Boolean) value).booleanValue() ? "是" : "否";
        if (value instanceof CharSequence || value instanceof Number
                || value instanceof Enum<?>) return String.valueOf(value);
        return null;
    }

    private String status(Object value) {
        String text = String.valueOf(value);
        if ("OPEN".equalsIgnoreCase(text)) return "开放";
        if ("CLOSED".equalsIgnoreCase(text)) return "关闭";
        if ("RESERVED".equalsIgnoreCase(text)) return "已预约";
        if ("CANCELLED".equalsIgnoreCase(text)) return "已取消";
        if ("PENDING".equalsIgnoreCase(text)) return "待处理";
        if ("PAID".equalsIgnoreCase(text)) return "已支付";
        if ("COMPLETED".equalsIgnoreCase(text)) return "已完成";
        if ("ACTIVE".equalsIgnoreCase(text)) return "有效";
        if ("INACTIVE".equalsIgnoreCase(text)) return "停用";
        return text;
    }

    private void indent(StringBuilder out, int depth) {
        for (int index = 0; index < depth; index++) out.append("  ");
    }

    private static int fieldOrder(String getter) {
        if ("getId".equals(getter)) return 0;
        if (getter.endsWith("Id")) return 1;
        if ("getName".equals(getter) || "getTitle".equals(getter)
                || getter.endsWith("Name") || "getRoomNo".equals(getter)) return 2;
        if (getter.startsWith("getStart") || getter.startsWith("getOpen")) return 3;
        if (getter.startsWith("getEnd") || getter.startsWith("getClose")) return 4;
        if ("getStatus".equals(getter)) return 9;
        return 5;
    }

    private Object first(Object value, String... getters) {
        for (String name : getters) {
            Object result = getter(value, name);
            if (result != null) return result;
        }
        return null;
    }

    private Object getter(Object value, String name) {
        if (value == null) return null;
        try { return value.getClass().getMethod(name).invoke(value); }
        catch (Exception ex) { return null; }
    }

    private boolean alias(String name) {
        return "getCourses".equals(name) || "getCourseId".equals(name)
                || "getCompetitionId".equals(name) || "getStartTime".equals(name)
                || "getEndTime".equals(name) || "getRegistrationCount".equals(name)
                || "getTotalElements".equals(name) || "getPageNumber".equals(name)
                || "getTotalPages".equals(name) || "getUserId".equals(name)
                || "getCoverImage".equals(name);
    }

    private boolean sensitive(String name) {
        String lower = name.toLowerCase();
        return lower.contains("password") || lower.contains("token") || lower.contains("secret");
    }

    private String label(String getter) {
        String text = getter.substring(3);
        String field = text.length() == 0 ? getter
                : Character.toLowerCase(text.charAt(0)) + text.substring(1);
        String translated = LABELS.get(field);
        return translated == null ? field : translated;
    }

    private static Map<String, String> labels() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("items", "结果"); labels.put("total", "总数");
        labels.put("page", "页码"); labels.put("pageSize", "每页条数");
        labels.put("id", "编号"); labels.put("status", "状态");
        labels.put("name", "名称"); labels.put("title", "标题");
        labels.put("courseName", "课程"); labels.put("courseCode", "课程编号");
        labels.put("teacherName", "教师"); labels.put("classroom", "教室");
        labels.put("weekday", "星期"); labels.put("startSection", "开始节次");
        labels.put("endSection", "结束节次"); labels.put("score", "成绩");
        labels.put("bookTitle", "书名"); labels.put("author", "作者");
        labels.put("college", "学院"); labels.put("major", "专业");
        labels.put("className", "班级"); labels.put("studentNo", "学号");
        labels.put("displayName", "姓名"); labels.put("account", "账号");
        labels.put("enrollmentYear", "入学年份"); labels.put("expectedGraduationYear", "预计毕业年份");
        labels.put("degreeLevel", "培养层次"); labels.put("gender", "性别");
        labels.put("birthDate", "出生日期"); labels.put("address", "地址");
        labels.put("emergencyContact", "紧急联系人"); labels.put("emergencyPhone", "紧急联系电话");
        labels.put("borrowedAt", "借阅时间"); labels.put("dueAt", "应还时间");
        labels.put("issuedAt", "借阅时间"); labels.put("availableCopies", "可借数量");
        labels.put("totalCopies", "馆藏数量"); labels.put("isbn", "ISBN");
        labels.put("publisher", "出版社"); labels.put("category", "分类");
        labels.put("location", "位置"); labels.put("description", "说明");
        labels.put("returnedAt", "归还时间"); labels.put("balance", "余额");
        labels.put("amount", "金额"); labels.put("quantity", "数量");
        labels.put("capacity", "容量"); labels.put("registeredCount", "已报名人数");
        labels.put("registeredAt", "报名时间");
        labels.put("registrationDeadline", "报名截止时间"); labels.put("startAt", "开始时间");
        labels.put("endAt", "结束时间"); labels.put("credits", "学分");
        labels.put("enrolledCount", "已选人数"); labels.put("courseType", "课程类型");
        labels.put("semesterCode", "学期"); labels.put("productName", "商品");
        labels.put("unitPrice", "单价"); labels.put("totalAmount", "总金额");
        labels.put("orderNo", "订单号"); labels.put("remark", "备注");
        labels.put("createdAt", "创建时间"); labels.put("updatedAt", "更新时间");
        labels.put("buildingName", "教学楼"); labels.put("roomNo", "房间");
        labels.put("roomId", "自习室编号"); labels.put("roomName", "自习室");
        labels.put("openTime", "开放时间"); labels.put("closeTime", "关闭时间");
        return labels;
    }

    private static final class Focus {
        private final Set<String> getters;

        private Focus(Set<String> getters) { this.getters = getters; }

        private boolean includes(String getter) {
            return getters.isEmpty() || structural(getter) || identity(getter)
                    || getters.contains(getter);
        }

        private static Focus from(String question) {
            Set<String> fields = new LinkedHashSet<String>();
            String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
            if (contains(q, "完整", "全部信息", "所有信息", "详细信息", "详情")) {
                return new Focus(fields);
            }
            add(fields, q, new String[] {"成绩", "分数"}, "getScore", "getAverageScore",
                    "getWeightedAverageScore");
            add(fields, q, new String[] {"绩点", "gpa"}, "getGradePoint", "getAverageGpa",
                    "getWeightedGpa");
            add(fields, q, new String[] {"作者", "谁写", "谁著"}, "getAuthor");
            add(fields, q, new String[] {"出版社"}, "getPublisher");
            add(fields, q, new String[] {"isbn", "书号"}, "getIsbn");
            add(fields, q, new String[] {"出版年份", "哪年出版"}, "getPublicationYear");
            add(fields, q, new String[] {"可借", "馆藏数量", "库存", "有几本"},
                    "getAvailableCopies", "getTotalCopies");
            add(fields, q, new String[] {"位置", "在哪", "哪里"}, "getLocation");
            add(fields, q, new String[] {"分类", "类别"}, "getCategory");
            add(fields, q, new String[] {"简介", "介绍", "说明"}, "getDescription");
            add(fields, q, new String[] {"学院"}, "getCollege");
            add(fields, q, new String[] {"专业"}, "getMajor");
            add(fields, q, new String[] {"班级"}, "getClassName");
            add(fields, q, new String[] {"学号"}, "getStudentNo");
            add(fields, q, new String[] {"入学年份", "哪年入学"}, "getEnrollmentYear");
            add(fields, q, new String[] {"毕业年份", "哪年毕业"}, "getExpectedGraduationYear");
            add(fields, q, new String[] {"培养层次", "学历"}, "getDegreeLevel");
            add(fields, q, new String[] {"教师", "老师"}, "getTeacherName");
            add(fields, q, new String[] {"教室"}, "getClassroom", "getBuildingName", "getRoomNo");
            add(fields, q, new String[] {"学分"}, "getCredits");
            add(fields, q, new String[] {"价格", "多少钱", "金额"}, "getUnitPrice",
                    "getAmount", "getTotalAmount");
            add(fields, q, new String[] {"数量"}, "getQuantity", "getAvailableCopies",
                    "getTotalCopies", "getCapacity", "getRegisteredCount");
            add(fields, q, new String[] {"应还", "到期"}, "getDueAt");
            add(fields, q, new String[] {"借阅时间", "什么时候借"}, "getBorrowedAt", "getIssuedAt");
            add(fields, q, new String[] {"备注"}, "getRemark");
            add(fields, q, new String[] {"地址"}, "getAddress");
            add(fields, q, new String[] {"紧急联系人"}, "getEmergencyContact", "getEmergencyPhone");
            add(fields, q, new String[] {"余额"}, "getBalance");
            add(fields, q, new String[] {"状态", "进度"}, "getStatus", "getEnrollmentStatus");
            add(fields, q, new String[] {"时间", "什么时候"}, "getStartAt", "getEndAt",
                    "getStartTime", "getEndTime", "getOpenTime", "getCloseTime", "getDueAt");
            return new Focus(fields);
        }

        private static void add(Set<String> target, String question, String[] keywords,
                                String... getters) {
            if (!contains(question, keywords)) return;
            target.addAll(Arrays.asList(getters));
        }

        private static boolean contains(String value, String... words) {
            for (String word : words) if (value.contains(word)) return true;
            return false;
        }

        private static boolean structural(String getter) {
            return "getItems".equals(getter) || "getGrades".equals(getter)
                    || "getMetrics".equals(getter) || "getEntries".equals(getter);
        }

        private static boolean identity(String getter) {
            return "getTitle".equals(getter) || "getName".equals(getter)
                    || "getBookTitle".equals(getter) || "getCourseName".equals(getter)
                    || "getCourseCode".equals(getter) || "getProductName".equals(getter)
                    || "getRoomName".equals(getter) || "getRoomNo".equals(getter)
                    || "getOrderNo".equals(getter);
        }
    }
}
