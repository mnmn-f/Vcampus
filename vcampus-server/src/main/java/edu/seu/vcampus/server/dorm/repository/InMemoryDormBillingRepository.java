package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 内存水电账单仓储及一次性支付保护。 */
final class InMemoryDormBillingRepository implements DormBillingRepository {
    private final InMemoryDormState state;

    InMemoryDormBillingRepository(InMemoryDormState state) { this.state = state; }

    @Override
    public synchronized DormPage<UtilityBillDto> listBills(Connection c, long studentId,
                                                            DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<UtilityBillDto> rows = new ArrayList<UtilityBillDto>();
        for (UtilityBillDto item : state.bills.values()) {
            Long owner = owner(item);
            if (owner != null && owner.longValue() == studentId && matches(item, query)) {
                rows.add(withOwner(item, owner));
            }
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormPage<UtilityBillDto> listAllBills(Connection c, UtilityBillQuery q) {
        UtilityBillQuery query = q == null ? UtilityBillQuery.all() : q;
        List<UtilityBillDto> rows = new ArrayList<UtilityBillDto>();
        for (UtilityBillDto item : state.bills.values()) {
            UtilityBillDto visible = withOwner(item, owner(item));
            if (matches(visible, query)) rows.add(visible);
        }
        return InMemoryDormSupport.page(rows, query.getPage(), query.getPageSize());
    }

    @Override
    public synchronized UtilityBillDto lockAllocation(Connection c, long studentId,
                                                       long allocationId) {
        UtilityBillDto value = state.bills.get(Long.valueOf(allocationId));
        checkOwner(value, studentId);
        return withOwner(value, owner(value));
    }

    @Override
    public synchronized UtilityBillDto pay(Connection c, long studentId,
                                           UtilityPaymentRequest request) {
        UtilityBillDto old = lockAllocation(c, studentId, request.getAllocationId());
        if (old == null) throw new DormRepositoryException(DormCommands.BILL_NOT_FOUND, "水电分摊不存在");
        if ("PAID".equals(old.getAllocationStatus())) {
            throw new DormRepositoryException(DormCommands.BILL_ALREADY_PAID, "该分摊已经缴费");
        }
        if (!"UNPAID".equals(old.getAllocationStatus())) {
            throw new DormRepositoryException(DormCommands.BILL_NOT_PAYABLE, "该分摊当前不可缴费");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "支付幂等键不能为空");
        }
        UtilityBillDto value = new UtilityBillDto(old.getAllocationId(), old.getBillId(), old.getRoomId(),
                owner(old), old.getRoomNo(), old.getPeriodStart(), old.getPeriodEnd(), old.getElectricityUnits(),
                old.getWaterUnits(), old.getTotalAmount(), old.getAllocatedAmount(), old.getBillStatus(),
                "PAID", old.getDueAt(), Long.valueOf(state.nextPayment++), LocalDateTime.now());
        state.bills.put(Long.valueOf(value.getAllocationId()), value);
        return value;
    }

    private void checkOwner(UtilityBillDto value, long studentId) {
        if (value == null) throw new DormRepositoryException(DormCommands.BILL_NOT_FOUND, "水电分摊不存在");
        Long owner = owner(value);
        if (owner == null || owner.longValue() != studentId) {
            throw new DormRepositoryException(DormCommands.BILL_NOT_FOUND, "水电分摊不存在");
        }
    }

    private Long owner(UtilityBillDto value) {
        if (value == null) return null;
        Long owner = state.billOwners.get(Long.valueOf(value.getAllocationId()));
        return owner == null ? value.getStudentUserId() : owner;
    }

    private static boolean matches(UtilityBillDto value, DormPageQuery query) {
        return InMemoryDormSupport.status(query.getStatus(), value.getAllocationStatus())
                && InMemoryDormSupport.matches(query.getKeyword(), value.getRoomNo(),
                String.valueOf(value.getPeriodStart()), String.valueOf(value.getPeriodEnd()));
    }

    private static boolean matches(UtilityBillDto value, UtilityBillQuery query) {
        if (!InMemoryDormSupport.status(query.getStatus(), value.getAllocationStatus())) return false;
        if (query.getRoomId() != null && query.getRoomId().longValue() != value.getRoomId()) return false;
        Long owner = value.getStudentUserId();
        if (!InMemoryDormSupport.matches(query.getKeyword(), value.getRoomNo(),
                owner == null ? null : String.valueOf(owner), String.valueOf(value.getPeriodStart()),
                String.valueOf(value.getPeriodEnd()))) return false;
        if (query.getPeriodStart() != null && value.getPeriodEnd() != null
                && value.getPeriodEnd().isBefore(query.getPeriodStart())) return false;
        return query.getPeriodEnd() == null || value.getPeriodStart() == null
                || !value.getPeriodStart().isAfter(query.getPeriodEnd());
    }

    private static UtilityBillDto withOwner(UtilityBillDto value, Long owner) {
        if (value == null || owner == null || owner.equals(value.getStudentUserId())) return value;
        return new UtilityBillDto(value.getAllocationId(), value.getBillId(), value.getRoomId(), owner,
                value.getRoomNo(), value.getPeriodStart(), value.getPeriodEnd(), value.getElectricityUnits(),
                value.getWaterUnits(), value.getTotalAmount(), value.getAllocatedAmount(), value.getBillStatus(),
                value.getAllocationStatus(), value.getDueAt(), value.getPaidTransactionId(), value.getPaidAt());
    }
}
