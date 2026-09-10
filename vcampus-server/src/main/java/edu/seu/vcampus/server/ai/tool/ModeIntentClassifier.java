package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.ai.AiMode;

import java.util.Locale;

/** 在进入模型或业务工具前识别用户应该使用的助手模式。 */
public final class ModeIntentClassifier {
    public AiMode recommend(String text, AiToolInvocation invocation, AiToolRegistry tools) {
        if (invocation != null) {
            AiTool tool = tools.get(invocation.getToolName());
            if (tool != null) return tool.isWriteOperation() ? AiMode.TASK : AiMode.QA;
        }
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (!campusRelated(value)) return AiMode.CHAT;
        if (contains(value, "如何", "怎么", "怎样", "步骤", "流程", "在哪里", "从哪里",
                "是什么", "多少", "哪些", "有什么", "能不能", "可以吗")) return AiMode.QA;
        if (contains(value, "帮我", "替我", "给我") && contains(value,
                "预约", "报名", "取消", "提交", "申请", "选课", "退课", "借书", "还书",
                "支付", "付款", "缴费", "加入购物车", "删除", "修改")) return AiMode.TASK;
        return AiMode.QA;
    }

    private boolean campusRelated(String value) {
        return contains(value, "vcampus", "校园系统", "本系统", "学籍", "成绩", "课表", "选课",
                "图书馆", "图书", "借阅", "自习室", "商店", "购物车", "订单", "余额", "宿舍",
                "报修", "请假", "水电", "校园公告", "竞赛", "srtp", "教室申请", "教室预约");
    }

    private boolean contains(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }
}
