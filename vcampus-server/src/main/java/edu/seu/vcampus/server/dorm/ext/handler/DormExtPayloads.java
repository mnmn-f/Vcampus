package edu.seu.vcampus.server.dorm.ext.handler;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.*;

/** Typed payload boundary for dorm-extension commands. */
final class DormExtPayloads {
    private DormExtPayloads() { }
    static DormPageQuery query(Object value) { return value == null ? DormPageQuery.all() : cast(value, DormPageQuery.class); }
    static BillGenerateRequest bill(Object value) { return cast(value, BillGenerateRequest.class); }
    static AccessPolicyRequest policy(Object value) { return cast(value, AccessPolicyRequest.class); }
    static RepairEntryPermitRequest permit(Object value) { return cast(value, RepairEntryPermitRequest.class); }
    static RepairWorkRequest work(Object value) { return cast(value, RepairWorkRequest.class); }
    static RepairAssignRequest assign(Object value) { return cast(value, RepairAssignRequest.class); }
    static Long orderId(Object value) { return value == null ? null : cast(value, Long.class); }
    static String taskName(Object value) { return value == null ? null : cast(value, String.class); }
    static NoticeExtraRequest notice(Object value) { return cast(value, NoticeExtraRequest.class); }
    static RoomDeleteRequest room(Object value) { return cast(value, RoomDeleteRequest.class); }
    static HygieneScoreSubmitRequest hygiene(Object value) { return cast(value, HygieneScoreSubmitRequest.class); }
    static HygieneDetailRequest detail(Object value) { return cast(value, HygieneDetailRequest.class); }
    static HygieneTaskGenerateRequest task(Object value) { return value == null ? new HygieneTaskGenerateRequest(null, null) : cast(value, HygieneTaskGenerateRequest.class); }
    static VisitorRegistrationRequest visitor(Object value) { return cast(value, VisitorRegistrationRequest.class); }
    static VisitorAuditRequest audit(Object value) { return cast(value, VisitorAuditRequest.class); }
    static WarningScanRequest scan(Object value) { return value == null ? new WarningScanRequest(null) : cast(value, WarningScanRequest.class); }
    static WarningHandleRequest handle(Object value) { return cast(value, WarningHandleRequest.class); }
    static WarningConfigRequest config(Object value) { return cast(value, WarningConfigRequest.class); }
    static MeterReadingRequest meter(Object value) { return cast(value, MeterReadingRequest.class); }
    private static <T> T cast(Object value, Class<T> type) {
        if (!type.isInstance(value)) throw new IllegalArgumentException("payload must be " + type.getSimpleName());
        return type.cast(value);
    }
}
