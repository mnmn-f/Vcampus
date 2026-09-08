package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.academic.EnrollmentRequest;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.StoreIdRequest;

import org.threeten.bp.LocalDateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 当前已接入业务命令的轻量 payload 适配器。 */
final class CampusCommandTool implements AiTool {
    private final String name;
    private final String description;
    private final String command;
    private final boolean write;
    private final Kind kind;
    private final String parameterGuide;

    enum Kind { NONE, BOOK_SEARCH, PRODUCT_SEARCH, COURSE_SEARCH, BORROW_MINE, DORM_PAGE, CAMPUS_PAGE,
        ID_ENROLL, ID_BORROW, ID_RETURN_BORROW, CAMPUS_ID, COMPETITION_REGISTER, CART_ITEM,
        STUDY_ROOM_SEARCH, STUDY_ROOM_RESERVE, LIBRARY_ID, STORE_ID, ORDER_PAY, COUPON_CODE,
        REPAIR_CREATE, LEAVE_SUBMIT, LEAVE_CANCEL, UTILITY_PAY, CLASSROOM_APPLY }

    CampusCommandTool(String name, String description, String command,
                      boolean write, Kind kind) {
        this(name, description, command, write, kind, guide(kind));
    }

    CampusCommandTool(String name, String description, String command,
                      boolean write, Kind kind, String parameterGuide) {
        this.name = name; this.description = description; this.command = command;
        this.write = write; this.kind = kind; this.parameterGuide = parameterGuide;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParameterGuide() { return parameterGuide; }
    public String getTargetCommand() { return command; }
    public boolean isWriteOperation() { return write; }

    public Serializable payload(String json) {
        if (kind == Kind.NONE) return null;
        if (kind == Kind.BORROW_MINE) return new BorrowSearchRequest();
        if (kind == Kind.DORM_PAGE) return new DormPageQuery();
        if (kind == Kind.CAMPUS_PAGE) return new CampusPageQuery(1, 20,
                optionalString(json, "keyword"), null);
        if (kind == Kind.BOOK_SEARCH) return new BookSearchRequest(optionalString(json, "keyword"), 1, 10);
        if (kind == Kind.PRODUCT_SEARCH) return new ProductQuery(optionalString(json, "keyword"), 1, 10);
        if (kind == Kind.COURSE_SEARCH) return new CourseQuery(1, 20,
                optionalString(json, "keyword"), null, null);
        if (kind == Kind.STUDY_ROOM_SEARCH) return new StudyRoomSearchRequest(
                optionalString(json, "keyword"), null, null, null, null, 1, 20);
        if (kind == Kind.STUDY_ROOM_RESERVE) return new StudyRoomReservationRequest(
                number(json, "roomId"), time(json, "startAt"), time(json, "endAt"));
        if (kind == Kind.REPAIR_CREATE) return new RepairCreateRequest(number(json, "roomId"),
                string(json, "category"), string(json, "description"),
                defaultText(optionalString(json, "priority"), "NORMAL"));
        if (kind == Kind.LEAVE_SUBMIT) return new LeaveSubmitRequest(string(json, "leaveType"),
                time(json, "startAt"), time(json, "endAt"), string(json, "reason"));
        if (kind == Kind.CLASSROOM_APPLY) return new ClassroomReservationRequest(
                number(json, "classroomId"), string(json, "purpose"),
                time(json, "startAt"), time(json, "endAt"));
        if (kind == Kind.ORDER_PAY) return new PaymentRequest(number(json, "orderId"),
                "ai-" + UUID.randomUUID().toString());
        if (kind == Kind.COUPON_CODE) return new CouponClaimRequest(string(json, "code"));
        if (kind == Kind.UTILITY_PAY) return new UtilityPaymentRequest(number(json, "allocationId"),
                "ai-" + UUID.randomUUID().toString());
        long id = number(json, "id");
        if (kind == Kind.ID_ENROLL) return new EnrollmentRequest(id);
        if (kind == Kind.ID_BORROW) return new BorrowRequest(id);
        if (kind == Kind.ID_RETURN_BORROW) return new ReturnBorrowRequest(id);
        if (kind == Kind.CAMPUS_ID) return new CampusIdRequest(id);
        if (kind == Kind.COMPETITION_REGISTER) return new CompetitionRegistrationRequest(id);
        if (kind == Kind.CART_ITEM) return new CartItemRequest(id,
                (int) optionalNumber(json, "quantity", 1L));
        if (kind == Kind.LIBRARY_ID) return new LibraryIdRequest(id);
        if (kind == Kind.STORE_ID) return new StoreIdRequest(id);
        if (kind == Kind.LEAVE_CANCEL) return new LeaveCancelRequest(id);
        throw new IllegalArgumentException("unsupported AI tool payload");
    }

    public String clarificationFor(String json) {
        if (kind == Kind.ID_ENROLL) return missingId(json, "请告诉我要操作的课程名称或课程编号。");
        if (kind == Kind.ID_BORROW) return missingId(json, "请告诉我想借哪本书（书名或图书编号）。");
        if (kind == Kind.ID_RETURN_BORROW) return missingId(json, "请告诉我归还哪一本书，或提供借阅记录编号。");
        if (kind == Kind.COMPETITION_REGISTER || (kind == Kind.CAMPUS_ID
                && name.contains("competition")))
            return missingId(json, "请告诉我要操作的竞赛名称或编号。");
        if (kind == Kind.CAMPUS_ID) return missingId(json,
                "请告诉我要取消的教室申请编号；也可以先让我查询你的申请。");
        if (kind == Kind.CART_ITEM || kind == Kind.STORE_ID) {
            String missingProduct = missingId(json, "请告诉我要操作的商品名称或编号。");
            if (missingProduct != null) return missingProduct;
            if (kind == Kind.CART_ITEM && name.contains("cart.update")
                    && !hasValue(json, "quantity")) return "请告诉我新的商品数量。";
            return null;
        }
        if (kind == Kind.LIBRARY_ID || kind == Kind.LEAVE_CANCEL)
            return missingId(json, "请提供要取消的记录编号；也可以先让我查询你的记录。");
        if (kind == Kind.STUDY_ROOM_RESERVE) return missing(json,
                new String[] {"roomId", "startAt", "endAt"},
                "预约还需要自习室编号、开始时间和结束时间（格式如 2026-09-06T14:00）。");
        if (kind == Kind.ORDER_PAY) return missing(json, new String[] {"orderId"},
                "请提供要支付的订单编号；也可以先让我查询你的订单。");
        if (kind == Kind.COUPON_CODE) return missing(json, new String[] {"code"},
                "请告诉我要领取的优惠券代码。");
        if (kind == Kind.REPAIR_CREATE) return missing(json,
                new String[] {"roomId", "category", "description"},
                "报修还需要房间编号、故障类别和故障描述；优先级可填 LOW、NORMAL、HIGH 或 URGENT。");
        if (kind == Kind.LEAVE_SUBMIT) return missing(json,
                new String[] {"leaveType", "startAt", "endAt", "reason"},
                "请补充请假类型（PERSONAL/ILLNESS/OFF_CAMPUS/OTHER）、开始时间、结束时间和原因。");
        if (kind == Kind.UTILITY_PAY) return missing(json, new String[] {"allocationId"},
                "请提供待缴水电分摊编号；也可以先让我查询你的水电账单。");
        if (kind == Kind.CLASSROOM_APPLY) return missing(json,
                new String[] {"classroomId", "purpose", "startAt", "endAt"},
                "教室申请还需要教室编号、用途、开始时间和结束时间。");
        return null;
    }

    private String missingId(String json, String message) {
        return hasValue(json, "id") ? null : message;
    }

    private String missing(String json, String[] keys, String message) {
        List<String> missing = new ArrayList<String>();
        for (String key : keys) if (!hasValue(json, key)) missing.add(key);
        if (missing.isEmpty()) return null;
        return message + "\n待补充字段：" + String.join(",", missing);
    }

    private boolean hasValue(String json, String key) {
        if (json == null) return false;
        int at = json.indexOf('"' + key + '"');
        if (at < 0 || (at = json.indexOf(':', at)) < 0) return false;
        at++;
        while (at < json.length() && Character.isWhitespace(json.charAt(at))) at++;
        return at < json.length() && json.charAt(at) != '}'
                && !json.regionMatches(true, at, "null", 0, Math.min(4, json.length() - at))
                && !(json.charAt(at) == '"' && at + 1 < json.length() && json.charAt(at + 1) == '"');
    }

    private long number(String json, String key) {
        int at = valueAt(json, key); int end = at;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == at) throw new IllegalArgumentException("工具参数缺少编号");
        return Long.parseLong(json.substring(at, end));
    }

