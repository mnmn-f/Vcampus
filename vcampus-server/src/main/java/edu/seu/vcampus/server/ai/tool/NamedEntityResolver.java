package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.service.AiServiceException;
import edu.seu.vcampus.server.security.SessionContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 将“报名数学建模”等名称参数解析为实时数据库对象编号。 */
public final class NamedEntityResolver {
    private final AiToolRegistry tools;
    private final ToolBridge bridge;

    public NamedEntityResolver(AiToolRegistry tools, ToolBridge bridge) {
        this.tools = tools; this.bridge = bridge;
    }

    public AiToolInvocation resolve(AiToolInvocation invocation, SessionContext session) {
        if (invocation == null) return null;
        if ("dorm.repair.create".equals(invocation.getToolName())
                && !hasNumber(invocation.getArgumentsJson(), "roomId")) {
            Object accommodation = bridge.execute(tools.get("dorm.accommodation.read"), "{}", session);
            Long roomId = findNumber(accommodation, "getRoomId", 0);
            if (roomId != null) return new AiToolInvocation(invocation.getToolName(),
                    withNumber(invocation.getArgumentsJson(), "roomId", roomId.longValue()),
                    invocation.getSummary() + "（本人当前宿舍）");
        }
        if ("library.study-room.reserve".equals(invocation.getToolName())
                && !hasNumber(invocation.getArgumentsJson(), "roomId")
                && string(invocation.getArgumentsJson(), "name").isEmpty()) {
            AiToolInvocation resolved = uniqueOpenStudyRoom(invocation, session);
            if (resolved != null) return resolved;
        }
        String name = string(invocation.getArgumentsJson(), "name");
        SearchSpec spec = spec(invocation.getToolName());
        if (spec == null) return invocation;
        Long chosen = number(invocation.getArgumentsJson(), spec.idKey);
        if (chosen != null) {
            for (Object raw : ToolDataViews.readAll(tools.get(spec.searchTool), "{}", bridge, session)) {
                Object item = selectable(raw, spec);
                if (chosen.equals(entityId(item))) {
                    String display = getterText(item, spec.primary);
                    String detail = getterText(item, spec.secondary);
                    return new AiToolInvocation(invocation.getToolName(), invocation.getArgumentsJson(),
                            invocation.getSummary() + "（" + (display == null ? spec.label : display)
                                    + (detail == null ? "" : " / " + detail) + "，编号 " + chosen + "）");
                }
            }
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "所选对象不存在或不在当前可操作记录中，请重新查询并选择。");
        }
        if (name.isEmpty()) return invocation;
        List<Object> items = ToolDataViews.readAll(tools.get(spec.searchTool),
                "{\"keyword\":\"" + escape(name) + "\"}", bridge, session);
        Match best = best(items, name, spec);
        // Ambiguity is a selection, never permission to pick the first fuzzy result.
        if (best == null) return invocation;
        String json = withNumber(invocation.getArgumentsJson(), spec.idKey, best.id);
        return new AiToolInvocation(invocation.getToolName(), json,
                invocation.getSummary() + "（编号 " + best.id + "，" + best.name + "）");
    }

    /** Candidate IDs come from the same permission-checked business commands as the UI. */
    public String choices(AiToolInvocation invocation, SessionContext session) {
        SearchSpec spec = spec(invocation.getToolName());
        if (spec == null) return "";
        List<Object> items = ToolDataViews.readAll(tools.get(spec.searchTool), "{}", bridge, session);
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Object raw : items) {
            Object item = selectable(raw, spec);
            if (item == null) continue;
            Long id = entityId(item);
            if (id == null) continue;
            String primary = getterText(item, spec.primary);
            String secondary = getterText(item, spec.secondary);
            String display = (primary == null ? spec.label : primary)
                    + (secondary == null ? "" : " / " + secondary);
            for (String getter : new String[] {"getStartAt", "getStatus", "getTotalAmount"}) {
                String detail = getterText(item, getter);
                if (detail != null) display += " · " + detail;
            }
            out.append("\n候选项：").append(spec.idKey).append('\t').append(id)
                    .append('\t').append(display.replaceAll("[\\t\\r\\n]", " "))
                    .append("（编号 ").append(id).append("）");
            if (++count >= 100) break;
        }
        return out.toString();
    }

    private Object selectable(Object item, SearchSpec spec) {
        if ("academic.enrollments.read".equals(spec.searchTool)) {
            if (!"ENROLLED".equals(getterText(invoke(item, "getEnrollment"), "getStatus"))) return null;
            return invoke(item, "getCourse");
        }
        String status = getterText(item, "getStatus");
        if ("RETURNED".equals(status) || "CANCELLED".equals(status) || "CANCELED".equals(status)
                || "REJECTED".equals(status) || "COMPLETED".equals(status)) return null;
        if ("dorm.leave.mine".equals(spec.searchTool) && !"PENDING".equals(status)) return null;
        if (("store.orders.mine".equals(spec.searchTool) || "dorm.utility.read".equals(spec.searchTool))
                && ("PAID".equals(status) || "CLOSED".equals(status))) return null;
        return item;
    }

    private AiToolInvocation uniqueOpenStudyRoom(AiToolInvocation invocation,
            SessionContext session) {
        Object result = bridge.execute(tools.get("library.study-room.search"), "{}", session);
        List<Object> items = new ArrayList<Object>();
        collectItems(result, items, 0);
        Object only = null;
        for (Object item : items) {
            String status = getterText(item, "getStatus");
            if (status != null && !"OPEN".equalsIgnoreCase(status)) continue;
            if (only != null) return null;
            only = item;
        }
        Long id = getterNumber(only, "getId");
        if (id == null) return null;
        String building = getterText(only, "getBuildingName");
        String room = getterText(only, "getRoomNo");
        String display = ((building == null ? "" : building + " ")
                + (room == null ? "" : room)).trim();
        return new AiToolInvocation(invocation.getToolName(),
                withNumber(invocation.getArgumentsJson(), "roomId", id.longValue()),
                invocation.getSummary() + "（自动匹配唯一开放自习室："
                        + (display.isEmpty() ? "编号 " + id : display) + "）");
    }

    private SearchSpec spec(String tool) {
        if ("academic.course.drop".equals(tool))
            return new SearchSpec("academic.enrollments.read", "已选课程", "getCourseName", "getCourseCode");
        if ("academic.course.enroll".equals(tool))
            return new SearchSpec("academic.course.search", "课程", "getCourseName", "getCourseCode");
        if ("library.book.borrow".equals(tool))
            return new SearchSpec("library.book.search", "图书", "getTitle", "getIsbn");
        if ("library.book.return".equals(tool))
            return new SearchSpec("library.borrow.mine", "借阅记录", "getBookTitle", null);
        if ("campus.competition.cancel".equals(tool))
            return new SearchSpec("campus.competition.mine", "已报名竞赛", "getTitle", null);
        if ("campus.competition.register".equals(tool))
            return new SearchSpec("campus.competition.search", "竞赛", "getTitle", null);
        if ("store.cart.update".equals(tool) || "store.cart.remove".equals(tool))
            return new SearchSpec("store.cart.read", "购物车商品", "getProductName", "getSku");
        if ("store.cart.add".equals(tool))
            return new SearchSpec("store.product.search", "商品", "getName", null);
        if ("library.study-room.reserve".equals(tool))
            return new SearchSpec("library.study-room.search", "自习室", "getBuildingName", "getRoomNo", "roomId");
        if ("campus.classroom.apply".equals(tool))
            return new SearchSpec("campus.classroom.search", "教室", "getBuildingName", "getRoomNo", "classroomId");
        if ("campus.classroom.cancel".equals(tool))
            return new SearchSpec("campus.classroom.mine", "教室申请", "getBuildingName", "getRoomNo");
        if ("library.study-room.cancel".equals(tool))
            return new SearchSpec("library.study-room.mine", "自习室预约", "getBuildingName", "getRoomNo");
        if ("dorm.leave.cancel".equals(tool))
            return new SearchSpec("dorm.leave.mine", "请假记录", "getReason", "getLeaveType");
        if ("store.order.pay".equals(tool))
            return new SearchSpec("store.orders.mine", "订单", "getOrderNo", "getStatus", "orderId");
        if ("dorm.utility.pay".equals(tool))
            return new SearchSpec("dorm.utility.read", "水电分摊", "getPeriod", "getStatus", "allocationId");
        return null;
    }

    private Match best(List<Object> items, String query, SearchSpec spec) {
        Match best = null; boolean tied = false; String q = normalize(query);
        for (Object raw : items) {
            Object item = selectable(raw, spec);
            if (item == null) continue;
            if ("library.borrow.mine".equals(spec.searchTool)
                    && "RETURNED".equalsIgnoreCase(getterText(item, "getStatus"))) continue;
            Long id = entityId(item);
            String primary = getterText(item, spec.primary);
            String secondary = getterText(item, spec.secondary);
            if (id == null || primary == null) continue;
            int score = score(q, normalize(primary));
            if (secondary != null) score = Math.max(score, score(q, normalize(secondary)));
            if (score > 0 && (best == null || score > best.score)) {
                best = new Match(id.longValue(), primary, score); tied = false;
            } else if (best != null && score == best.score) tied = true;
        }
        return tied ? null : best;
    }

    private int score(String query, String candidate) {
        if (query.equals(candidate)) return 100;
        // A substring is suitable for suggestions, not automatic mutation.
        return 0;
    }

    private void collectItems(Object value, List<Object> out, int depth) {
        if (value == null || depth > 3) return;
        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) out.add(item);
            return;
        }
        for (String getter : new String[] {"getItems", "getCourses", "getRecords"}) {
            Object child = invoke(value, getter);
            if (child != null) { collectItems(child, out, depth + 1); return; }
        }
    }

    private Long getterNumber(Object value, String getter) {
        Object result = invoke(value, getter);
        return result instanceof Number ? Long.valueOf(((Number) result).longValue()) : null;
    }
    private Long entityId(Object value) {
        for (String getter : new String[]{"getId", "getProductId", "getCompetitionId", "getAllocationId"}) {
            Long id = getterNumber(value, getter); if (id != null) return id;
        }
        return null;
    }

    private String getterText(Object value, String getter) {
        Object result = invoke(value, getter);
        return result == null ? null : String.valueOf(result);
    }

    private Object invoke(Object value, String getter) {
        if (value == null || getter == null) return null;
        try { Method method = value.getClass().getMethod(getter); return method.invoke(value); }
        catch (Exception ex) { return null; }
    }

    private Long findNumber(Object value, String getter, int depth) {
        if (value == null || depth > 3) return null;
        Long direct = getterNumber(value, getter);
        if (direct != null) return direct;
        for (String container : new String[] {"getItems", "getRecords", "getAccommodation"}) {
            Object child = invoke(value, container);
            if (child instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) child) {
                    Long found = findNumber(item, getter, depth + 1);
                    if (found != null) return found;
                }
            } else {
                Long found = findNumber(child, getter, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String withNumber(String json, String key, long value) {
        String source = json == null ? "{}" : json.trim();
        if (!source.startsWith("{") || !source.endsWith("}")) source = "{}";
        String prefix = source.substring(0, source.length() - 1).trim();
        return prefix + (prefix.length() > 1 ? "," : "") + "\"" + key + "\":" + value + "}";
    }

    private boolean hasNumber(String json, String key) { return number(json, key) != null; }

    private Long number(String json, String key) {
        int at = valueAt(json, key); if (at < 0) return null; int end = at;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return end == at ? null : Long.valueOf(json.substring(at, end)); }
        catch (NumberFormatException ex) { return null; }
    }

    private String string(String json, String key) {
        int at = valueAt(json, key);
        if (at < 0 || at >= json.length() || json.charAt(at) != '"') return "";
        StringBuilder out = new StringBuilder();
        for (int i = at + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return out.toString();
            if (c == '\\' && i + 1 < json.length()) c = json.charAt(++i);
            out.append(c);
        }
        return "";
    }

    private int valueAt(String json, String key) {
        if (json == null) return -1; int at = json.indexOf('"' + key + '"');
        if (at < 0 || (at = json.indexOf(':', at)) < 0) return -1; at++;
        while (at < json.length() && Character.isWhitespace(json.charAt(at))) at++;
        return at;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final class SearchSpec {
        private final String searchTool; private final String label;
        private final String primary; private final String secondary; private final String idKey;
        private SearchSpec(String searchTool, String label, String primary, String secondary) {
            this(searchTool, label, primary, secondary, "id");
        }
        private SearchSpec(String searchTool, String label, String primary, String secondary,
                           String idKey) {
            this.searchTool = searchTool; this.label = label;
            this.primary = primary; this.secondary = secondary; this.idKey = idKey;
        }
    }
    private static final class Match {
        private final long id; private final String name; private final int score;
        private Match(long id, String name, int score) { this.id = id; this.name = name; this.score = score; }
    }
}
