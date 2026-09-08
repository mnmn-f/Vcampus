package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 外来人员登记的输入、住宿前置条件与证件脱敏函数。 */
public final class DormVisitorValidationTest {
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 9, 10, 9, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 10, 18, 0);
    private DormExtService service;
    private SessionContext student;

    @Before public void setUp() {
        InMemoryDormExtRepository r = new InMemoryDormExtRepository(); r.addRoom(10L, "D1", "101");
        r.addAccommodation(11L, 10L); service = new DormExtService(r);
        student = DormExtTestSupport.session(11L, Role.STUDENT);
    }
    @Test public void noAccommodationIsRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.NO_ACCOMMODATION, new DormExtTestSupport.Action() {
            public void run() { service.submitVisitor(DormExtTestSupport.session(77L, Role.STUDENT), request("张三")); }
        });
    }
    @Test public void invalidTimeAndBlankNameAreRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.INVALID_INPUT, new DormExtTestSupport.Action() {
            public void run() { service.submitVisitor(student, new VisitorRegistrationRequest("张三",
                    "320123456789012312", null, "探亲", TO, FROM)); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.INVALID_INPUT, new DormExtTestSupport.Action() {
            public void run() { service.submitVisitor(student, new VisitorRegistrationRequest("  ",
                    "320123456789012312", null, "探亲", FROM, TO)); }
        });
    }
    @Test public void maskKeepsHeadAndTailOnly() {
        assertEquals("123*******89", VisitorRegistrationDto.mask("123456789789"));
        assertEquals("****", VisitorRegistrationDto.mask("1234")); assertNull(VisitorRegistrationDto.mask(null));
    }
    @Test public void unknownRegistrationIsRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.VISITOR_NOT_FOUND, new DormExtTestSupport.Action() {
            public void run() { service.auditVisitor(DormExtTestSupport.session(90L, Role.DORM_MANAGER),
                    new edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest(999L, true, null)); }
        });
    }
    private static VisitorRegistrationRequest request(String name) {
        return new VisitorRegistrationRequest(name, "320123456789012312", "13800000000", "探亲", FROM, TO);
    }
}
