package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormStayRules;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 在宿状态、晚归规则、门禁查询与策略配置。 */
public final class DormStayStatusTest {
    private static final LocalDateTime EXIT = LocalDateTime.of(2026, 9, 1, 20, 0);
    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 9, 1, 22, 0);
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext student;
    private SessionContext manager;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101"); repository.addRoom(20L, "D1", "102");
        repository.addResident(11L, 10L, EXIT, ENTRY); service = new DormExtService(repository);
        student = DormExtTestSupport.session(11L, Role.STUDENT); manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }
    @Test public void stayRulesClassifyReturnLeaveAndNoHistory() {
        assertEquals(StayStatusDto.IN_DORM, DormStayRules.status(EXIT, ENTRY, false));
        assertEquals(StayStatusDto.OUT, DormStayRules.status(EXIT, null, false));
        assertEquals(StayStatusDto.LEAVE_REGISTERED, DormStayRules.status(EXIT, null, true));
        assertEquals(StayStatusDto.IN_DORM, DormStayRules.status(null, null, false));
        assertEquals(StayStatusDto.IN_DORM, service.myStayStatus(student).getStatus());
        assertEquals("D1", service.myStayStatus(student).getBuildingCode()); assertEquals("101", service.myStayStatus(student).getRoomNo());
    }
    @Test public void leaveAndMissingAccommodationAreHandled() {
        repository.addApprovedLeave(11L, LocalDate.now());
        assertEquals(StayStatusDto.LEAVE_REGISTERED, service.myStayStatus(student).getStatus());
        DormExtTestSupport.assertCode(DormExtCommands.NO_ACCOMMODATION, new DormExtTestSupport.Action() {
            public void run() { service.myStayStatus(DormExtTestSupport.session(77L, Role.STUDENT)); }
        });
        assertEquals(1L, service.stayStatuses(manager).getTotalElements());
    }
    @Test public void lateReturnRulesRespectNightWindow() {
        assertTrue(DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 1, 23, 30), LocalTime.of(23, 0), LocalTime.of(5, 0)));
        assertTrue(DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 2, 2, 0), LocalTime.of(23, 0), LocalTime.of(5, 0)));
        assertFalse(DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 1, 21, 0), LocalTime.of(23, 0), LocalTime.of(5, 0)));
        assertFalse(DormStayRules.isLateReturn("EXIT", LocalDateTime.of(2026, 9, 1, 23, 30), LocalTime.of(23, 0), LocalTime.of(5, 0)));
    }
    @Test public void accessListMarksLateReturnsAndFiltersType() {
        repository.addAccessRecord(1L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 1, 21, 0), "南门");
        repository.addAccessRecord(2L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 2, 23, 40), "南门");
        List<AccessRecordExtDto> rows = service.myAccessRecords(student, DormPageQuery.all()).getItems();
        assertEquals(2, rows.size()); assertFalse(rows.get(0).isLateReturn()); assertTrue(rows.get(1).isLateReturn());
        repository.addAccessRecord(3L, 11L, "EXIT", LocalDateTime.of(2026, 9, 2, 7, 0), "南门");
        assertEquals(2, service.myAccessRecords(student, new DormPageQuery(1, 20, null, "ENTRY", null, null)).getItems().size());
        assertEquals(1, service.myAccessRecords(student, new DormPageQuery(1, 20, null, "EXIT", null, null)).getItems().size());
    }
    @Test public void policyChangesHistoricalClassification() {
        repository.addAccessRecord(1L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 1, 22, 30), "南门");
        assertFalse(service.myAccessRecords(student, DormPageQuery.all()).getItems().get(0).isLateReturn());
        service.saveAccessPolicy(manager, new AccessPolicyRequest(LocalTime.of(22, 0), LocalTime.of(5, 0)));
        assertTrue(service.myAccessRecords(student, DormPageQuery.all()).getItems().get(0).isLateReturn());
    }
    @Test public void policyValidationAndDefaults() {
        DormExtTestSupport.assertCode(DormExtCommands.POLICY_INVALID, new DormExtTestSupport.Action() {
            public void run() { service.saveAccessPolicy(manager,
                    new AccessPolicyRequest(LocalTime.of(5, 0), LocalTime.of(23, 0))); }
        });
        AccessPolicyDto policy = service.accessPolicy(manager);
        assertEquals(LocalTime.of(23, 0), policy.getCurfewTime()); assertEquals(LocalTime.of(5, 0), policy.getDawnTime());
    }
}
