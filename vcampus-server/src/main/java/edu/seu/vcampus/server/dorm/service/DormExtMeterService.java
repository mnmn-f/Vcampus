package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.math.*;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Meter input and utility-bill orchestration. */
final class DormExtMeterService extends DormServiceSupport {
    private static final int DEFAULT_DUE_DAYS = 15;
    private final DormExtRepository repository;
    DormExtMeterService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    DormPage<MeterReadingDto> list(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_GOVERN); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<MeterReadingDto>>() { @Override public DormPage<MeterReadingDto> run(Connection c) throws Exception { return repository.listMeterReadings(c, q); } });
    }

    MeterReadingDto save(SessionContext session, MeterReadingRequest request) {
        require(session, Permission.DORM_GOVERN); DormExtValidation.meter(request);
        return execute(new Work<MeterReadingDto>() { @Override public MeterReadingDto run(Connection c) throws Exception { return repository.saveMeterReading(c, request, session.getUserId()); } });
    }

    BillGenerateResultDto generateBills(SessionContext session, BillGenerateRequest request) {
        require(session, Permission.DORM_GOVERN); DormExtValidation.period(request);
        return execute(new Work<BillGenerateResultDto>() { @Override public BillGenerateResultDto run(Connection c) throws Exception { return generate(c, request, session.getUserId()); } });
    }

    BillGenerateResultDto generateScheduled(BillGenerateRequest request, long actor) {
        DormExtValidation.period(request); DormExtValidation.id(actor, "记账人用户号");
        return execute(new Work<BillGenerateResultDto>() { @Override public BillGenerateResultDto run(Connection c) throws Exception { return generate(c, request, actor); } });
    }

    private BillGenerateResultDto generate(Connection c, BillGenerateRequest request, long actor) throws Exception {
        LocalDateTime due = request.getDueAt() != null ? request.getDueAt() : request.getPeriodEnd().plusDays(DEFAULT_DUE_DAYS).atTime(23, 59);
        List<MeterReadingDto> pending = repository.pendingReadings(c, request.getRoomId(), request.getPeriodStart(), request.getPeriodEnd());
        if (pending.isEmpty()) throw new DormException(DormExtCommands.NO_PENDING_READING, "该账期没有待出账的抄表读数");
        List<String> notes = new ArrayList<String>(); BigDecimal total = BigDecimal.ZERO; int bills = 0; int shares = 0; int skipped = 0;
        for (MeterReadingDto reading : pending) {
            String label = reading.getBuildingCode() + " " + reading.getRoomNo();
            if (repository.billExists(c, reading.getRoomId(), reading.getPeriodStart(), reading.getPeriodEnd())) { skipped++; notes.add(label + "：该账期已有账单，跳过"); continue; }
            List<Long> residents = repository.activeResidents(c, reading.getRoomId());
            if (residents.isEmpty()) { skipped++; notes.add(label + "：无在住学生，跳过"); continue; }
            BigDecimal amount = reading.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
            if (!DormBillSplit.isSplittable(amount, residents.size())) { skipped++; notes.add(label + "：应缴 " + amount + " 元不足以按 " + residents.size() + " 人分摊，跳过"); continue; }
            long bill = repository.createBill(c, reading, amount, due, actor); List<BigDecimal> split = DormBillSplit.split(amount, residents.size());
            for (int i = 0; i < residents.size(); i++) { repository.createAllocation(c, bill, residents.get(i).longValue(), split.get(i)); shares++; }
            repository.linkReadingToBill(c, reading.getId(), bill); bills++; total = total.add(amount); notes.add(label + "：账单 " + amount + " 元，由 " + residents.size() + " 人分摊");
        }
        return new BillGenerateResultDto(bills, shares, skipped, total, notes);
    }
}
