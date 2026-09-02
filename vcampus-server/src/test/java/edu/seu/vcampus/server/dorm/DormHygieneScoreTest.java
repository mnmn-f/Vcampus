package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 卫生检查分项校验、总分定级、整改记录与详情。 */
public final class DormHygieneScoreTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101");
        repository.addRoom(20L, "D1", "102"); service = new DormExtService(repository);
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }

    @Test public void totalAndLevelFollowDocumentedRules() {
        assertEquals(0, new BigDecimal("86").compareTo(DormHygieneRules.total(items(18, 17, 18, 16, 17))));
        assertEquals(HygieneDetailDto.LEVEL_EXCELLENT, DormHygieneRules.level(new BigDecimal("90")));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, DormHygieneRules.level(new BigDecimal("89")));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, DormHygieneRules.level(new BigDecimal("80")));
        assertEquals(HygieneDetailDto.LEVEL_PASS, DormHygieneRules.level(new BigDecimal("60")));
        assertEquals(HygieneDetailDto.LEVEL_POOR, DormHygieneRules.level(new BigDecimal("59")));
        assertTrue(DormHygieneRules.needRectify(new BigDecimal("69")));
        assertFalse(DormHygieneRules.needRectify(new BigDecimal("70")));
    }

    @Test public void missingDuplicateAndOutOfRangeItemsAreRejected() {
        List<HygieneItemScoreDto> missing = items(18, 17, 18, 16, 17); missing.remove(0); assertInvalid(missing);
        List<HygieneItemScoreDto> duplicate = items(18, 17, 18, 16, 17);
        duplicate.set(1, new HygieneItemScoreDto(HygieneItemScoreDto.FLOOR, new BigDecimal("20"), null));
        assertInvalid(duplicate);
        List<HygieneItemScoreDto> high = items(18, 17, 18, 16, 17);
        high.set(0, new HygieneItemScoreDto(HygieneItemScoreDto.FLOOR, new BigDecimal("21"), null));
        assertInvalid(high);
    }

    @Test public void passingInspectionComputesServerSideAndCreatesNoTask() {
        HygieneDetailDto saved = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(18, 17, 18, 16, 17), null));
        assertEquals(0, new BigDecimal("86").compareTo(saved.getTotalScore()));
        assertEquals(HygieneDetailDto.LEVEL_GOOD, saved.getScoreLevel()); assertFalse(saved.isNeedRectify());
        assertNull(saved.getRecheckDate()); assertEquals(5, saved.getItems().size());
        assertEquals(0L, service.hygieneTasks(manager, DormPageQuery.all()).getTotalElements());
    }

    @Test public void failingInspectionCreatesRecheckTask() {
        HygieneDetailDto saved = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(10, 10, 12, 10, 10), null));
        assertEquals(0, new BigDecimal("52").compareTo(saved.getTotalScore()));
        assertEquals(HygieneDetailDto.LEVEL_POOR, saved.getScoreLevel()); assertTrue(saved.isNeedRectify());
        assertNotNull(saved.getRecheckDate());
        DormPage<HygieneTaskDto> tasks = service.hygieneTasks(manager, DormPageQuery.all());
        assertEquals(1L, tasks.getTotalElements()); assertTrue(tasks.getItems().get(0).isRecheck());
        assertEquals(Long.valueOf(saved.getInspectionId()), tasks.getItems().get(0).getSourceInspectionId());
    }

    @Test public void detailCarriesEveryItemAndUnknownIdFails() {
        long id = service.submitHygiene(manager,
                new HygieneScoreSubmitRequest(10L, items(20, 20, 20, 20, 20), "无问题")).getInspectionId();
        HygieneDetailDto detail = service.hygieneDetail(manager, new HygieneDetailRequest(id));
        assertEquals(5, detail.getItems().size()); assertEquals(HygieneDetailDto.LEVEL_EXCELLENT, detail.getScoreLevel());
        assertEquals("无问题", detail.getIssueDescription());
        DormExtTestSupport.assertCode(DormExtCommands.HYGIENE_NOT_FOUND, new DormExtTestSupport.Action() {
            public void run() { service.hygieneDetail(manager, new HygieneDetailRequest(999L)); }
        });
    }

    private void assertInvalid(List<HygieneItemScoreDto> scores) {
        DormExtTestSupport.assertCode(DormExtCommands.HYGIENE_ITEMS_INVALID, new DormExtTestSupport.Action() {
            public void run() { service.submitHygiene(manager, new HygieneScoreSubmitRequest(10L, scores, null)); }
        });
    }
    private static List<HygieneItemScoreDto> items(int floor, int desk, int bed, int bathroom, int balcony) {
        int[] scores = {floor, desk, bed, bathroom, balcony}; List<HygieneItemScoreDto> value = new ArrayList<HygieneItemScoreDto>();
        for (int i = 0; i < HygieneItemScoreDto.ITEM_CODES.length; i++) {
            value.add(new HygieneItemScoreDto(HygieneItemScoreDto.ITEM_CODES[i], BigDecimal.valueOf(scores[i]), null));
        }
        return value;
    }
}
