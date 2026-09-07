package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.academic.EnrollmentRequest;
import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.library.BookSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.BorrowSearchRequest;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.ProductQuery;

import java.io.Serializable;

/** 当前已接入业务命令的轻量 payload 适配器。 */
final class CampusCommandTool implements AiTool {
    private final String name;
    private final String description;
    private final String command;
    private final boolean write;
    private final Kind kind;

    enum Kind { NONE, BOOK_SEARCH, PRODUCT_SEARCH, BORROW_MINE, DORM_PAGE, CAMPUS_PAGE,
        ID_ENROLL, ID_BORROW, ID_RETURN_BORROW, CAMPUS_ID, COMPETITION_REGISTER, CART_ITEM }

    CampusCommandTool(String name, String description, String command,
                      boolean write, Kind kind) {
        this.name = name; this.description = description; this.command = command;
        this.write = write; this.kind = kind;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTargetCommand() { return command; }
    public boolean isWriteOperation() { return write; }

    public Serializable payload(String json) {
        if (kind == Kind.NONE) return null;
        if (kind == Kind.BORROW_MINE) return new BorrowSearchRequest();
        if (kind == Kind.DORM_PAGE) return new DormPageQuery();
        if (kind == Kind.CAMPUS_PAGE) return new CampusPageQuery();
        if (kind == Kind.BOOK_SEARCH) return new BookSearchRequest(string(json, "keyword"), 1, 10);
        if (kind == Kind.PRODUCT_SEARCH) return new ProductQuery(string(json, "keyword"), 1, 10);
        long id = number(json, "id");
        if (kind == Kind.ID_ENROLL) return new EnrollmentRequest(id);
        if (kind == Kind.ID_BORROW) return new BorrowRequest(id);
        if (kind == Kind.ID_RETURN_BORROW) return new ReturnBorrowRequest(id);
        if (kind == Kind.CAMPUS_ID) return new CampusIdRequest(id);
        if (kind == Kind.COMPETITION_REGISTER) return new CompetitionRegistrationRequest(id);
        if (kind == Kind.CART_ITEM) return new CartItemRequest(id, (int) number(json, "quantity"));
        throw new IllegalArgumentException("unsupported AI tool payload");
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
