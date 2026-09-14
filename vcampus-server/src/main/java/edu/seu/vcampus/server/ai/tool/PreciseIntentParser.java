package edu.seu.vcampus.server.ai.tool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Actions, entities and record identifiers are distinct; digits in a title are never IDs. */
final class PreciseIntentParser {
    AiToolInvocation parse(String text) {
        String q = text.split("[\\r\\n；;]", 2)[0].trim();
        boolean cancel = has(q, "取消", "撤销", "撤回", "退选", "退课");
        boolean read = !cancel && !q.matches("^(请|帮我|替我|我要|我想)?(预约|申请|支付|借|还|选|报名|提交|添加).*")
                && has(q, "查询", "查看", "查一下", "列出", "有什么", "有哪些", "多少", "什么",
                "已预约", "预约了", "已申请", "未支付", "没选", "未选", "没有选", "编号", "详情", "信息",
                "作者", "出版社", "学分", "内容", "已借", "记录", "进度");
        if (read) {
            if (has(q, "在线资源", "电子资源", "数字资源")) return invoke("library.resource.search", "{}");
            if (has(q, "借阅记录", "我的借阅", "已借图书", "借了什么", "借了哪些"))
                return invoke("library.borrow.mine", "{}");
            if (has(q, "公告")) return search(has(q, "宿舍", "住宿", "水电") ? "dorm.announcement.read"
                    : "campus.announcement.read", q);
            if (has(q, "请假")) return invoke("dorm.leave.mine", "{}");
            if (has(q, "报修", "维修")) return invoke("dorm.repair.mine", "{}");
            if (has(q, "自习室", "研讨室")) return invoke(has(q, "我的", "本人", "已预约", "预约了")
                    ? "library.study-room.mine" : "library.study-room.search", "{}");
            if (has(q, "教室")) return invoke(has(q, "本人", "我的", "已申请", "申请了")
                    ? "campus.classroom.mine" : "campus.classroom.search", "{}");
            if (has(q, "订单")) return invoke("store.orders.mine", "{}");
            if (has(q, "未选", "没选", "没有选") && has(q, "课"))
                return invoke("academic.course.search", "{\"notEnrolled\":1}");
            if (has(q, "选了", "已选", "我的选课")) return invoke("academic.enrollments.read", "{}");
            if (has(q, "课程", "可选的课") && !has(q, "课表", "成绩", "课程表"))
                return search("academic.course.search", q);
            if (has(q, "《", "图书", "作者", "出版社", "书籍"))
                return search("library.book.search", q);
        }
        if (has(q, "购物车") && has(q, "加入", "添加到", "放进", "放到", "放购物车",
                "移出", "删除", "移除", "修改", "改成")) {
            String tool = has(q, "移出", "删除", "移除") ? "store.cart.remove"
                    : has(q, "修改", "改成") ? "store.cart.update" : "store.cart.add";
            Long id = explicitId(text);
            Matcher productId = Pattern.compile("商品\\s+([1-9][0-9]*)(?:\\s|[，,]|加入)").matcher(q);
            if (id == null && productId.find()) id = Long.valueOf(productId.group(1));
            String quantity = latest(text, "商品数量", "数量", "quantity");
            Matcher amount = Pattern.compile("(?:数量\\s*[：:]?\\s*|改成\\s*)([0-9]+)").matcher(q);
            if (quantity == null && amount.find()) quantity = amount.group(1);
            String product = latest(text, "商品名称或编号", "商品", "product");
            if (product == null) product = q.replaceFirst("^(请|帮我)?(把|将)?", "")
                    .replaceAll("(加入购物车|添加到购物车|放进购物车|放到购物车|放购物车|移出购物车|从购物车删除|删除购物车|购物车移除|修改购物车)", "")
                    .replaceAll("[，,]?\\s*(数量|改成).*", "").trim();
            String args = id == null ? "\"name\":\"" + escape(product) + "\"" : "\"id\":" + id;
            if (!"store.cart.remove".equals(tool)) {
                if (quantity != null && quantity.matches("[0-9]{1,6}")) args += ",\"quantity\":" + quantity;
                else if ("store.cart.add".equals(tool)) args += ",\"quantity\":1";
            }
            return invoke(tool, "{" + args + "}");
        }
        if (cancel && has(q, "教室")) return target("campus.classroom.cancel", text,
                "取消", "撤销", "撤回", "教室申请", "教室预约", "申请", "预约");
        if (cancel && has(q, "自习室", "研讨室")) return target("library.study-room.cancel", text,
                "取消", "撤销", "自习室预约", "预约自习室", "预约", "自习室");
        if (cancel && has(q, "请假")) return target("dorm.leave.cancel", text, "取消", "撤销", "请假");
        if (has(q, "退课", "退选")) return target("academic.course.drop", text,
                "退选课程", "退课", "退选", "课程编号是", "编号是");
        if (!read && has(q, "借书", "借阅图书", "帮我借", "我要借"))
            return target("library.book.borrow", text, "借阅图书", "借书", "借");
        if (!read && has(q, "还书", "归还", "帮我还"))
            return target("library.book.return", text, "归还图书", "还书", "归还", "还");
        if (!read && has(q, "选课", "选择课程", "帮我选"))
            return target("academic.course.enroll", text, "选择课程", "选课", "选上", "选");
        if (cancel && has(q, "报名", "竞赛", "比赛")) return target("campus.competition.cancel", text,
                "取消", "撤销", "报名", "参加");
        if (!read && has(q, "报名", "参加") && has(q, "竞赛", "比赛"))
            return target("campus.competition.register", text, "报名", "参加");
        if (!read && has(q, "下单", "结算购物车")) return invoke("store.order.create", "{}");
        if (!read && has(q, "支付", "缴", "交") && has(q, "水电", "电费", "水费")) {
            Long id = explicitId(text);
            return invoke("dorm.utility.pay", id == null ? "{}" : "{\"allocationId\":" + id + "}");
        }
        return null;
    }

