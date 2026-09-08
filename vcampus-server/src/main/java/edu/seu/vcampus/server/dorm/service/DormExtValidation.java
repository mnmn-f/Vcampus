package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import java.math.BigDecimal;

/** Shared extension-specific validation; all branches use one stable result-code namespace. */
final class DormExtValidation {
    private DormExtValidation() { }
    static void id(long value, String field) { if (value <= 0L) throw invalid(field + "不正确"); }
    static void text(String value, String field) { if (value == null || value.trim().isEmpty()) throw invalid(field + "不能为空"); }
    static void page(DormPageQuery query) { if (query.getPage() <= 0 || query.getPageSize() <= 0 || query.getPageSize() > 100) throw invalid("分页参数不正确"); }
    static DormException invalid(String message) { return new DormException(DormExtCommands.INVALID_INPUT, message); }
    static void meter(MeterReadingRequest r) {
        if (r == null) throw invalid("抄表参数不能为空"); id(r.getRoomId(), "房间编号");
        if (r.getPeriodStart() == null || r.getPeriodEnd() == null) throw invalid("账期起止日期不能为空");
        if (r.getPeriodEnd().isBefore(r.getPeriodStart())) throw invalid("账期结束日期不能早于开始日期");
        nonNegative(r.getElectricityUnits(), "用电量"); nonNegative(r.getWaterUnits(), "用水量");
        positive(r.getElectricityPrice(), "电费单价"); positive(r.getWaterPrice(), "水费单价");
    }
    static void period(BillGenerateRequest r) {
        if (r == null) throw invalid("出账参数不能为空"); if (r.getRoomId() != null) id(r.getRoomId().longValue(), "房间编号");
        if (r.getPeriodStart() == null || r.getPeriodEnd() == null) throw invalid("账期起止日期不能为空");
        if (r.getPeriodEnd().isBefore(r.getPeriodStart())) throw invalid("账期结束日期不能早于开始日期");
    }
    static void visitor(VisitorRegistrationRequest r) {
        if (r == null) throw invalid("登记参数不能为空"); text(r.getVisitorName(), "来访人姓名"); text(r.getVisitorIdCard(), "来访人证件号"); text(r.getVisitReason(), "来访事由");
        if (r.getStartAt() == null || r.getEndAt() == null) throw invalid("来访起止时间不能为空");
        if (!r.getEndAt().isAfter(r.getStartAt())) throw invalid("离开时间必须晚于来访时间");
    }
    static void nonNegative(BigDecimal value, String field) { if (value == null || value.signum() < 0) throw invalid(field + "不能为负数"); }
    static void positive(BigDecimal value, String field) { if (value == null || value.signum() <= 0) throw invalid(field + "必须大于零"); }
    static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
