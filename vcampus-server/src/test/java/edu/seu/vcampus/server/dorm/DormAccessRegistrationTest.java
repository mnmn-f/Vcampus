package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormRepository;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import java.util.Collections;
import org.threeten.bp.LocalDateTime;
import org.junit.Test;
import static org.junit.Assert.*;

public class DormAccessRegistrationTest {
    @Test public void studentCannotForgeDoorTimestampOrHardwareSource() {
        DormService service = new DormService(new InMemoryDormRepository());
        SessionContext student = session(Role.STUDENT);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        AccessRecordDto record = service.recordAccess(student,
                new AccessRecordRequest("ENTRY", before.minusDays(20), "大门", "DEVICE", null));
        assertTrue(record.getOccurredAt().isAfter(before)); assertEquals("SELF", record.getSource());
        assertEquals(student.getUserId(), record.getStudentUserId());
    }
    @Test(expected = DormException.class) public void invalidDirectionIsRejectedBeforeRepository() {
        new DormService(new InMemoryDormRepository()).recordAccess(session(Role.STUDENT),
                new AccessRecordRequest("FAKE", null, null, null, null));
    }
    private static SessionContext session(Role role) {
        return new SessionManager().createSession(2L, "student", "测试", Collections.singleton(role), role);
    }
}
