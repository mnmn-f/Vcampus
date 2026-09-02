package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 外来人员登记的提交、审核、撤销与权限边界。 */
public final class DormVisitorRegistrationTest {
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 9, 10, 9, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 10, 18, 0);
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext student;
    private SessionContext roommate;
    private SessionContext manager;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101");
        repository.addAccommodation(11L, 10L); repository.addAccommodation(12L, 10L);
        service = new DormExtService(repository); student = DormExtTestSupport.session(11L, Role.STUDENT);
        roommate = DormExtTestSupport.session(12L, Role.STUDENT); manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }

    @Test public void submitUsesAccommodationAndMasksIdentity() {
        VisitorRegistrationDto saved = service.submitVisitor(student, request("张三"));
        assertNotNull(saved); assertEquals(11L, saved.getStudentUserId()); assertEquals(10L, saved.getRoomId());
        assertEquals("D1", saved.getBuildingCode()); assertEquals("101", saved.getRoomNo());
        assertTrue(saved.isPending()); assertNull(saved.getAuditorId());
        assertEquals(18, saved.getVisitorIdCardMasked().length()); assertTrue(saved.getVisitorIdCardMasked().startsWith("320"));
        assertTrue(saved.getVisitorIdCardMasked().endsWith("12")); assertFalse(saved.getVisitorIdCardMasked().contains("456789"));
    }
    @Test public void studentOnlySeesOwnRegistrations() {
        service.submitVisitor(student, request("张三")); service.submitVisitor(roommate, request("李四"));
        assertEquals(1L, service.ownVisitors(student, DormPageQuery.all()).getTotalElements());
        assertEquals(2L, service.visitors(manager, DormPageQuery.all()).getTotalElements());
    }
    @Test public void managerApprovesRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        VisitorRegistrationDto row = service.auditVisitor(manager, new VisitorAuditRequest(id, true, "已核对证件"));
        assertEquals(VisitorRegistrationDto.STATUS_APPROVED, row.getAuditStatus());
        assertEquals(Long.valueOf(90L), row.getAuditorId()); assertNotNull(row.getAuditedAt());
    }
    @Test public void auditingTwiceIsRejected() {
        long id = service.submitVisitor(student, request("张三")).getId();
        service.auditVisitor(manager, new VisitorAuditRequest(id, false, "证件不全"));
        DormExtTestSupport.assertCode(DormExtCommands.VISITOR_INVALID_STATE, new DormExtTestSupport.Action() {
            public void run() { service.auditVisitor(manager, new VisitorAuditRequest(id, true, null)); }
        });
    }
    @Test public void studentCancelsOwnPendingRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        VisitorRegistrationDto row = service.cancelVisitor(student, new VisitorAuditRequest(id, false, "临时有事"));
        assertEquals(VisitorRegistrationDto.STATUS_CANCELLED, row.getAuditStatus()); assertNull(row.getAuditorId());
    }
    @Test public void studentCannotCancelOthersOrApprovedRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.cancelVisitor(roommate, new VisitorAuditRequest(id, false, null)); }
        });
        service.auditVisitor(manager, new VisitorAuditRequest(id, true, null));
        DormExtTestSupport.assertCode(DormExtCommands.VISITOR_INVALID_STATE, new DormExtTestSupport.Action() {
            public void run() { service.cancelVisitor(student, new VisitorAuditRequest(id, false, null)); }
        });
    }
    @Test public void studentCannotAuditOrUseManagementListing() {
        long id = service.submitVisitor(student, request("张三")).getId();
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.auditVisitor(student, new VisitorAuditRequest(id, true, null)); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.visitors(student, DormPageQuery.all()); }
        });
    }

    private static VisitorRegistrationRequest request(String name) {
        return new VisitorRegistrationRequest(name, "320123456789012312", "13800000000", "探亲", FROM, TO);
    }
}
