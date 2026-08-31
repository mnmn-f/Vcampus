package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 报修评价只允许本人对已完成工单进行一次提交。 */
public final class DormRepairEvaluationTest {
    private DormService service;
    private SessionContext student;
    private SessionContext other;
    private SessionContext manager;

    @Before
    public void setUp() {
        InMemoryDormRepository repository = new InMemoryDormRepository();
        LocalDateTime now = LocalDateTime.of(2026, 8, 29, 10, 0);
        repository.addRepair(new RepairOrderDto(1L, 10L, 11L, "LIGHT", "灯坏了", "NORMAL",
                "COMPLETED", 90L, now.minusDays(1), now.minusHours(12), now, null, null));
        repository.addRepair(new RepairOrderDto(2L, 10L, 11L, "DOOR", "门坏了", "NORMAL",
                "SUBMITTED", null, now, null, null, null, null));
        service = new DormService(repository);
        student = session(11L, Role.STUDENT);
        other = session(12L, Role.STUDENT);
        manager = session(90L, Role.DORM_MANAGER);
    }

    @Test
    public void ownerCanEvaluateCompletedOrderOnce() {
        RepairOrderDto value = service.evaluateRepair(student,
                new RepairEvaluationRequest(1L, 5, "处理及时"));
        assertEquals(Integer.valueOf(5), value.getEvaluationScore());
        assertEquals("处理及时", value.getEvaluationNote());
        try {
            service.evaluateRepair(student, new RepairEvaluationRequest(1L, 4, "覆盖"));
            fail("evaluation must be one shot");
        } catch (DormException ex) {
            assertEquals(DormCommands.REPAIR_INVALID_STATE, ex.getResultCode());
        }
    }

    @Test
    public void evaluationChecksOwnerStatusScoreAndRole() {
        try {
            service.evaluateRepair(other, new RepairEvaluationRequest(1L, 5, null));
            fail("other student must not evaluate");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.evaluateRepair(student, new RepairEvaluationRequest(2L, 5, null));
            fail("unfinished order must not evaluate");
        } catch (DormException ex) {
            assertEquals(DormCommands.REPAIR_INVALID_STATE, ex.getResultCode());
        }
        try {
            service.evaluateRepair(student, new RepairEvaluationRequest(1L, 6, null));
            fail("score outside range must fail");
        } catch (DormException ex) {
            assertEquals(DormCommands.INVALID_INPUT, ex.getResultCode());
        }
        try {
            service.evaluateRepair(manager, new RepairEvaluationRequest(1L, 5, null));
            fail("dorm manager must not evaluate for student");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("repair-" + id + role.name(), id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
