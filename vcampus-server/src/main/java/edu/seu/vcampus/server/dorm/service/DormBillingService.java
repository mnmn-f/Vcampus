package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 本人水电分摊查询与缴费。 */
final class DormBillingService extends DormServiceSupport {
    private final DormRepository repository;

    DormBillingService(DormRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    DormPage<UtilityBillDto> mine(final SessionContext session, final DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<DormPage<UtilityBillDto>>() { public DormPage<UtilityBillDto> run(java.sql.Connection c) throws Exception { return repository.listBills(c, session.getUserId(), q); } });
    }

    DormPage<UtilityBillDto> managerBills(final SessionContext session, final UtilityBillQuery query) {
        require(session, Permission.DORM_GOVERN);
        final UtilityBillQuery q = query == null ? UtilityBillQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        if (q.getRoomId() != null && q.getRoomId().longValue() <= 0L) {
            throw new DormException(DormCommands.INVALID_INPUT, "房间编号不正确");
        }
        if (q.getPeriodStart() != null && q.getPeriodEnd() != null
                && q.getPeriodStart().isAfter(q.getPeriodEnd())) {
            throw new DormException(DormCommands.INVALID_INPUT, "账期范围不正确");
        }
        return execute(new Work<DormPage<UtilityBillDto>>() { public DormPage<UtilityBillDto> run(java.sql.Connection c) throws Exception { return repository.listAllBills(c, q); } });
    }

    UtilityBillDto pay(final SessionContext session, final UtilityPaymentRequest request) {
        require(session, Permission.DORM_BILL_PAY);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "缴费参数不能为空");
        id(request.getAllocationId(), "水电分摊");
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new DormException(DormCommands.INVALID_INPUT, "支付幂等键不能为空");
        }
        final UtilityPaymentRequest normalized = new UtilityPaymentRequest(request.getAllocationId(),
                request.getIdempotencyKey().trim());
        return execute(new Work<UtilityBillDto>() { public UtilityBillDto run(java.sql.Connection c) throws Exception { return repository.pay(c, session.getUserId(), normalized); } });
    }
}
