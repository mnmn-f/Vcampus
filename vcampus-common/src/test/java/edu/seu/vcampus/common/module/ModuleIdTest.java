package edu.seu.vcampus.common.module;

import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.common.security.RolePolicy;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/** 验证未实现的 AI 功能不会出现在任何角色导航中。 */
public final class ModuleIdTest {
    @Test
    public void aiPermissionsAreNotGrantedToAnyRole() {
        for (Role role : Role.values()) {
            assertFalse(role.name(), RolePolicy.allows(role, Permission.AI_QUERY));
            assertFalse(role.name(), RolePolicy.allows(role, Permission.AI_KNOWLEDGE_MANAGE));
            assertFalse(role.name(), ModuleId.AI_ASSISTANT.isVisibleTo(role));
        }
    }
}
