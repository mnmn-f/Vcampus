package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 周卫生任务生成的覆盖、幂等、排序与权限。 */
public final class DormHygieneTaskTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101");
        repository.addRoom(20L, "D1", "102"); service = new DormExtService(repository);
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
        student = DormExtTestSupport.session(11L, Role.STUDENT);
    }

    @Test public void weeklyGenerationCoversEveryRoomAndIsIdempotent() {
        HygieneTaskGenerateResultDto first = generate();
        assertEquals(2, first.getRoomsScanned()); assertEquals(2, first.getCreated()); assertEquals(0, first.getExisting());
        HygieneTaskGenerateResultDto again = generate();
        assertEquals(0, again.getCreated()); assertEquals(2, again.getExisting());
        assertEquals(2L, service.hygieneTasks(manager, DormPageQuery.all()).getTotalElements());
    }

    @Test public void submittingInspectionClosesPendingWeeklyTask() {
        service.generateHygieneTasks(manager, new HygieneTaskGenerateRequest(null, LocalDate.now()));
        service.submitHygiene(manager, new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));
        DormPage<HygieneTaskDto> done = service.hygieneTasks(manager,
                new DormPageQuery(1, 20, null, HygieneTaskDto.STATUS_DONE, null, 10L));
        assertEquals(1L, done.getTotalElements());
    }

    @Test public void taskListPutsRecheckAndEarlierPendingFirst() {
        repository.addTask(1L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 10), HygieneTaskDto.STATUS_PENDING);
        repository.addTask(2L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 1), HygieneTaskDto.STATUS_DONE);
        repository.addTask(3L, 10L, HygieneTaskDto.TYPE_RECHECK, LocalDate.of(2026, 9, 12), HygieneTaskDto.STATUS_PENDING);
        repository.addTask(4L, 10L, HygieneTaskDto.TYPE_WEEKLY, LocalDate.of(2026, 9, 3), HygieneTaskDto.STATUS_PENDING);
        List<HygieneTaskDto> rows = service.hygieneTasks(manager, DormPageQuery.all()).getItems();
        assertEquals(4, rows.size()); assertEquals(3L, rows.get(0).getId()); assertEquals(4L, rows.get(1).getId());
        assertEquals(1L, rows.get(2).getId()); assertEquals(2L, rows.get(3).getId());
    }

    @Test public void studentCannotSubmitOrReadTasks() {
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.submitHygiene(student,
                    new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null)); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.hygieneTasks(student, DormPageQuery.all()); }
        });
    }

    private HygieneTaskGenerateResultDto generate() {
        return service.generateHygieneTasks(manager, new HygieneTaskGenerateRequest(null, LocalDate.of(2026, 9, 7)));
    }
    private static List<HygieneItemScoreDto> items(int floor, int desk, int bed, int bathroom, int balcony) {
        int[] scores = {floor, desk, bed, bathroom, balcony}; List<HygieneItemScoreDto> value = new ArrayList<HygieneItemScoreDto>();
        for (int i = 0; i < HygieneItemScoreDto.ITEM_CODES.length; i++) {
            value.add(new HygieneItemScoreDto(HygieneItemScoreDto.ITEM_CODES[i], BigDecimal.valueOf(scores[i]), null));
        }
        return value;
    }
}
