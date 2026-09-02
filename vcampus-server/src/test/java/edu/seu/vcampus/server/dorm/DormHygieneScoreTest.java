package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.threeten.bp.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 卫生检查：分项校验、总分定级、整改与复查任务、周检查清单幂等。 */
public final class DormHygieneScoreTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addRoom(20L, "D1", "102");
        service = new DormExtService(repository);
        manager = session(90L, Role.DORM_MANAGER);
        student = session(11L, Role.STUDENT);
    }

    // ---------- 判定规则（纯函数） ----------

    @Test
    public void totalIsSumOfFiveItems() {
        assertEquals(0, new BigDecimal("86").compareTo(
                DormHygieneRules.total(items(18, 17, 18, 16, 17))));
    }

    @Test
    public void levelFollowsScoreBands() {
        assertEquals(HygieneDetailDto.LEVEL_EXCELLENT, DormHygieneRules.level(new BigDecimal("90")));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, DormHygieneRules.level(new BigDecimal("89")));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, DormHygieneRules.level(new BigDecimal("80")));
        assertEquals(HygieneDetailDto.LEVEL_PASS, DormHygieneRules.level(new BigDecimal("60")));
        assertEquals(HygieneDetailDto.LEVEL_POOR, DormHygieneRules.level(new BigDecimal("59")));
    }

    @Test
    public void rectifyThresholdIsSeventy() {
        assertTrue(DormHygieneRules.needRectify(new BigDecimal("69")));
        assertFalse(DormHygieneRules.needRectify(new BigDecimal("70")));
    }

    @Test
    public void missingItemIsRejected() {
        List<HygieneItemScoreDto> incomplete = items(18, 17, 18, 16, 17);
        incomplete.remove(0);
        assertInvalid(incomplete);
    }

    @Test
    public void duplicateItemIsRejected() {
        List<HygieneItemScoreDto> duplicated = items(18, 17, 18, 16, 17);
        duplicated.set(1, new HygieneItemScoreDto(HygieneItemScoreDto.FLOOR,
                new BigDecimal("20"), null));
        assertInvalid(duplicated);
    }

    @Test
    public void outOfRangeItemScoreIsRejected() {
        List<HygieneItemScoreDto> tooHigh = items(18, 17, 18, 16, 17);
        tooHigh.set(0, new HygieneItemScoreDto(HygieneItemScoreDto.FLOOR,
                new BigDecimal("21"), null));
        assertInvalid(tooHigh);
    }

    // ---------- 提交检查 ----------

    @Test
    public void submitComputesTotalAndLevelOnServer() {
        HygieneDetailDto saved = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));
        assertEquals(0, new BigDecimal("86").compareTo(saved.getTotalScore()));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, saved.getScoreLevel());
        assertFalse(saved.isNeedRectify());
        assertNull(saved.getRecheckDate());
        assertEquals(5, saved.getItems().size());
    }

    @Test
    public void failingInspectionCreatesRecheckTask() {
        HygieneDetailDto saved = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(10, 10, 12, 10, 10), null));
        assertEquals(0, new BigDecimal("52").compareTo(saved.getTotalScore()));
        assertEquals(HygieneDetailDto.LEVEL_POOR, saved.getScoreLevel());
        assertTrue(saved.isNeedRectify());
        assertNotNull("需要整改时必须给出复查日期", saved.getRecheckDate());

        DormPage<HygieneTaskDto> tasks = service.hygieneTasks(manager, DormPageQuery.all());
        assertEquals(1L, tasks.getTotalElements());
        assertTrue(tasks.getItems().get(0).isRecheck());
        assertEquals(Long.valueOf(saved.getInspectionId()),
                tasks.getItems().get(0).getSourceInspectionId());
    }

    @Test
    public void passingInspectionCreatesNoTask() {
        service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));
        assertEquals(0L, service.hygieneTasks(manager, DormPageQuery.all()).getTotalElements());
    }

    @Test
    public void submittingClosesPendingWeeklyTask() {
        service.generateHygieneTasks(manager,
                new HygieneTaskGenerateRequest(null, LocalDate.now()));
        service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));

        DormPage<HygieneTaskDto> done = service.hygieneTasks(manager,
                new DormPageQuery(1, 20, null, HygieneTaskDto.STATUS_DONE, null, 10L));
        assertEquals("提交检查后该房间的待办任务应当收口", 1L, done.getTotalElements());
    }

    @Test
    public void detailCarriesEveryItem() {
        long id = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(20, 20, 20, 20, 20), "无问题"))
                .getInspectionId();
        HygieneDetailDto detail = service.hygieneDetail(manager, new HygieneDetailRequest(id));
        assertEquals(5, detail.getItems().size());
        assertEquals(HygieneDetailDto.LEVEL_EXCELLENT, detail.getScoreLevel());
        assertEquals("无问题", detail.getIssueDescription());
    }

    @Test
    public void unknownInspectionIsRejected() {
        try {
            service.hygieneDetail(manager, new HygieneDetailRequest(999L));
            fail("不存在的检查记录应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.HYGIENE_NOT_FOUND, ex.getResultCode());
        }
    }

    // ---------- 周检查任务 ----------

    @Test
    public void weeklyGenerationCoversEveryRoom() {
        HygieneTaskGenerateResultDto result = service.generateHygieneTasks(manager,
                new HygieneTaskGenerateRequest(null, LocalDate.of(2026, 9, 7)));
        assertEquals(2, result.getRoomsScanned());
        assertEquals(2, result.getCreated());
        assertEquals(0, result.getExisting());
    }

    @Test
    public void weeklyGenerationIsIdempotent() {
        service.generateHygieneTasks(manager,
                new HygieneTaskGenerateRequest(null, LocalDate.of(2026, 9, 7)));
        HygieneTaskGenerateResultDto again = service.generateHygieneTasks(manager,
                new HygieneTaskGenerateRequest(null, LocalDate.of(2026, 9, 7)));
        assertEquals("重复生成不应产生第二批任务", 0, again.getCreated());
        assertEquals(2, again.getExisting());
        assertEquals(2L, service.hygieneTasks(manager, DormPageQuery.all()).getTotalElements());
    }

    // ---------- 权限 ----------

    @Test
    public void studentCannotSubmitOrRead() {
        try {
            service.submitHygiene(student,
                    new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));
            fail("学生没有宿舍治理权限");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.hygieneTasks(student, DormPageQuery.all());
            fail("学生不能查看检查任务");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private void assertInvalid(List<HygieneItemScoreDto> items) {
        try {
            service.submitHygiene(manager, new HygieneScoreSubmitRequest(10L, items, null));
            fail("非法的分项组合应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.HYGIENE_ITEMS_INVALID, ex.getResultCode());
        }
    }

    private static List<HygieneItemScoreDto> items(int floor, int desk, int bed,
                                                   int bathroom, int balcony) {
        int[] scores = { floor, desk, bed, bathroom, balcony };
        List<HygieneItemScoreDto> value = new ArrayList<HygieneItemScoreDto>();
        for (int i = 0; i < HygieneItemScoreDto.ITEM_CODES.length; i++) {
            value.add(new HygieneItemScoreDto(HygieneItemScoreDto.ITEM_CODES[i],
                    BigDecimal.valueOf(scores[i]), null));
        }
        return value;
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }

    @Test
    public void taskListIsOrderedAsAToDoList() {
        // 表格顺序就是待办顺序：待检查在最前，其中复查优先于周检查，
        // 同组按计划日期从早到晚；已完成的沉到最后。
        repository.addTask(1L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 10),
                HygieneTaskDto.STATUS_PENDING);
        repository.addTask(2L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 1),
                HygieneTaskDto.STATUS_DONE);
        repository.addTask(3L, 10L, HygieneTaskDto.TYPE_RECHECK, LocalDate.of(2026, 9, 12),
                HygieneTaskDto.STATUS_PENDING);
        repository.addTask(4L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 3),
                HygieneTaskDto.STATUS_PENDING);

        List<HygieneTaskDto> rows = service.hygieneTasks(manager, DormPageQuery.all()).getItems();
        assertEquals(4, rows.size());
        assertEquals("复查最急，排最前", 3L, rows.get(0).getId());
        assertEquals("待检查里日期早的在前", 4L, rows.get(1).getId());
        assertEquals(1L, rows.get(2).getId());
        assertEquals("已完成沉底", 2L, rows.get(3).getId());
    }
}
