package edu.seu.vcampus.client.view.modules.real;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 状态和选项的用户文案；提交时保留内部值。 */
final class RealUiLabels {
    private static final Map<String, String> LABELS = new HashMap<String, String>();

    static {
        put("ACTIVE", "已启用"); put("INACTIVE", "已停用"); put("DISABLED", "已停用");
        put("CLOSED", "已关闭"); put("AUTH.INVALID_CREDENTIALS", "账号或密码错误");
        put("AUTH.ACCOUNT_DISABLED", "账号已停用"); put("COMMON.INVALID_INPUT", "输入无效");
        put("STUDENT", "学生"); put("TEACHER", "任课教师"); put("REGISTRAR", "学籍管理员");
        put("ACADEMIC_ADMIN", "教务老师"); put("LIBRARIAN", "图书管理员");
        put("STORE_MANAGER", "商店管理员"); put("DORM_MANAGER", "宿管员");
        put("REPAIR_WORKER", "维修员");
        put("AI_KNOWLEDGE_ADMIN", "AI知识管理员"); put("SYSTEM_ADMIN", "系统管理员");
        put("REQUIRED", "必修"); put("ELECTIVE", "选修"); put("PUBLIC", "公共"); put("PRACTICE", "实践"); put("OK", "成功");
        put("SUCCESS", "成功"); put("FAILURE", "失败"); put("ERROR", "失败");
        put("OPEN", "开放"); put("ON_SHELF", "在架"); put("UNAVAILABLE", "不可借");
        put("OFF_SHELF", "不可借"); put("BORROWED", "借阅中"); put("BORROWING", "借阅中");
        put("RETURNED", "已归还"); put("LOST", "遗失"); put("OVERDUE", "逾期"); put("RESERVED", "已预约");
        put("CANCELLED", "已取消"); put("CONFIRMED", "已确认"); put("IGNORED", "已忽略");
        put("ENROLLED", "在读"); put("DROPPED", "已退选"); put("SUSPENDED", "休学"); put("GRADUATED", "毕业");
        put("WITHDRAWN", "退学"); put("ENDED", "已结束"); put("PUBLISHED", "已发布"); put("DRAFT", "草稿");
        put("PREPARING", "备货中"); put("SHIPPED", "已发货"); put("IN_TRANSIT", "运输中");
        put("READY_FOR_PICKUP", "待取货"); put("DELIVERED", "已送达");
        put("SCHEDULED", "定时发布"); put("REVOKED", "已撤回"); put("EXPIRED", "已过期");
        put("ROLE", "指定角色"); put("ARCHIVED", "已归档"); put("MAINTENANCE", "维护中");
        put("ON_SALE", "在售"); put("OFF_SALE", "已下架"); put("RECHARGE", "充值");
        put("PAYMENT", "消费"); put("PURCHASE", "消费"); put("REFUND", "退款"); put("ADJUSTMENT", "调账");
        put("DORM_BILL_PAYMENT", "水电缴费"); put("CREATED", "待支付");
        put("REGISTERED", "已报名"); put("PAID", "已支付"); put("REFUNDED", "已退款");
        put("COMPLETED", "已完成"); put("NO_SHOW", "未到场"); put("FROZEN", "已冻结"); put("PARTIAL", "部分缴费");
        put("VOID", "已作废"); put("AVAILABLE", "空闲"); put("OCCUPIED", "在住");
        put("ALL", "不限"); put("MIXED", "混住"); put("MALE", "男生"); put("FEMALE", "女生");
        put("PENDING", "待审批"); put("APPROVED", "已通过"); put("REJECTED", "已驳回");
        put("PERSONAL", "事假"); put("ILLNESS", "病假"); put("OFF_CAMPUS", "离校"); put("OTHER", "其他");
        put("DAILY", "日用百货"); put("FOOD", "食品饮料"); put("STATIONERY", "文具用品"); put("CULTURE", "校园文创");
        put("FULL", "满员"); put("STANDARD", "标准间"); put("SUITE", "套间"); put("SPECIAL", "特殊房型");
        put("TEACHING", "教学"); put("LAB", "实验"); put("MEETING", "会议"); put("PASS", "通过"); put("FAIL", "不通过");
        put("WATER", "给排水"); put("PLUMBING", "给排水"); put("LIGHTING", "照明");
        put("ELECTRIC", "电气"); put("ELECTRICAL", "电气"); put("NETWORK", "网络");
        put("FURNITURE", "家具"); put("APPLIANCE", "家电"); put("AIR_CONDITIONING", "空调");
        put("EQUIPMENT", "设备"); put("DOOR_WINDOW", "门窗");
        put("SUBMITTED", "待派单"); put("ACCEPTED", "已派单"); put("IN_PROGRESS", "处理中");
        put("PENDING_REVIEW", "待宿管审核");
        put("CHECK_IN", "入住"); put("TRANSFER", "调宿"); put("CHECK_OUT", "退宿");
        put("UNPAID", "待缴费"); put("WAIVED", "已减免"); put("HIGH", "高");
        put("LOW", "低"); put("NORMAL", "正常"); put("URGENT", "紧急"); put("RECTIFICATION_REQUIRED", "待整改");
        put("RECTIFIED", "已整改"); put("CLEARED", "已清除");
    }

    private RealUiLabels() { }

    static String status(String value) {
        if (value == null || value.trim().isEmpty()) return "--";
        String label = LABELS.get(value.toUpperCase(Locale.ROOT));
        return label == null ? value : label;
    }

    private static void put(String code, String label) {
        LABELS.put(code, label);
    }
}
