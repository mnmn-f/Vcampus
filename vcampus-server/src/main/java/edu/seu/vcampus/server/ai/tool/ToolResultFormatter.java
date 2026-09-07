package edu.seu.vcampus.server.ai.tool;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 将已授权的业务 DTO 转成适合对话展示的有界摘要。 */
public final class ToolResultFormatter {
    private static final int MAX_LENGTH = 5000;
    private static final Map<String, String> LABELS = labels();

    public String format(Object value) {
        StringBuilder out = new StringBuilder(); append(out, value, 0);
        if (out.length() > MAX_LENGTH) return out.substring(0, MAX_LENGTH) + "\n（结果已截断）";
        return out.length() == 0 ? "操作已完成。" : out.toString();
    }

    private void append(StringBuilder out, Object value, int depth) {
        if (value == null) { out.append("暂无数据"); return; }
        if (simple(value)) { out.append(String.valueOf(value)); return; }
        if (value instanceof Iterable<?>) {
            Iterator<?> it = ((Iterable<?>) value).iterator(); int index = 1;
            while (it.hasNext() && index <= 10) {
                out.append(index++).append(". "); append(out, it.next(), depth + 1); out.append('\n');
            }
            if (index == 1) out.append("暂无数据"); return;
        }
        if (value.getClass().isArray()) {
            int length = Math.min(10, Array.getLength(value));
            for (int i = 0; i < length; i++) { out.append(i + 1).append(". ");
                append(out, Array.get(value, i), depth + 1); out.append('\n'); }
            return;
        }
        if (depth >= 4) { out.append(value.getClass().getSimpleName()); return; }
        Method[] methods = value.getClass().getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            public int compare(Method left, Method right) {
                return left.getName().compareTo(right.getName());
            }
        });
        int fields = 0;
        for (Method method : methods) {
            String name = method.getName();
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterTypes().length != 0
                    || !name.startsWith("get") || "getClass".equals(name) || sensitive(name)) continue;
            try {
                Object child = method.invoke(value);
                if (child == null) continue;
                if (fields++ > 0) out.append(depth == 0 ? "；" : "，");
                out.append(label(name)).append("："); append(out, child, depth + 1);
                if (fields >= 12 || out.length() > MAX_LENGTH) break;
            } catch (Exception ignored) { }
        }
        if (fields == 0) out.append(value.getClass().getSimpleName());
    }

    private boolean simple(Object value) {
        return value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum<?>;
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
        labels.put("borrowedAt", "借阅时间"); labels.put("dueAt", "应还时间");
        labels.put("returnedAt", "归还时间"); labels.put("balance", "余额");
        labels.put("amount", "金额"); labels.put("quantity", "数量");
        labels.put("createdAt", "创建时间"); labels.put("updatedAt", "更新时间");
        return labels;
    }
}
