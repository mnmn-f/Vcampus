package edu.seu.vcampus.server.ai.tool;

import java.util.Locale;

/** 无模型或明确校园指令时使用的确定性意图解析器。 */
public final class ToolIntentParser {
    public AiToolInvocation parse(String text) {
        String q = text == null ? "" : text.trim();
        if (instructionQuestion(q)) return null;
        Long id = firstNumber(q);
        if (contains(q, "退课", "退选") && id != null) {
            return id("academic.course.drop", id.longValue(), "退选课程 " + id);
        }
        if (contains(q, "选课", "选择课程") && id != null) {
            return id("academic.course.enroll", id.longValue(), "选修课程 " + id);
        }
        if (contains(q, "还书", "归还图书") && id != null) {
            return id("library.book.return", id.longValue(), "归还借阅记录 " + id);
        }
        if (contains(q, "借书", "借阅图书") && id != null) {
            return id("library.book.borrow", id.longValue(), "借阅图书 " + id);
        }
        if (contains(q, "取消比赛报名", "取消竞赛报名", "取消报名") && id != null) {
            return id("campus.competition.cancel", id.longValue(), "取消竞赛报名 " + id);
        }
        if (contains(q, "报名比赛", "报名竞赛", "竞赛报名") && id != null) {
            return id("campus.competition.register", id.longValue(), "报名竞赛 " + id);
        }
        if (contains(q, "加入购物车", "添加到购物车") && id != null) {
            Long quantity = secondNumber(q);
            return cart(id.longValue(), quantity == null ? 1 : quantity.intValue());
        }
        if (contains(q, "提交订单", "创建订单", "生成订单")) {
            return none("store.order.create", "从购物车创建订单");
        }
        if (contains(q, "课表", "课程表")) return none("academic.schedule.read", "查询本人课表");
        if (contains(q, "学籍", "学籍信息")) return none("student.profile.read", "查询本人学籍资料");
        if (contains(q, "成绩", "分数")) return none("student.grades.read", "查询本人成绩");
        if (contains(q, "账号资料", "个人资料", "个人信息")) {
            return none("identity.profile.read", "查询本人账号资料");
        }
        if (contains(q, "借阅记录", "我的借阅", "已借图书")) {
            return none("library.borrow.mine", "查询本人借阅记录");
        }
        if (contains(q, "在线资源", "电子资源", "数字资源")) {
            return none("library.resource.search", "查询图书馆在线资源");
        }
        if (contains(q, "图书", "书籍") && contains(q, "查", "搜", "有没有")) {
            return keyword("library.book.search", q, "检索图书");
        }
        if (contains(q, "自习室", "研讨室") && contains(q, "我的", "预约记录", "已预约")) {
            return none("library.study-room.mine", "查询本人自习室预约");
        }
        if (contains(q, "自习室", "研讨室")) {
            return none("library.study-room.search", "查询可用自习室");
        }
        if (contains(q, "商品", "商店") && contains(q, "查", "搜", "有没有")) {
            return keyword("store.product.search", q, "检索校园商品");
        }
        if (contains(q, "余额", "账户余额")) return none("store.account.read", "查询本人账户余额");
        if (contains(q, "账户流水", "消费记录", "交易流水")) {
            return none("store.ledger.read", "查询本人账户流水");
        }
        if (contains(q, "购物车")) return none("store.cart.read", "查询本人购物车");
        if (contains(q, "我的订单", "订单记录")) return none("store.orders.mine", "查询本人订单");
        if (contains(q, "住宿信息", "我的宿舍", "住在哪")) {
            return none("dorm.accommodation.read", "查询本人住宿信息");
        }
        if (contains(q, "水电", "电费", "水费")) return none("dorm.utility.read", "查询本人水电分摊");
        if (contains(q, "我的报修", "报修记录", "报修进度")) {
            return none("dorm.repair.mine", "查询本人宿舍报修");
        }
        if (contains(q, "我的请假", "请假记录")) return none("dorm.leave.mine", "查询本人宿舍请假");
        if (contains(q, "宿舍公告", "宿管公告")) return none("dorm.announcement.read", "查询宿舍公告");
        if (contains(q, "校园公告", "系统公告")) return none("campus.announcement.read", "查询校园公告");
        if (contains(q, "竞赛", "比赛") && contains(q, "查", "搜", "列表", "有哪些")) {
            return none("campus.competition.search", "查询校园竞赛");
        }
        if (contains(q, "srtp", "创新项目", "科研项目")) return none("campus.srtp.mine", "查询本人 SRTP 项目");
        if (contains(q, "教室申请", "教室预约") && contains(q, "我的", "记录", "进度")) {
            return none("campus.classroom.mine", "查询本人教室申请");
        }
        return null;
    }

    private AiToolInvocation none(String tool, String summary) {
        return new AiToolInvocation(tool, "{}", summary);
    }

    private AiToolInvocation id(String tool, long id, String summary) {
        return new AiToolInvocation(tool, "{\"id\":" + id + "}", summary);
    }

    private AiToolInvocation cart(long id, int quantity) {
        return new AiToolInvocation("store.cart.add", "{\"id\":" + id
                + ",\"quantity\":" + quantity + "}", "将商品 " + id + " 加入购物车");
    }

    private AiToolInvocation keyword(String tool, String value, String summary) {
        String clean = value.replace("查找", "").replace("搜索", "").replace("查询", "")
                .replace("图书", "").replace("书籍", "").replace("商品", "")
                .replace("商店", "").replace("有没有", "").trim();
        if (clean.isEmpty()) clean = value;
        return new AiToolInvocation(tool, "{\"keyword\":\"" + escape(clean) + "\"}", summary);
    }

    private boolean contains(String value, String... candidates) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (normalized.contains(candidate.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private Long firstNumber(String value) {
        for (int i = 0; i < value.length(); i++) if (Character.isDigit(value.charAt(i))) {
            int end = i + 1;
            while (end < value.length() && Character.isDigit(value.charAt(end))) end++;
            try { return Long.valueOf(value.substring(i, end)); }
            catch (NumberFormatException ex) { return null; }
        }
        return null;
    }

    private Long secondNumber(String value) {
        boolean first = true;
        for (int i = 0; i < value.length(); i++) if (Character.isDigit(value.charAt(i))) {
            int end = i + 1;
            while (end < value.length() && Character.isDigit(value.charAt(end))) end++;
            if (!first) {
                try { return Long.valueOf(value.substring(i, end)); }
                catch (NumberFormatException ex) { return null; }
            }
            first = false; i = end - 1;
        }
        return null;
    }

    private boolean instructionQuestion(String value) {
        return contains(value, "如何", "怎么", "怎样", "操作步骤", "在哪里", "从哪里");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\b", "\\b").replace("\f", "\\f")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
