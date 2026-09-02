package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 外来人员登记：房间归属、证件号掩码、审核与撤销状态机、越权边界。 */
public final class DormVisitorRegistrationTest {
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 9, 10, 9, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 10, 18, 0);

    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext student;
    private SessionContext roommate;
    private SessionContext manager;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addAccommodation(11L, 10L);
        repository.addAccommodation(12L, 10L);
        service = new DormExtService(repository);
        student = session(11L, Role.STUDENT);
        roommate = session(12L, Role.STUDENT);
        manager = session(90L, Role.DORM_MANAGER);
    }

    @Test
    public void studentCanSubmitVisitorRegistration() {
        VisitorRegistrationDto saved = service.submitVisitor(student, request("张三"));
        assertNotNull(saved);
        assertEquals(11L, saved.getStudentUserId());
        assertEquals("D1", saved.getBuildingCode());
        assertEquals("101", saved.getRoomNo());
        assertTrue(saved.isPending());
        assertNull(saved.getAuditorId());
    }

    @Test
    public void roomComesFromAccommodationNotFromClient() {
        // 请求里根本没有房间字段，房间只能由服务端按在住记录解析。
        assertEquals(10L, service.submitVisitor(student, request("张三")).getRoomId());
    }

    @Test
    public void studentWithoutAccommodationIsRejected() {
        SessionContext homeless = session(77L, Role.STUDENT);
        try {
            service.submitVisitor(homeless, request("张三"));
            fail("没有住宿记录不应能登记来访人员");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.NO_ACCOMMODATION, ex.getResultCode());
        }
    }

    @Test
    public void identityCardIsMaskedEverywhere() {
        String masked = service.submitVisitor(student, request("张三")).getVisitorIdCardMasked();
        assertEquals("长度与原证件号一致，便于核对位数", 18, masked.length());
        assertTrue(masked.startsWith("320"));
        assertTrue(masked.endsWith("12"));
        assertFalse("中间各位必须被掩掉", masked.contains("456789"));
    }

    @Test
    public void maskKeepsHeadAndTailOnly() {
        assertEquals("123*******89", VisitorRegistrationDto.mask("123456789789"));
        assertEquals("****", VisitorRegistrationDto.mask("1234"));
        assertNull(VisitorRegistrationDto.mask(null));
    }

    @Test
    public void endBeforeStartIsRejected() {
        VisitorRegistrationRequest bad = new VisitorRegistrationRequest("张三",
                "320123456789012312", "13800000000", "探亲", TO, FROM);
        try {
            service.submitVisitor(student, bad);
            fail("离开时间早于来访时间应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void blankVisitorNameIsRejected() {
        VisitorRegistrationRequest bad = new VisitorRegistrationRequest("  ",
                "320123456789012312", null, "探亲", FROM, TO);
        try {
            service.submitVisitor(student, bad);
            fail("来访人姓名不能为空");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void studentOnlySeesOwnRegistrations() {
        service.submitVisitor(student, request("张三"));
        service.submitVisitor(roommate, request("李四"));
        assertEquals(1L, service.ownVisitors(student, DormPageQuery.all()).getTotalElements());
        assertEquals("宿管看得到全部", 2L,
                service.visitors(manager, DormPageQuery.all()).getTotalElements());
    }

    @Test
    public void managerApprovesRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        VisitorRegistrationDto audited = service.auditVisitor(manager,
                new VisitorAuditRequest(id, true, "已核对证件"));
        assertEquals(VisitorRegistrationDto.STATUS_APPROVED, audited.getAuditStatus());
        assertEquals(Long.valueOf(90L), audited.getAuditorId());
        assertNotNull(audited.getAuditedAt());
    }

    @Test
    public void auditingTwiceIsRejected() {
        long id = service.submitVisitor(student, request("张三")).getId();
        service.auditVisitor(manager, new VisitorAuditRequest(id, false, "证件不全"));
        try {
            service.auditVisitor(manager, new VisitorAuditRequest(id, true, null));
            fail("已处理的登记不应重复审核");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.VISITOR_INVALID_STATE, ex.getResultCode());
        }
    }

    @Test
    public void studentCancelsOwnPendingRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        VisitorRegistrationDto cancelled = service.cancelVisitor(student,
                new VisitorAuditRequest(id, false, "来访人临时有事"));
        assertEquals(VisitorRegistrationDto.STATUS_CANCELLED, cancelled.getAuditStatus());
        assertNull("撤销不是审核，不应留下审核人", cancelled.getAuditorId());
    }

    @Test
    public void studentCannotCancelOthersRegistration() {
        long id = service.submitVisitor(student, request("张三")).getId();
        try {
            service.cancelVisitor(roommate, new VisitorAuditRequest(id, false, null));
            fail("不能撤销别人提交的登记");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void approvedRegistrationCannotBeCancelled() {
        long id = service.submitVisitor(student, request("张三")).getId();
        service.auditVisitor(manager, new VisitorAuditRequest(id, true, null));
        try {
            service.cancelVisitor(student, new VisitorAuditRequest(id, false, null));
            fail("已审核的登记不应能撤销");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.VISITOR_INVALID_STATE, ex.getResultCode());
        }
    }

    @Test
    public void studentCannotAuditAndManagerCannotSubmit() {
        final long id = service.submitVisitor(student, request("张三")).getId();
        try {
            service.auditVisitor(student, new VisitorAuditRequest(id, true, null));
            fail("学生没有审批权限");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.visitors(student, DormPageQuery.all());
            fail("学生不能查看全量登记");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void auditingUnknownRegistrationIsRejected() {
        try {
            service.auditVisitor(manager, new VisitorAuditRequest(999L, true, null));
            fail("不存在的登记应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.VISITOR_NOT_FOUND, ex.getResultCode());
        }
    }

    private static VisitorRegistrationRequest request(String name) {
        return new VisitorRegistrationRequest(name, "320123456789012312",
                "13800000000", "探亲", FROM, TO);
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
