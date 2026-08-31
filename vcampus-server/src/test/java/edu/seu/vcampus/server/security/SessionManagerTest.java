package edu.seu.vcampus.server.security;

import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionManagerTest {
    @Test
    public void sessionKeepsRoleSetAndSupportsSafeActiveRoleSwitch() {
        SessionManager manager = new SessionManager();
        SessionContext session = manager.createSession(1L, "u", "用户",
                EnumSet.of(Role.STUDENT, Role.ACADEMIC_ADMIN));

        assertEquals(2, session.getRoles().size());
        assertFalse(session.allows(Permission.COURSE_MANAGE));
        assertTrue(manager.switchActiveRole(session.getSessionToken(), Role.ACADEMIC_ADMIN));
        assertTrue(manager.find(session.getSessionToken())
                .allows(Permission.COURSE_MANAGE));
        assertTrue(manager.switchActiveRole(session.getSessionToken(), Role.STUDENT));
        assertEquals(Role.STUDENT,
                manager.find(session.getSessionToken()).getActiveRole());
        assertFalse(manager.find(session.getSessionToken())
                .allows(Permission.COURSE_MANAGE));
        assertFalse(manager.switchActiveRole(session.getSessionToken(), Role.LIBRARIAN));
    }

    @Test
    public void invalidationRemovesSession() {
        SessionManager manager = new SessionManager();
        SessionContext session = manager.createSession(1L, "u", "用户",
                EnumSet.of(Role.STUDENT));
        manager.invalidate(session.getSessionToken());
        assertFalse(manager.contains(session.getSessionToken()));
        assertEquals(0, manager.activeSessionCount());
    }
}