    private AiToolInvocation target(String tool, String text, String... actions) {
        Long id = explicitId(text);
        if (id != null) return invoke(tool, "{\"id\":" + id + "}");
        String selected = latest(text, "课程名称或编号", "书名或记录编号", "书名", "竞赛名称或编号",
                "商品名称或编号", "对象名称", "name");
        String value = selected == null ? text.split("[\\r\\n；;]", 2)[0] : selected;
        Matcher quoted = Pattern.compile("《([^》]+)》").matcher(value);
        if (quoted.find()) value = quoted.group(1);
        else if (selected == null) {
            value = value.replaceFirst("^(请|帮我|替我|我要|我想)", "");
            for (String action : actions) value = value.replace(action, "");
            value = value.replaceAll("^(本人|我的|我)", "").replaceAll("[，。？！,:：!]", "").trim();
        }
        if (value.matches("[1-9][0-9]*")) return invoke(tool, "{\"id\":" + value + "}");
        if (value.matches("(课程|图书|竞赛|比赛|教室|自习室|记录|一下|一本书|一门课)")) value = "";
        return invoke(tool, "{\"name\":\"" + escape(value) + "\"}");
    }

    static Long explicitId(String text) {
        String selected = latest(text, "对象编号", "记录编号", "id", "订单编号", "水电分摊编号");
        if (selected != null && selected.matches("[1-9][0-9]*")) {
            try { return Long.valueOf(selected); } catch (NumberFormatException ignored) { return null; }
        }
        String first = text.split("[\\r\\n；;]", 2)[0].trim();
        Matcher matcher = Pattern.compile("(?:编号|ID|id)\\s*[：:是为]?\\s*([1-9][0-9]*)\\s*[。！!]?\\s*$").matcher(first);
        if (!matcher.find()) {
            matcher = Pattern.compile("^(?:请|帮我)?(?:退选课程|退课|退选|选课|选择课程|借书|还书|借阅图书|归还图书|报名比赛|报名竞赛|取消报名|支付订单|加入购物车)\\s+([1-9][0-9]*)(?:\\s.*)?$").matcher(first);
            if (!matcher.find()) return null;
        }
        try { return Long.valueOf(matcher.group(1)); } catch (NumberFormatException ignored) { return null; }
    }

    static String latest(String text, String... labels) {
        String result = null; int at = -1;
        for (String label : labels) {
            Matcher m = Pattern.compile("(?:^|[；;\\r\\n])\\s*" + Pattern.quote(label) + "\\s*[：:]\\s*([^；;\\r\\n]+)").matcher(text);
            while (m.find()) if (m.start() >= at) { result = m.group(1).trim(); at = m.start(); }
        }
        return result;
    }

    static String searchTerm(String value) {
        Matcher quoted = Pattern.compile("《([^》]+)》").matcher(value);
        if (quoted.find()) return quoted.group(1);
        String clean = value.replaceAll("[，。？！,.?!：:;；、\"“”]", "").trim()
                .replaceFirst("^(请|帮我)?(我想知道|我想了解|查找|搜索|查询|查看|看看|浏览|我想看)", "")
                .replaceFirst("^(图书馆里|图书馆|商店里|商店|商城)(有什么书|有哪些书|有什么|有哪些)?", "")
                .replaceAll("(的)?(具体内容|完整信息|详细信息|作者|出版社|出版年份|编号|代码|学分|ISBN|isbn|可借数量|详情|信息|内容)(是谁|是什么|是多少|是啥|呢|吗)?$", "")
                .replaceAll("(的)?(是谁|是什么|是多少|是啥|呢|吗)$", "").trim();
        return clean.replaceAll("^(有什么|有哪些|可选的|可选)?(图书|书籍|课程|课|商品)$", "");
    }
    private AiToolInvocation search(String tool, String q) { return invoke(tool, "{\"keyword\":\"" + escape(searchTerm(q)) + "\"}"); }
    private AiToolInvocation invoke(String tool, String json) {
        return new AiToolInvocation(tool, json, AiToolRegistry.campusDefaults().get(tool).getDescription());
    }
    private static boolean has(String q, String... words) { for (String w : words) if (q.contains(w)) return true; return false; }
    private static String escape(String q) { return q.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"); }
}
