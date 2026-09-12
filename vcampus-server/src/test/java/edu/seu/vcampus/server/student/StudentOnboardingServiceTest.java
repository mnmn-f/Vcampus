package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import edu.seu.vcampus.server.student.registry.StudentCommandRegistry;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** 待建档账号选择和服务端账号解析测试。 */
public final class StudentOnboardingServiceTest {
    @Test public void pendingAccountsExcludeProfilesAndFilterReadableFields() throws Exception {
        InMemoryStudentRecordRepository repository = repository();
        StudentRecordService service = service(repository);
        service.createProfile(registrar(), profile(1L, "S001"));
        StudentAccountCandidatePage all = service.searchPendingAccounts(registrar(),
                StudentAccountCandidateQuery.firstPage());
        assertEquals(2L, all.getTotal());
        assertEquals("student3", all.getItems().get(0).getAccount());
        assertFalse(all.getItems().get(0).toString().contains("用户编号"));
        StudentAccountCandidatePage filtered = service.searchPendingAccounts(registrar(),
                new StudentAccountCandidateQuery("学生二", 1, 100));
        assertEquals(1L, filtered.getTotal());
        assertEquals("student2", filtered.getItems().get(0).getAccount());
    }

    @Test public void createResolvesAccountAndDoesNotAcceptProfileAgain() throws Exception {
        InMemoryStudentRecordRepository repository = repository();
        StudentRecordService service = service(repository);
        StudentProfileDto result = service.createProfile(registrar(), create("student2", "S002"));
        assertEquals(2L, result.getUserId());
        assertEquals("student2", result.getAccount());
        try {
            service.createProfile(registrar(), create("student2", "S002-R"));
        } catch (edu.seu.vcampus.server.student.service.StudentRecordException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
            return;
        }
        throw new AssertionError("an account with a profile must not be selected again");
    }

    @Test public void profileCreateCommandRejectsLegacyInternalIdPayload() {
        InMemoryStudentRecordRepository repository = repository();
        StudentRecordService service = service(repository);
        SessionManager sessions = new SessionManager();
        SessionContext registrar = sessions.createSession(9L, "registrar", "管理员",
                EnumSet.of(Role.REGISTRAR));
        CommandRouter router = StudentCommandRegistry.registerAll(new CommandRouter(sessions), service);
        Message response = router.route(Message.request(StudentCommands.PROFILE_CREATE,
                registrar.getSessionToken(), profile(1L, "S001")));
        assertEquals(ResultCodes.INVALID_INPUT, response.getResultCode());
    }

    private static InMemoryStudentRecordRepository repository() {
        InMemoryStudentRecordRepository value = new InMemoryStudentRecordRepository();
        value.addUser(1L, "student1", "学生一"); value.addUser(2L, "student2", "学生二");
        value.addUser(3L, "student3", "学生三"); value.addStaffUser(9L, "registrar", "管理员");
        return value;
    }

    private static StudentRecordService service(InMemoryStudentRecordRepository repository) {
        return new StudentRecordService(repository, new StudentTransactionRunner() {
            @Override public <T> T execute(TransactionWork<T> work) throws Exception { return work.execute(null); }
        });
    }

    private static SessionContext registrar() {
        return new SessionContext("registrar-token", 9L, "registrar", "管理员",
                EnumSet.of(Role.REGISTRAR), Role.REGISTRAR);
    }

    private static StudentProfileCreateRequest create(String account, String no) {
        return new StudentProfileCreateRequest(account, no, "计算机科学与工程学院", "软件工程",
                "软工2601", 2026, 2030, "UNDERGRADUATE", "MALE", null, null, null, null,
                StudentStatus.ENROLLED);
    }

    private static StudentProfileWriteRequest profile(long id, String no) {
        return new StudentProfileWriteRequest(id, no, "软件工程", "软件工程", "软工2601",
                2026, 2030, "UNDERGRADUATE", "UNKNOWN", null, null, null, null,
                StudentStatus.ENROLLED);
    }
}