    private String string(String json, String key) {
        int at = valueAt(json, key);
        if (at >= json.length() || json.charAt(at) != '"') return "";
        StringBuilder out = new StringBuilder();
        for (int i = at + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return out.toString();
            if (c == '\\' && i + 1 < json.length()) c = json.charAt(++i);
            out.append(c);
        }
        return out.toString();
    }

    private String optionalString(String json, String key) {
        if (json == null || json.indexOf('"' + key + '"') < 0) return "";
        return string(json, key);
    }

    private long optionalNumber(String json, String key, long fallback) {
        if (json == null || json.indexOf('"' + key + '"') < 0) return fallback;
        return number(json, key);
    }

    private LocalDateTime time(String json, String key) {
        try { return LocalDateTime.parse(string(json, key)); }
        catch (Exception ex) { throw new IllegalArgumentException(key + " 时间格式应为 yyyy-MM-ddTHH:mm"); }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String guide(Kind kind) {
        if (kind == Kind.NONE || kind == Kind.BORROW_MINE || kind == Kind.DORM_PAGE) return "{}（无参数）";
        if (kind == Kind.BOOK_SEARCH || kind == Kind.PRODUCT_SEARCH || kind == Kind.COURSE_SEARCH
                || kind == Kind.STUDY_ROOM_SEARCH
                || kind == Kind.CAMPUS_PAGE) return "{keyword?: string}，关键词可省略以列出全部";
        if (kind == Kind.CART_ITEM) return "{id?: number, name?: string, quantity?: number=1}";
        if (kind == Kind.STUDY_ROOM_RESERVE) return "{roomId: number, startAt: ISO日期时间, endAt: ISO日期时间}";
        if (kind == Kind.ORDER_PAY) return "{orderId: number}";
        if (kind == Kind.COUPON_CODE) return "{code: string}";
        if (kind == Kind.REPAIR_CREATE) return "{roomId: number, category: string, description: string, priority?: LOW|NORMAL|HIGH|URGENT}";
        if (kind == Kind.LEAVE_SUBMIT) return "{leaveType: PERSONAL|ILLNESS|OFF_CAMPUS|OTHER, startAt: ISO日期时间, endAt: ISO日期时间, reason: string}";
        if (kind == Kind.UTILITY_PAY) return "{allocationId: number}";
        if (kind == Kind.CLASSROOM_APPLY) return "{classroomId: number, purpose: string, startAt: ISO日期时间, endAt: ISO日期时间}";
        return "{id?: number, name?: string}，名称会先匹配实时数据";
    }

    private int valueAt(String json, String key) {
        if (json == null) throw new IllegalArgumentException("工具参数不能为空");
        int at = json.indexOf('"' + key + '"');
        if (at < 0 || (at = json.indexOf(':', at)) < 0) {
            throw new IllegalArgumentException("工具参数缺少 " + key);
        }
        at++;
        while (at < json.length() && Character.isWhitespace(json.charAt(at))) at++;
        return at;
    }
}
