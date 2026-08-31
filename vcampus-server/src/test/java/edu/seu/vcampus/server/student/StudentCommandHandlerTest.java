package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import edu.seu.vcampus.server.student.registry.StudentCommandRegistry;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import edu.seu.vcampus.server.db.TransactionWork;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 命令注册、路由鉴权和自查命令不信任目标载荷测试。 */
public class StudentCommandHandlerTest {
    @Test
    public void selfProfileUsesSessionIdentityEvenIfPayloadContainsAnotherId() throws Exception {
        InMemoryStudentRecordRepository repository = new InMemoryStudentRecordRepository();
        repository.addUser(1L, "student1", "学生一");
        repository.addUser(2L, "student2", "学生二");
        StudentRecordService service = new StudentRecordService(repository, immediate());
        SessionManager sessions = new SessionManager();
        SessionContext registrar = sessions.createSession(9L, "registrar", "管理员",
                EnumSet.of(Role.REGISTRAR));
        repository.addUser(9L, "registrar", "管理员");
        service.createProfile(registrar, profile(1L, "S001"));
        service.createProfile(registrar, profile(2L, "S002"));
        SessionContext student = sessions.createSession(1L, "student1", "学生一",
                EnumSet.of(Role.STUDENT));
        CommandRouter router = StudentCommandRegistry.registerAll(
                new CommandRouter(sessions), service);

        Message response = router.route(Message.request(StudentCommands.SELF_PROFILE,
                student.getSessionToken(), Long.valueOf(2L)));

        assertTrue(response.isSuccess());
        assertEquals(1L, ((edu.seu.vcampus.common.dto.student.StudentProfileDto)
                response.getPayload()).getUserId());
    }

    @Test
    public void studentCannotUseRegistrarCommandThroughRouter() {
        InMemoryStudentRecordRepository repository = new InMemoryStudentRecordRepository();
        StudentRecordService service = new StudentRecordService(repository, immediate());
        SessionManager sessions = new SessionManager();
        SessionContext student = sessions.createSession(1L, "student1", "学生一",
                EnumSet.of(Role.STUDENT));
        CommandRouter router = StudentCommandRegistry.registerAll(
                new CommandRouter(sessions), service);

        Message response = router.route(Message.request(StudentCommands.PROFILE_SEARCH,
                student.getSessionToken(), null));

        assertEquals(ResultCodes.FORBIDDEN, response.getResultCode());
    }

    private static StudentProfileWriteRequest profile(long id, String no) {
        return new StudentProfileWriteRequest(id, no, "软件工程", "软件工程", "软工2601",
                2026, 2030, "UNDERGRADUATE", "UNKNOWN", null, null, null, null,
                StudentStatus.ENROLLED);
    }

    private static StudentTransactionRunner immediate() {
        return new StudentTransactionRunner() {
            @Override
            public <T> T execute(TransactionWork<T> work) throws Exception {
                return work.execute(null);
            }
        };
    }
}
