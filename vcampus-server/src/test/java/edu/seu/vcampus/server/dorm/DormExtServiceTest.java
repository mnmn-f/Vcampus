package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.ext.registry.DormExtCommandRegistry;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormBillSplit;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.threeten.bp.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 宿舍扩展模块：命令路由、权限边界、抄表录入与水电账单生成分摊。 */
public final class DormExtServiceTest {
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 9, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 9, 30);

    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.setResidents(10L, Long.valueOf(11L), Long.valueOf(12L));
        service = new DormExtService(repository);
        manager = session(90L, Role.DORM_MANAGER);
        student = session(11L, Role.STUDENT);
    }

    // ---------- P0：抄表录入 ----------

    @Test
    public void managerCanRecordAndListMeterReadings() {
        MeterReadingDto saved = service.saveMeterReading(manager, reading("120.5", "8.0"));
        assertNotNull(saved);
        assertEquals(10L, saved.getRoomId());
        assertNull("刚录入的读数不应关联账单", saved.getBillId());
        // 120.5 * 0.6 + 8.0 * 3.5 = 72.30 + 28.00
        assertEquals(0, new BigDecimal("100.30").compareTo(saved.getTotalAmount()));

        DormPage<MeterReadingDto> page = service.meterReadings(manager, DormPageQuery.all());
        assertEquals(1L, page.getTotalElements());
    }

    @Test
    public void sameRoomAndPeriodUpdatesInsteadOfDuplicating() {
        long first = service.saveMeterReading(manager, reading("100", "5")).getId();
        MeterReadingDto second = service.saveMeterReading(manager, reading("180", "9"));

        assertEquals("同房间同账期必须更新而不是新增", first, second.getId());
        assertEquals(1L, service.meterReadings(manager, DormPageQuery.all()).getTotalElements());
        assertEquals(0, new BigDecimal("180").compareTo(second.getElectricityUnits()));
    }

    @Test
    public void studentCannotTouchMeterReadings() {
        try {
            service.meterReadings(student, DormPageQuery.all());
            fail("学生没有宿舍治理权限");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void periodEndBeforeStartIsRejected() {
        MeterReadingRequest bad = new MeterReadingRequest(10L, PERIOD_END, PERIOD_START,
                new BigDecimal("10"), new BigDecimal("1"),
                new BigDecimal("0.6"), new BigDecimal("3.5"));
        try {
            service.saveMeterReading(manager, bad);
            fail("倒置的账期应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void unknownRoomIsRejected() {
        MeterReadingRequest bad = new MeterReadingRequest(999L, PERIOD_START, PERIOD_END,
                new BigDecimal("10"), new BigDecimal("1"),
                new BigDecimal("0.6"), new BigDecimal("3.5"));
        try {
            service.saveMeterReading(manager, bad);
            fail("不存在的房间应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.ROOM_NOT_FOUND, ex.getResultCode());
        }
    }

    // ---------- P1：账单生成与分摊 ----------

    @Test
    public void generateSplitsBillEvenlyAmongResidents() {
        service.saveMeterReading(manager, reading("100", "10"));   // 60.00 + 35.00 = 95.00
        BillGenerateResultDto result = service.generateBills(manager, generate(null));

        assertEquals(1, result.getBillCount());
        assertEquals(2, result.getAllocationCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, new BigDecimal("95.00").compareTo(result.getTotalAmount()));
        assertEquals(1, repository.billCount());
    }

    @Test
    public void indivisibleAmountKeepsSumExact() {
        repository.setResidents(10L, Long.valueOf(11L), Long.valueOf(12L), Long.valueOf(13L));
        service.saveMeterReading(manager, reading("100", "1"));    // 60.00 + 3.50 = 63.50
        service.generateBills(manager, generate(null));

        List<BigDecimal> shares = repository.allocationsOf(1000L);
        assertEquals(3, shares.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal share : shares) {
            sum = sum.add(share);
            assertTrue("每份分摊都必须大于零", share.signum() > 0);
        }
        assertEquals("分摊合计必须精确等于房间总额",
                0, new BigDecimal("63.50").compareTo(sum));
    }

    @Test
    public void generationLocksTheReading() {
        service.saveMeterReading(manager, reading("100", "10"));
        service.generateBills(manager, generate(null));
        try {
            service.saveMeterReading(manager, reading("200", "20"));
            fail("已出账的读数不应允许修改");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.METER_LOCKED, ex.getResultCode());
        }
    }

    @Test
    public void generatingTwiceFindsNothingPending() {
        service.saveMeterReading(manager, reading("100", "10"));
        service.generateBills(manager, generate(null));
        try {
            service.generateBills(manager, generate(null));
            fail("读数已锁定，第二次出账应当无事可做");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.NO_PENDING_READING, ex.getResultCode());
        }
    }

    @Test
    public void roomWithoutResidentsIsSkipped() {
        repository.addRoom(20L, "D1", "102");
        MeterReadingRequest empty = new MeterReadingRequest(20L, PERIOD_START, PERIOD_END,
                new BigDecimal("50"), new BigDecimal("5"),
                new BigDecimal("0.6"), new BigDecimal("3.5"));
        service.saveMeterReading(manager, empty);
        service.saveMeterReading(manager, reading("100", "10"));

        BillGenerateResultDto result = service.generateBills(manager, generate(null));
        assertEquals("只有有人住的房间出账", 1, result.getBillCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(2, result.getNotes().size());
    }

    @Test
    public void studentCannotGenerateBills() {
        try {
            service.generateBills(student, generate(null));
            fail("学生不能触发出账");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void generateWithoutPendingReadingIsRejected() {
        try {
            service.generateBills(manager, generate(null));
            fail("没有待出账读数时应当明确报错");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.NO_PENDING_READING, ex.getResultCode());
        }
    }

    // ---------- 分摊算法 ----------

    @Test
    public void splitDistributesRemainderToLeadingShares() {
        List<BigDecimal> shares = DormBillSplit.split(new BigDecimal("10.00"), 3);
        assertEquals(3, shares.size());
        assertEquals(0, new BigDecimal("3.34").compareTo(shares.get(0)));
        assertEquals(0, new BigDecimal("3.33").compareTo(shares.get(1)));
        assertEquals(0, new BigDecimal("3.33").compareTo(shares.get(2)));
    }

    @Test
    public void amountTooSmallCannotBeSplit() {
        assertFalse(DormBillSplit.isSplittable(new BigDecimal("0.03"), 4));
        assertTrue(DormBillSplit.isSplittable(new BigDecimal("0.04"), 4));
        assertFalse(DormBillSplit.isSplittable(BigDecimal.ZERO, 1));
    }

    // ---------- 路由与状态 ----------

    /**
     * 每加一条命令都改一次断言里的数字，改漏了就变成「测试提醒你改测试」，
     * 而不是「测试替你发现忘了注册」。这里改成反射数 DormExtCommands 里所有
     * dorm.ext.* 常量，注册表漏掉任何一条都会被抓到，加命令时也不用动这个测试。
     */
    @Test
    public void registryRegistersEveryExtensionCommand() {
        Set<String> declared = declaredCommands();
        assertTrue("DormExtCommands 里应当有命令常量", declared.size() >= 27);
        CommandRouter router = new CommandRouter(new SessionManager());
        DormExtCommandRegistry.registerAll(router, service);
        assertEquals("注册表与命令常量必须一一对应，漏注册或重复注册都会在这里暴露",
                declared.size(), router.registeredCommandCount());
    }

    /** DormExtCommands 里所有取值以 dorm.ext. 开头的公开常量，即本模块的全部命令字。 */
    private static Set<String> declaredCommands() {
        Set<String> values = new HashSet<String>();
        for (Field field : DormExtCommands.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (value instanceof String && ((String) value).startsWith("dorm.ext.")) {
                    values.add((String) value);
                }
            } catch (IllegalAccessException ex) {
                throw new AssertionError("无法读取 " + field.getName() + ": " + ex);
            }
        }
        return values;
    }

    @Test
    public void statusReportsModuleVersion() {
        DormExtStatusDto status = service.status(manager);
        assertEquals(DormExtService.MODULE_VERSION, status.getModuleVersion());
        assertFalse("未启动 DormScheduler 时调度器应为未运行", status.isSchedulerRunning());
        assertNotNull(status.getServerTime());
    }

    @Test
    public void unknownCommandIsRejectedByRouter() {
        CommandRouter router = new CommandRouter(new SessionManager());
        DormExtCommandRegistry.registerAll(router, service);
        Message response = router.route(Message.request("dorm.ext.not.exist", null, null));
        assertNotNull(response);
    }

    private static BillGenerateRequest generate(Long roomId) {
        return new BillGenerateRequest(roomId, PERIOD_START, PERIOD_END, null);
    }

    private static MeterReadingRequest reading(String electricity, String water) {
        return new MeterReadingRequest(10L, PERIOD_START, PERIOD_END,
                new BigDecimal(electricity), new BigDecimal(water),
                new BigDecimal("0.6"), new BigDecimal("3.5"));
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
