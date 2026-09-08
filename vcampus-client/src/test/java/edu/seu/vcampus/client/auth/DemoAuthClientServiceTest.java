package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DemoAuthClientServiceTest {
    @Test
    public void loginReturnsTheMatchedDutyRole() throws Exception {
        DemoAuthClientService service = new DemoAuthClientService();
        LoginResult result = service.login("demo_academic", "academic123");
        assertEquals(Role.ACADEMIC_ADMIN, result.getActiveRole());
        assertTrue(result.getRoles().contains(Role.ACADEMIC_ADMIN));
        assertEquals("演示教务老师", result.getDisplayName());
        assertEquals(4L, result.getUserId());
        assertTrue(service.isLoggedIn());
    }

    @Test
    public void multiDutyAccountPreservesAllRoles() throws Exception {
        LoginResult result = new DemoAuthClientService().login("demo_teacher", "teacher123");
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains(Role.TEACHER));
        assertTrue(result.getRoles().contains(Role.ACADEMIC_ADMIN));
    }

    @Test
    public void demoSeedCredentialsAreAcceptedWithoutClientSidePasswordPolicy() throws Exception {
        LoginResult result = new DemoAuthClientService().login("demo_ai", "ai123");
        assertEquals("演示AI知识管理员", result.getDisplayName());
        assertEquals(Role.AI_KNOWLEDGE_ADMIN, result.getActiveRole());
    }

    @Test(expected = ClientServiceException.class)
    public void invalidPasswordIsRejected() throws Exception {
        new DemoAuthClientService().login("student", "wrong-password");
    }
}
