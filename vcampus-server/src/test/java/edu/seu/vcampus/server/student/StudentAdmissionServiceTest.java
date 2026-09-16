package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentAccountProvisioner;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import org.junit.Test;
import java.sql.Connection;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 新生账号与学籍由一次服务调用完成。 */
public final class StudentAdmissionServiceTest {
    @Test public void registrarCreatesLoginAccountAndProfileTogether() throws Exception {
        final InMemoryStudentRecordRepository repository = new InMemoryStudentRecordRepository();
        repository.addStaffUser(9L, "registrar", "学籍管理员");
        final AtomicReference<StudentProfileCreateRequest> accountRequest = new AtomicReference<>();
        StudentAccountProvisioner accounts = new StudentAccountProvisioner() {
            @Override public long create(Connection connection, SessionContext operator,
                                         StudentProfileCreateRequest request) {
                accountRequest.set(request); repository.addUser(88L, request.getAccount(), request.getDisplayName());
                return 88L;
            }
        };
        StudentRecordService service = new StudentRecordService(repository, runner(), accounts);
        StudentProfileDto result = service.createProfile(registrar(), request());
        assertEquals(88L, result.getUserId());
        assertEquals("261234567", result.getAccount());
        assertEquals("261234567", result.getStudentNo());
        assertEquals("王晨茜", result.getDisplayName());
        assertTrue(accountRequest.get().isCreateAccount());
    }

    private static StudentProfileCreateRequest request() {
        return new StudentProfileCreateRequest("261234567", "王晨茜", "Vc123456A",
                "261234567", "计算机科学与工程学院", "计算机科学与技术",
                "计算机科学与技术2026级1班", 2026, 2030, "UNDERGRADUATE",
                "FEMALE", null, null, null, null, StudentStatus.ENROLLED);
    }
    private static StudentTransactionRunner runner() {
        return new StudentTransactionRunner() {
            @Override public <T> T execute(TransactionWork<T> work) throws Exception {
                return work.execute(null);
            }
        };
    }
    private static SessionContext registrar() {
        return new SessionContext("registrar", 9L, "registrar", "学籍管理员",
                EnumSet.of(Role.REGISTRAR), Role.REGISTRAR);
    }
}
