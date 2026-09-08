package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormBillSplit;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 抄表录入、账单生成分摊及其权限边界。 */
public final class DormMeterBillingTest {
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.setResidents(10L, 11L, 12L);
        service = new DormExtService(repository);
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
        student = DormExtTestSupport.session(11L, Role.STUDENT);
    }

    @Test public void managerCanRecordAndListMeterReadings() {
        MeterReadingDto saved = service.saveMeterReading(manager, reading("120.5", "8.0"));
        assertNotNull(saved); assertEquals(10L, saved.getRoomId()); assertNull(saved.getBillId());
        assertEquals(0, new BigDecimal("100.30").compareTo(saved.getTotalAmount()));
        assertEquals(1L, service.meterReadings(manager, DormPageQuery.all()).getTotalElements());
    }
    @Test public void sameRoomAndPeriodUpdatesInsteadOfDuplicating() {
        long id = service.saveMeterReading(manager, reading("100", "5")).getId();
        MeterReadingDto second = service.saveMeterReading(manager, reading("180", "9"));
        assertEquals(id, second.getId()); assertEquals(1L,
                service.meterReadings(manager, DormPageQuery.all()).getTotalElements());
        assertEquals(0, new BigDecimal("180").compareTo(second.getElectricityUnits()));
    }
    @Test public void studentCannotTouchMeterReadings() {
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.meterReadings(student, DormPageQuery.all()); }
        });
    }
    @Test public void invalidPeriodAndRoomAreRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.INVALID_INPUT, new DormExtTestSupport.Action() {
            public void run() { service.saveMeterReading(manager, new MeterReadingRequest(
                    10L, TO, FROM, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("0.6"), new BigDecimal("3.5"))); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.ROOM_NOT_FOUND, new DormExtTestSupport.Action() {
            public void run() { service.saveMeterReading(manager, new MeterReadingRequest(
                    999L, FROM, TO, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("0.6"), new BigDecimal("3.5"))); }
        });
    }
    @Test public void generateSplitsBillEvenlyAmongResidents() {
        service.saveMeterReading(manager, reading("100", "10"));
        BillGenerateResultDto r = service.generateBills(manager, generate(null));
        assertEquals(1, r.getBillCount()); assertEquals(2, r.getAllocationCount());
        assertEquals(0, r.getSkippedCount()); assertEquals(0, new BigDecimal("95.00").compareTo(r.getTotalAmount()));
        assertEquals(1, repository.billCount());
    }
    @Test public void indivisibleAmountKeepsSumExact() {
        repository.setResidents(10L, 11L, 12L, 13L);
        service.saveMeterReading(manager, reading("100", "1")); service.generateBills(manager, generate(null));
        List<BigDecimal> shares = repository.allocationsOf(1000L); BigDecimal sum = BigDecimal.ZERO;
        assertEquals(3, shares.size());
        for (BigDecimal share : shares) { sum = sum.add(share); assertTrue(share.signum() > 0); }
        assertEquals(0, new BigDecimal("63.50").compareTo(sum));
    }
    @Test public void generationLocksReadingAndSecondRunFindsNothing() {
        service.saveMeterReading(manager, reading("100", "10")); service.generateBills(manager, generate(null));
        DormExtTestSupport.assertCode(DormExtCommands.METER_LOCKED, new DormExtTestSupport.Action() {
            public void run() { service.saveMeterReading(manager, reading("200", "20")); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.NO_PENDING_READING, new DormExtTestSupport.Action() {
            public void run() { service.generateBills(manager, generate(null)); }
        });
    }
    @Test public void roomWithoutResidentsIsSkipped() {
        repository.addRoom(20L, "D1", "102");
        service.saveMeterReading(manager, new MeterReadingRequest(20L, FROM, TO,
                new BigDecimal("50"), new BigDecimal("5"), new BigDecimal("0.6"), new BigDecimal("3.5")));
        service.saveMeterReading(manager, reading("100", "10"));
        BillGenerateResultDto r = service.generateBills(manager, generate(null));
        assertEquals(1, r.getBillCount()); assertEquals(1, r.getSkippedCount()); assertEquals(2, r.getNotes().size());
    }
    @Test public void studentCannotGenerateAndNoPendingIsRejected() {
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.generateBills(student, generate(null)); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.NO_PENDING_READING, new DormExtTestSupport.Action() {
            public void run() { service.generateBills(manager, generate(null)); }
        });
    }
    @Test public void splitDistributesRemainderToLeadingShares() {
        List<BigDecimal> shares = DormBillSplit.split(new BigDecimal("10.00"), 3);
        assertEquals(3, shares.size()); assertEquals(0, new BigDecimal("3.34").compareTo(shares.get(0)));
        assertEquals(0, new BigDecimal("3.33").compareTo(shares.get(1)));
        assertEquals(0, new BigDecimal("3.33").compareTo(shares.get(2)));
    }
    @Test public void amountTooSmallCannotBeSplit() {
        assertFalse(DormBillSplit.isSplittable(new BigDecimal("0.03"), 4));
        assertTrue(DormBillSplit.isSplittable(new BigDecimal("0.04"), 4));
        assertFalse(DormBillSplit.isSplittable(BigDecimal.ZERO, 1));
    }

    private static BillGenerateRequest generate(Long roomId) { return new BillGenerateRequest(roomId, FROM, TO, null); }
    private static MeterReadingRequest reading(String electricity, String water) {
        return new MeterReadingRequest(10L, FROM, TO, new BigDecimal(electricity), new BigDecimal(water),
                new BigDecimal("0.6"), new BigDecimal("3.5"));
    }
}
