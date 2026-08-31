package edu.seu.vcampus.common.module;

import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.common.security.RolePolicy;

/** 主导航模块及其最低访问权限。 */
public enum ModuleId {
    DASHBOARD("工作台", Permission.PROFILE_READ),
    PROFILE("个人中心", Permission.PROFILE_READ),
    STUDENT_RECORD("学籍管理", Permission.STUDENT_RECORD_SELF_READ, Permission.STUDENT_RECORD_MANAGE),
    ACADEMIC("虚拟教务", Permission.COURSE_READ, Permission.COURSE_MANAGE, Permission.COURSE_TEACH),
    LIBRARY("图书馆", Permission.LIBRARY_READ, Permission.LIBRARY_MANAGE),
    STORE("校园商店", Permission.STORE_READ, Permission.STORE_MANAGE),
    DORMITORY("宿舍服务", Permission.DORM_SELF_READ, Permission.DORM_MANAGE),
    AI_ASSISTANT("校园助手", Permission.AI_QUERY, Permission.AI_KNOWLEDGE_MANAGE),
    USER_ADMIN("用户与权限", Permission.USER_MANAGE),
    SYSTEM("系统运行", Permission.SYSTEM_MONITOR);

    private final String displayName;
    private final Permission[] entryPermissions;

    ModuleId(String displayName, Permission... entryPermissions) {
        this.displayName = displayName;
        this.entryPermissions = entryPermissions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVisibleTo(Role role) {
        for (Permission permission : entryPermissions) {
            if (RolePolicy.allows(role, permission)) {
                return true;
            }
        }
        return false;
    }
}
