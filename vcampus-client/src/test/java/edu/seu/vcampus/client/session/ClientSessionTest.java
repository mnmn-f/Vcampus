package edu.seu.vcampus.client.session;

import edu.seu.vcampus.client.auth.DemoAuthClientService;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientSessionTest {
    @Test
    public void activeRoleCanBeSwitchedOnlyWithinTheLoginResult() throws Exception {
        LoginResult result = new DemoAuthClientService().login("demo_teacher", "teacher123");
        ClientSession session = new ClientSession();
        session.open(result);
        session.switchRole(Role.ACADEMIC_ADMIN);
        assertEquals(Role.ACADEMIC_ADMIN, session.getActiveRole());
        assertEquals("demo_teacher", session.getAccount());
        assertEquals("演示教师兼教务员", session.getDisplayName());
        assertTrue(session.getRoles().contains(Role.TEACHER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownRoleCannotBeSelected() throws Exception {
        LoginResult result = new DemoAuthClientService().login("demo_student", "student123");
        ClientSession session = new ClientSession();
        session.open(result);
        session.switchRole(Role.SYSTEM_ADMIN);
    }
}
