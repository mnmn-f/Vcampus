package edu.seu.vcampus.common.module;

import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.common.security.RolePolicy;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** 验证 AI 助手只对问答用户和知识管理员开放。 */
public final class ModuleIdTest {
    @Test
    public void aiPermissionsFollowLeastPrivilegePolicy() {
        for (Role role : Role.values()) {
            boolean student = role == Role.STUDENT;
            boolean knowledgeAdmin = role == Role.AI_KNOWLEDGE_ADMIN;
            assertTrue(role.name(), RolePolicy.allows(role, Permission.AI_QUERY) == student);
            assertTrue(role.name(),
                    RolePolicy.allows(role, Permission.AI_KNOWLEDGE_MANAGE) == knowledgeAdmin);
            assertTrue(role.name(), ModuleId.AI_ASSISTANT.isVisibleTo(role) == (student || knowledgeAdmin));
        }
    }
}
