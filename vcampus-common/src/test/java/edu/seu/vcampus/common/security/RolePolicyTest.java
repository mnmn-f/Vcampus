package edu.seu.vcampus.common.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RolePolicyTest {
    @Test
    public void dormManagerCannotManageCourses() {
        assertTrue(RolePolicy.allows(Role.DORM_MANAGER, Permission.DORM_APPROVE));
        assertFalse(RolePolicy.allows(Role.DORM_MANAGER, Permission.COURSE_MANAGE));
    }

    @Test
    public void systemAdminDoesNotOwnBusinessPermissions() {
        assertTrue(RolePolicy.allows(Role.SYSTEM_ADMIN, Permission.USER_MANAGE));
        assertTrue(RolePolicy.allows(Role.SYSTEM_ADMIN, Permission.PROFILE_UPDATE));
        assertTrue(RolePolicy.allows(Role.SYSTEM_ADMIN, Permission.SYSTEM_MONITOR));
        assertFalse(RolePolicy.allows(Role.SYSTEM_ADMIN, Permission.COURSE_MANAGE));
        assertFalse(RolePolicy.allows(Role.SYSTEM_ADMIN, Permission.DORM_APPROVE));
    }

    @Test
    public void everyAuthenticatedRoleCanUpdateOwnProfile() {
        for (Role role : Role.values()) {
            assertTrue(role.name(), RolePolicy.allows(role, Permission.PROFILE_READ));
            assertTrue(role.name(), RolePolicy.allows(role, Permission.PROFILE_UPDATE));
        }
    }
}
