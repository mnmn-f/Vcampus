package edu.seu.vcampus.server.identity;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRepository;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.identity.service.IdentityServiceException;
import edu.seu.vcampus.server.identity.service.IdentityTransactionRunner;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 账号注销申请的权限、状态机、范围和提交后会话失效测试。 */
public final class AccountCancellationServiceTest {
    private InMemoryIdentityRepository repository;
    private PasswordHasher hasher;
    private SessionManager sessions;
    private IdentityService service;
    private SessionContext admin;
    private SessionContext student;
    private SessionContext teacher;

    @Before public void setUp() throws Exception {
        repository = new InMemoryIdentityRepository();
        hasher = new PasswordHasher(4);
        sessions = repository.getSessionManager();
        service = new IdentityService(repository, hasher, sessions,
                new edu.seu.vcampus.server.identity.service.InMemoryIdentityTransactionRunner(repository));
        repository.addUser(1L, "admin", hasher.hash("Admin1234"), "系统管理员", Role.SYSTEM_ADMIN);
        repository.addUser(2L, "student", hasher.hash("Student1234"), "学生", Role.STUDENT);
        repository.addUser(3L, "teacher", hasher.hash("Teacher1234"), "教师", Role.TEACHER);
        admin = login("admin", "Admin1234");
        student = login("student", "Student1234");
        teacher = login("teacher", "Teacher1234");
    }

    @Test public void ownerScopeDuplicateAndWithdrawAreEnforced() throws Exception {
        final AccountCancellationDto created = service.submitAccountCancellation(student,
                new AccountCancellationRequest("不再使用账号"));
        assertEquals("PENDING", created.getStatus());
        assertEquals(1L, service.listOwnAccountCancellations(student, null).getTotal());
        expect(ResultCodes.CONFLICT, new Action() {
            @Override public void run() throws Exception {
                service.submitAccountCancellation(student, new AccountCancellationRequest("再次申请"));
            }
        });
        expect(ResultCodes.FORBIDDEN, new Action() {
            @Override public void run() throws Exception {
                service.withdrawAccountCancellation(admin, created.getId());
            }
        });
        assertEquals("CANCELLED", service.withdrawAccountCancellation(student, created.getId()).getStatus());
        expect(ResultCodes.CONFLICT, new Action() {
            @Override public void run() throws Exception {
                service.withdrawAccountCancellation(student, created.getId());
            }
        });
    }

    @Test public void rejectionNeedsRemarkAndLeavesAccountAndSessionActive() throws Exception {
        final AccountCancellationDto created = service.submitAccountCancellation(student,
                new AccountCancellationRequest("申请注销"));
        expect(ResultCodes.INVALID_INPUT, new Action() {
            @Override public void run() throws Exception {
                service.rejectAccountCancellation(admin,
                        new AccountCancellationReviewRequest(created.getId(), " "));
            }
        });
        expect(ResultCodes.FORBIDDEN, new Action() {
            @Override public void run() throws Exception {
                service.approveAccountCancellation(teacher,
                        new AccountCancellationReviewRequest(created.getId(), "无权审批"));
            }
        });
        AccountCancellationDto rejected = service.rejectAccountCancellation(admin,
                new AccountCancellationReviewRequest(created.getId(), "资料不完整"));
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("ACTIVE", repository.getUser(student.getUserId()).getStatus());
        assertTrue(sessions.contains(student.getSessionToken()));
        expect(ResultCodes.CONFLICT, new Action() {
            @Override public void run() throws Exception {
                service.rejectAccountCancellation(admin,
                        new AccountCancellationReviewRequest(created.getId(), "重复处理"));
            }
        });
    }

    @Test public void approvalDisablesUserAndInvalidatesAllRuntimeSessionsAfterCommit() throws Exception {
        AccountCancellationDto created = service.submitAccountCancellation(student,
                new AccountCancellationRequest("账号注销"));
        SessionContext second = sessions.createSession(student.getUserId(), "student", "学生",
                java.util.Collections.singleton(Role.STUDENT));
        AccountCancellationDto approved = service.approveAccountCancellation(admin,
                new AccountCancellationReviewRequest(created.getId(), "确认注销"));
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("DISABLED", repository.getUser(student.getUserId()).getStatus());
        assertFalse(sessions.contains(student.getSessionToken()));
        assertFalse(sessions.contains(second.getSessionToken()));
        assertTrue(sessions.contains(admin.getSessionToken()));
    }

    @Test public void failedTransactionDoesNotDisableOrInvalidateSession() throws Exception {
        final AccountCancellationDto created = service.submitAccountCancellation(student,
                new AccountCancellationRequest("回滚探针"));
        final IdentityService failing = new IdentityService(repository, hasher, sessions,
                new IdentityTransactionRunner() {
                    @Override public <T> T execute(TransactionWork<T> work) throws Exception {
                        throw new SQLException("intentional cancellation rollback");
                    }
                });
        expect(ResultCodes.INTERNAL_ERROR, new Action() {
            @Override public void run() throws Exception {
                failing.approveAccountCancellation(admin,
                        new AccountCancellationReviewRequest(created.getId(), "不会提交"));
            }
        });
        assertEquals("ACTIVE", repository.getUser(student.getUserId()).getStatus());
        assertTrue(sessions.contains(student.getSessionToken()));
        assertEquals("PENDING", service.listOwnAccountCancellations(student, null)
                .getRequests().get(0).getStatus());
    }

    private SessionContext login(String account, String password) throws Exception {
        AuthService auth = new AuthService(repository.getAuthRepository(), hasher, sessions);
        LoginResult result = auth.login(account, password);
        return sessions.resolve(result.getSessionToken());
    }

    private static void expect(String code, Action action) throws Exception {
        try {
            action.run();
            fail("expected " + code);
        } catch (IdentityServiceException ex) {
            assertEquals(code, ex.getResultCode());
        }
    }

    private interface Action { void run() throws Exception; }
}
