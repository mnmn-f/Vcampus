package edu.seu.vcampus.server.campus;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.campus.repository.InMemoryCampusRepository;
import edu.seu.vcampus.server.campus.service.CampusException;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/** 覆盖扩展模块的身份、状态、容量和教室冲突边界。 */
public class CampusServiceTest {
    private InMemoryCampusRepository repository;
    private CampusService service;
    private SessionContext student;
    private SessionContext otherStudent;
    private SessionContext academic;

    @Before public void setUp() {
        repository = new InMemoryCampusRepository();
        repository.addClassroom(new CampusClassroomDto(1L, "教学楼", "A101", "TEACHING", 50, "AVAILABLE"));
        service = new CampusService(repository);
        student = session(1L, Role.STUDENT);
        otherStudent = session(2L, Role.STUDENT);
        academic = session(9L, Role.ACADEMIC_ADMIN);
    }

    @Test public void competitionRegistrationChecksDuplicateAndCapacity() {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        CompetitionSaveRequest request = CompetitionSaveRequest.create("创新赛", "测试", start,
                start.plusHours(2), start.minusDays(1), Integer.valueOf(1), "PUBLISHED");
        final long id = service.saveCompetition(academic, request).getId();
        service.registerCompetition(student, id);
        assertEquals("REGISTERED", service.myCompetitionRegistrations(student,
                new edu.seu.vcampus.common.dto.campus.CampusPageQuery()).getItems().get(0).getStatus());
        assertCode(CampusCommands.COMPETITION_DUPLICATE, new Operation() {
            @Override public void run() { service.registerCompetition(student, id); }
        });
        assertCode(CampusCommands.COMPETITION_FULL, new Operation() {
            @Override public void run() { service.registerCompetition(otherStudent, id); }
        });
        service.cancelCompetition(student, id);
        assertEquals("CANCELLED", service.myCompetitionRegistrations(student,
                new edu.seu.vcampus.common.dto.campus.CampusPageQuery()).getItems().get(0).getStatus());
    }

    @Test public void classroomApprovalRejectsSecondOverlappingApprovedRequest() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        long first = service.applyClassroom(student, new ClassroomReservationRequest(1L, "答辩", start, start.plusHours(1))).getId();
        final long second = service.applyClassroom(otherStudent, new ClassroomReservationRequest(1L, "答辩", start, start.plusHours(1))).getId();
        service.reviewClassroom(academic, new ClassroomReviewRequest(first, true, "通过"));
        assertCode(CampusCommands.CLASSROOM_CONFLICT, new Operation() {
            @Override public void run() { service.reviewClassroom(academic, new ClassroomReviewRequest(second, true, "通过")); }
        });
    }

    @Test public void studentSrtpIdentityComesFromSession() {
        SrtpSaveRequest request = new SrtpSaveRequest(null, "P-1", Long.valueOf(999L), "项目", "说明", BigDecimal.ONE, "SUBMITTED");
        assertEquals(1L, service.saveSrtp(student, request).getStudentUserId());
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override public void run() { service.reviewSrtp(student, new SrtpStatusRequest(1L, "APPROVED", "越权")); }
        });
    }

    @Test public void announcementVisibilityAndOwnedModuleAreEnforced() {
        LocalDateTime now = LocalDateTime.now();
        service.saveAnnouncement(academic, CampusAnnouncementSaveRequest.create("ACADEMIC", "公告", "内容", "ALL", null, "PUBLISHED", now.minusMinutes(1), null));
        assertEquals(1L, service.announcements(student, (edu.seu.vcampus.common.dto.campus.CampusPageQuery) null).getTotalElements());
        assertCode(CampusCommands.ANNOUNCEMENT_VISIBILITY, new Operation() {
            @Override public void run() { service.saveAnnouncement(academic, CampusAnnouncementSaveRequest.create("DORM", "越权", "内容", "ALL", null, "DRAFT", null, null)); }
        });
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id + role.name(), id, "user" + id, "用户" + id,
                EnumSet.of(role), role);
    }

    private static void assertCode(String expected, Operation operation) {
        try { operation.run(); } catch (CampusException ex) { assertEquals(expected, ex.getResultCode()); return; }
        throw new AssertionError("expected error code " + expected);
    }

    private interface Operation { void run(); }
}
