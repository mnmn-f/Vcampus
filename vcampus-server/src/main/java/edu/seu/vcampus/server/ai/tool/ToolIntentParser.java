package edu.seu.vcampus.server.ai.tool;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 无模型或明确校园指令时使用的确定性意图解析器。 */
public final class ToolIntentParser {
    public AiToolInvocation parse(String text) {
        String q = text == null ? "" : text.trim();
        if (instructionQuestion(q)) return null;
        Long id = firstNumber(q);
        if (contains(q, "取消自习室", "取消研讨室") && contains(q, "预约")) {
            return id == null ? none("library.study-room.cancel", "取消自习室预约")
                    : id("library.study-room.cancel", id.longValue(), "取消自习室预约 " + id);
        }
        if (contains(q, "预约", "预订") && contains(q, "自习室", "研讨室")) {
            return new AiToolInvocation("library.study-room.reserve", studyRoomArguments(q), "预约自习室");
        }
        if (contains(q, "移出购物车", "从购物车删除", "删除购物车", "购物车移除")) {
            String product = cleanEntity(q, "帮我", "把", "将", "移出购物车", "从购物车删除",
                    "删除购物车", "购物车移除");
            return id != null ? id("store.cart.remove", id.longValue(), "移除购物车商品 " + id)
                    : name("store.cart.remove", product, "移除购物车商品");
        }
        if (contains(q, "修改购物车", "购物车数量", "改成") && contains(q, "购物车", "数量")) {
            Long quantity = secondNumber(q);
            String json = id == null ? "{}" : "{\"id\":" + id
                    + (quantity == null ? "" : ",\"quantity\":" + quantity) + "}";
            return new AiToolInvocation("store.cart.update", json, "修改购物车商品数量");
        }
        if (contains(q, "支付订单", "订单付款", "付款订单")) {
            String json = id == null ? "{}" : "{\"orderId\":" + id + "}";
            return new AiToolInvocation("store.order.pay", json, "支付订单" + (id == null ? "" : " " + id));
        }
        if (contains(q, "领取优惠券", "领券")) {
            String code = cleanEntity(q, "帮我", "领取优惠券", "领券", "优惠券", "代码");
            return new AiToolInvocation("store.coupon.claim",
                    code.isEmpty() ? "{}" : "{\"code\":\"" + escape(code) + "\"}", "领取优惠券");
        }
        if (contains(q, "报修", "维修") && !contains(q, "记录", "进度", "我的报修")) {
            return new AiToolInvocation("dorm.repair.create", repairArguments(q), "提交宿舍报修");
        }
        if (contains(q, "取消请假", "撤销请假")) {
            return id == null ? none("dorm.leave.cancel", "取消请假")
                    : id("dorm.leave.cancel", id.longValue(), "取消请假 " + id);
        }
        if (contains(q, "请假", "离校申请") && !contains(q, "记录", "我的请假")) {
            return new AiToolInvocation("dorm.leave.submit", leaveArguments(q), "提交宿舍请假");
        }
        if (contains(q, "缴水电", "交水电", "支付水电", "缴电费", "交电费")) {
            String json = id == null ? "{}" : "{\"allocationId\":" + id + "}";
            return new AiToolInvocation("dorm.utility.pay", json, "支付水电分摊");
        }
        if (contains(q, "申请教室", "预约教室") && !contains(q, "我的", "记录", "进度")) {
            return new AiToolInvocation("campus.classroom.apply", classroomArguments(q), "提交教室申请");
        }
        if (contains(q, "取消教室申请", "取消教室预约")) {
            return id == null ? none("campus.classroom.cancel", "取消教室申请")
                    : id("campus.classroom.cancel", id.longValue(), "取消教室申请 " + id);
        }
        if (contains(q, "报名了什么", "报名了哪些", "参加了什么", "参加了哪些",
                "我的竞赛", "我的比赛", "已报名竞赛", "已报名比赛")) {
            return none("campus.competition.mine", "查询本人已报名竞赛");
        }
        if (contains(q, "退课", "退选") && id != null) {
            return id("academic.course.drop", id.longValue(), "退选课程 " + id);
        }
        if (contains(q, "选课", "选择课程") && id != null) {
            return id("academic.course.enroll", id.longValue(), "选修课程 " + id);
        }
        if (contains(q, "退课", "退选")) {
            String target = firstNonBlank(labeled(q, "课程名称或编号", "课程", "course"),
                    cleanEntity(q, "帮我", "退课", "退选"));
            return name("academic.course.drop", target, "退选课程");
        }
        if (contains(q, "选课", "选择课程")) {
            String target = firstNonBlank(labeled(q, "课程名称或编号", "课程", "course"),
                    cleanEntity(q, "帮我", "选课", "选择课程"));
            return name("academic.course.enroll", target, "选修课程");
        }
        if (contains(q, "还书", "归还图书") && id != null) {
            return id("library.book.return", id.longValue(), "归还借阅记录 " + id);
        }
        if (contains(q, "借书", "借阅图书") && id != null) {
            return id("library.book.borrow", id.longValue(), "借阅图书 " + id);
        }
        if (contains(q, "还书", "归还图书")) {
            String target = firstNonBlank(labeled(q, "书名或记录编号", "书名", "book"),
                    cleanEntity(q, "帮我", "还书", "归还图书"));
            return name("library.book.return", target, "归还图书");
        }
        if (contains(q, "借书", "借阅图书")) {
            String target = firstNonBlank(labeled(q, "书名或记录编号", "书名", "book"),
                    cleanEntity(q, "帮我", "借书", "借阅图书"));
            return name("library.book.borrow", target, "借阅图书");
        }
        if (contains(q, "取消比赛报名", "取消竞赛报名", "取消报名") && id != null) {
            return id("campus.competition.cancel", id.longValue(), "取消竞赛报名 " + id);
        }
        if (contains(q, "报名比赛", "报名竞赛", "竞赛报名") && id != null) {
            return id("campus.competition.register", id.longValue(), "报名竞赛 " + id);
        }
        if (contains(q, "取消比赛报名", "取消竞赛报名", "取消报名")
                || (contains(q, "取消") && contains(q, "比赛", "竞赛"))) {
            String name = cleanEntity(q, "帮我", "取消比赛报名", "取消竞赛报名", "取消报名",
                    "取消", "报名", "参加", "比赛", "竞赛");
            name = firstNonBlank(labeled(q, "竞赛名称或编号", "竞赛", "competition"), name);
            return name("campus.competition.cancel", name, "取消竞赛报名");
        }
        if (contains(q, "报名比赛", "报名竞赛", "竞赛报名", "参加比赛", "参加竞赛")
                || (contains(q, "报名", "参加") && contains(q, "比赛", "竞赛"))) {
            String name = cleanEntity(q, "帮我", "报名比赛", "报名竞赛", "竞赛报名", "参加比赛", "参加竞赛",
                    "报名", "参加", "比赛", "竞赛");
            name = firstNonBlank(labeled(q, "竞赛名称或编号", "竞赛", "competition"), name);
            return name("campus.competition.register", name, "报名竞赛");
        }
        if (contains(q, "加入购物车", "添加到购物车") && id != null) {
            Long quantity = secondNumber(q);
            return cart(id.longValue(), quantity == null ? 1 : quantity.intValue());
        }
        if (contains(q, "加入购物车", "添加到购物车", "放进购物车", "放到购物车", "放购物车")) {
            Long quantity = firstNumber(q);
            String product = firstNonBlank(labeled(q, "商品名称或编号", "商品", "product"),
                    cleanEntity(q, "帮我", "把", "将", "加入购物车", "添加到购物车",
                    "放进购物车", "放到购物车", "放购物车", "数量", "件", "个"));
            return namedCart(product, quantity == null ? 1 : quantity.intValue());
        }
        if (contains(q, "提交订单", "创建订单", "生成订单")) {
            return none("store.order.create", "从购物车创建订单");
        }
        if (contains(q, "我的选课", "已选课程", "选了什么课", "选了哪些课")) {
            return none("academic.enrollments.read", "查询本人已选课程");
        }
        if (contains(q, "课表", "课程表")) return none("academic.schedule.read", "查询本人课表");
        if (contains(q, "成绩", "分数", "绩点", "gpa")) return none("student.grades.read", "查询本人成绩");
        if (contains(q, "学籍", "学籍信息", "我的学院", "我的专业", "我的班级", "我的学号",
                "入学年份", "毕业年份", "学历", "培养层次")) {
            return none("student.profile.read", "查询本人学籍资料");
        }
        if (contains(q, "账号资料", "个人资料", "个人信息")) {
            return none("identity.profile.read", "查询本人账号资料");
        }
        if (contains(q, "借阅记录", "我的借阅", "已借图书")) {
            return none("library.borrow.mine", "查询本人借阅记录");
        }
        if (contains(q, "在线资源", "电子资源", "数字资源")) {
            return none("library.resource.search", "查询图书馆在线资源");
        }
        if ((contains(q, "图书", "书籍", "书目", "馆藏")
                || contains(q, "什么书", "哪些书", "有书")) &&
                (contains(q, "查", "搜", "有没有", "什么", "哪些", "看看", "浏览")
                        || contains(q, "图书", "书籍", "书目", "馆藏"))) {
            return keyword("library.book.search", q, "检索图书");
        }
        if (contains(q, "自习室", "研讨室") && contains(q, "我的", "预约记录", "已预约")) {
            return none("library.study-room.mine", "查询本人自习室预约");
        }
        if (contains(q, "自习室", "研讨室")) {
            return none("library.study-room.search", "查询可用自习室");
        }
        if (contains(q, "商品", "商店", "买东西", "商城") && !contains(q, "购物车")
                && !contains(q, "订单")) {
            return keyword("store.product.search", q, "检索校园商品");
        }
        if (contains(q, "余额", "账户余额")) return none("store.account.read", "查询本人账户余额");
        if (contains(q, "账户流水", "消费记录", "交易流水")) {
            return none("store.ledger.read", "查询本人账户流水");
        }
        if (contains(q, "购物车")) return none("store.cart.read", "查询本人购物车");
        if (contains(q, "我的订单", "订单记录", "订单", "购买记录", "买过什么")) {
            return none("store.orders.mine", "查询本人订单");
        }
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
        if (contains(q, "竞赛", "比赛") && contains(q, "查", "搜", "列表", "有哪些", "有什么", "看看", "目前")) {
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

    private AiToolInvocation name(String tool, String value, String summary) {
        return new AiToolInvocation(tool, "{\"name\":\"" + escape(value) + "\"}",
                summary + (value.isEmpty() ? "" : "“" + value + "”"));
    }

    private AiToolInvocation namedCart(String value, int quantity) {
        return new AiToolInvocation("store.cart.add", "{\"name\":\"" + escape(value)
                + "\",\"quantity\":" + Math.max(1, quantity) + "}",
                "将商品“" + value + "”加入购物车");
    }

    private AiToolInvocation keyword(String tool, String value, String summary) {
        String quoted = between(value, '《', '》');
        String clean = quoted != null ? quoted : value.replaceAll("[，。？！,.?!：:;；、\"'“”《》]", " ")
                .replace("查找", "").replace("搜索", "").replace("查询", "")
                .replace("查看", "").replace("看看", "").replace("浏览", "")
                .replace("图书馆有什么书", "").replace("图书馆有哪些书", "")
                .replace("图书馆", "").replace("有什么书", "").replace("什么书", "").replace("哪些书", "")
                .replace("图书", "").replace("书籍", "").replace("商品", "")
                .replace("书目", "").replace("馆藏", "").replace("商城", "")
                .replace("作者", "").replace("出版社", "").replace("出版年份", "")
                .replace("isbn", "").replace("ISBN", "").replace("位置", "")
                .replace("分类", "").replace("详情", "").replace("完整信息", "")
                .replace("信息", "").replace("可借数量", "").replace("有几本", "")
                .replace("商店", "").replace("有什么", "").replace("有哪些", "")
                .replace("有没有", "").replace("是谁", "").replace("是什么", "")
                .replace("里的", "").replace("里", "").replaceAll("的\\s*$", "").trim();
        return new AiToolInvocation(tool, "{\"keyword\":\"" + escape(clean) + "\"}", summary);
    }

    private String between(String value, char open, char close) {
        int start = value.indexOf(open); int end = start < 0 ? -1 : value.indexOf(close, start + 1);
        if (start < 0 || end <= start + 1) return null;
        return value.substring(start + 1, end).trim();
    }

    private String cleanEntity(String value, String... words) {
        String out = value;
        for (String word : words) out = out.replace(word, "");
        return out.replaceAll("[，。？！,.!?：:]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.trim().isEmpty() ? fallback : preferred.trim();
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
        return contains(value, "如何", "怎么", "怎样", "操作步骤", "在哪里", "从哪里",
                "写一封", "邮件", "文案", "润色", "解释");
    }

    private String studyRoomArguments(String value) {
        return object(numberField(value, "roomId", "自习室编号", "研讨室编号", "roomId"),
                textField(value, "startAt", "开始时间", "startAt"),
                textField(value, "endAt", "结束时间", "endAt"));
    }

    private String repairArguments(String value) {
        return object(numberField(value, "roomId", "房间编号", "roomId"),
                textField(value, "category", "故障类别", "category"),
                textField(value, "description", "故障描述", "description"),
                textField(value, "priority", "优先级", "priority"));
    }

    private String leaveArguments(String value) {
        return object(textField(value, "leaveType", "请假类型", "leaveType"),
                textField(value, "startAt", "开始时间", "startAt"),
                textField(value, "endAt", "结束时间", "endAt"),
                textField(value, "reason", "原因", "reason"));
    }

    private String classroomArguments(String value) {
        return object(numberField(value, "classroomId", "教室编号", "classroomId"),
                textField(value, "purpose", "用途", "purpose"),
                textField(value, "startAt", "开始时间", "startAt"),
                textField(value, "endAt", "结束时间", "endAt"));
    }

    private String numberField(String source, String key, String... labels) {
        String value = labeled(source, labels);
        if (value == null || !value.matches("\\d+")) return null;
        return "\"" + key + "\":" + value;
    }

    private String textField(String source, String key, String... labels) {
        String value = labeled(source, labels);
        return value == null ? null : "\"" + key + "\":\"" + escape(value) + "\"";
    }

    private String labeled(String source, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile("(?:^|[；;\\r\\n])\\s*" + Pattern.quote(label)
                    + "\\s*[：:]\\s*([^；;\\r\\n]+)").matcher(source);
            if (matcher.find()) return matcher.group(1).trim();
        }
        return null;
    }

    private String object(String... fields) {
        StringBuilder out = new StringBuilder("{");
        for (String field : fields) {
            if (field == null) continue;
            if (out.length() > 1) out.append(',');
            out.append(field);
        }
        return out.append('}').toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\b", "\\b").replace("\f", "\\f")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
