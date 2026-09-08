package edu.seu.vcampus.client.view;

import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationPolicyTest {
    @Test
    public void systemAdminSeesOnlySystemResponsibilities() {
        assertTrue(ModuleId.DASHBOARD.isVisibleTo(Role.SYSTEM_ADMIN));
        assertTrue(ModuleId.USER_ADMIN.isVisibleTo(Role.SYSTEM_ADMIN));
        assertTrue(ModuleId.SYSTEM.isVisibleTo(Role.SYSTEM_ADMIN));
        assertFalse(ModuleId.ACADEMIC.isVisibleTo(Role.SYSTEM_ADMIN));
        assertFalse(ModuleId.DORMITORY.isVisibleTo(Role.SYSTEM_ADMIN));
        assertFalse(ModuleId.LIBRARY.isVisibleTo(Role.SYSTEM_ADMIN));
    }

    @Test
    public void businessManagersDoNotSeeUnrelatedAdminModules() {
        assertTrue(ModuleId.DORMITORY.isVisibleTo(Role.DORM_MANAGER));
        assertFalse(ModuleId.USER_ADMIN.isVisibleTo(Role.DORM_MANAGER));
        assertTrue(ModuleId.LIBRARY.isVisibleTo(Role.LIBRARIAN));
        assertFalse(ModuleId.STORE.isVisibleTo(Role.LIBRARIAN));
    }
}
