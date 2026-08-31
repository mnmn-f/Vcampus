package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordException;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import org.junit.Before;
import org.junit.Test;

import org.threeten.bp.LocalDate;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 学籍本人读取、分页过滤、唯一学号和管理员权限测试。 */
public class StudentProfileServiceTest {
    private InMemoryStudentRecordRepository repository;
    private StudentRecordService service;
    private SessionContext student;
    private SessionContext registrar;

    @Before
    public void setUp() {
        repository = new InMemoryStudentRecordRepository();
        repository.addUser(1L, "student1", "学生一");
        repository.addUser(2L, "student2", "学生二");
        repository.addUser(9L, "registrar", "学籍管理员");
        service = new StudentRecordService(repository, immediateTransactions());
        student = session(1L, Role.STUDENT);
        registrar = session(9L, Role.REGISTRAR);
    }

    @Test
    public void studentReadsOnlyTheProfileFromItsSession() throws Exception {
        service.createProfile(registrar, request(1L, "S001", "软件工程", "软工2601"));
        service.createProfile(registrar, request(2L, "S002", "计算机", "计科2601"));

        StudentProfileDto result = service.getOwnProfile(student);

        assertEquals(1L, result.getUserId());
        assertEquals("S001", result.getStudentNo());
    }

    @Test
    public void registrarSearchSupportsFiltersAndPagination() throws Exception {
        service.createProfile(registrar, request(1L, "S001", "软件工程", "软工2601"));
        service.createProfile(registrar, request(2L, "S002", "软件工程", "软工2602"));

        StudentProfilePage result = service.searchProfiles(registrar,
                new StudentProfileQuery(null, null, "软件工程", null, null,
                        StudentStatus.ENROLLED, 2, 1));

        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("S002", result.getItems().get(0).getStudentNo());
        assertTrue(result.hasNext() == false);
    }

    @Test
    public void duplicateStudentNumberIsRejected() throws Exception {
        service.createProfile(registrar, request(1L, "S001", "软件工程", "软工2601"));
        try {
            service.createProfile(registrar, request(2L, "S001", "计算机", "计科2601"));
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
            return;
        }
        throw new AssertionError("duplicate student number should fail");
    }

    @Test
    public void registrarCanUpdateAndReadProfileDetail() throws Exception {
        service.createProfile(registrar, request(1L, "S001", "软件工程", "软工2601"));

        StudentProfileDto updated = service.updateProfile(registrar,
                request(1L, "S001-R", "计算机科学", "计科2601"));

        assertEquals("S001-R", updated.getStudentNo());
        assertEquals("计算机科学", updated.getCollege());
        assertEquals("S001-R", service.getProfileDetail(registrar, 1L)
                .getProfile().getStudentNo());
    }

    @Test
    public void nonRegistrarCannotSearchAllProfiles() throws Exception {
        try {
            service.searchProfiles(student, StudentProfileQuery.firstPage());
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("student should not search all profiles");
    }

    @Test
    public void onlyActiveRoleContributesToAuthorization() throws Exception {
        service.createProfile(registrar, request(1L, "S001", "软件工程", "软工2601"));
        SessionContext librarianWorkstation = new SessionContext("multi-token", 1L,
                "student1", "学生一", EnumSet.of(Role.STUDENT, Role.LIBRARIAN),
                Role.LIBRARIAN);
        try {
            service.getOwnProfile(librarianWorkstation);
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("inactive student role must not grant profile access");
    }

    @Test
    public void invalidYearsAndFutureBirthDateAreRejected() throws Exception {
        try {
            service.createProfile(registrar, new StudentProfileWriteRequest(
                    1L, "S001", "软件工程", null, null, 2030, 2029,
                    null, null, LocalDate.now().plusDays(1), null, null, null,
                    StudentStatus.ENROLLED));
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.INVALID_INPUT, ex.getResultCode());
            assertNotNull(ex.getUserMessage());
            return;
        }
        throw new AssertionError("invalid profile should fail");
    }

    private static StudentProfileWriteRequest request(long id, String no,
                                                      String college, String clazz) {
        return new StudentProfileWriteRequest(id, no, college, "软件工程", clazz,
                2026, 2030, "UNDERGRADUATE", "UNKNOWN", null, null, null, null,
                StudentStatus.ENROLLED);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id + "-" + role.name(), id,
                "user" + id, "用户" + id, EnumSet.of(role), role);
    }

    private static StudentTransactionRunner immediateTransactions() {
        return new StudentTransactionRunner() {
            @Override
            public <T> T execute(TransactionWork<T> work) throws Exception {
                return work.execute(null);
            }
        };
    }
}
