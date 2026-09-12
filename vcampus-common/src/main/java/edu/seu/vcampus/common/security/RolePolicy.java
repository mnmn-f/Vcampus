package edu.seu.vcampus.common.security;

import java.util.Collections;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** 角色与权限的唯一登记处。 */
public final class RolePolicy {
    private static final Map<Role, Set<Permission>> POLICIES = buildPolicies();

    private RolePolicy() {
    }

    public static boolean allows(Role role, Permission permission) {
        Set<Permission> permissions = POLICIES.get(role);
        return permissions != null && permissions.contains(permission);
    }

    public static boolean allowsAny(Collection<Role> roles, Permission permission) {
        if (roles == null) {
            return false;
        }
        for (Role role : roles) {
            if (allows(role, permission)) {
                return true;
            }
        }
        return false;
    }

    public static Set<Permission> permissionsOf(Role role) {
        Set<Permission> permissions = POLICIES.get(role);
        return permissions == null ? Collections.<Permission>emptySet() : permissions;
    }

    private static Map<Role, Set<Permission>> buildPolicies() {
        Map<Role, Set<Permission>> result = new EnumMap<Role, Set<Permission>>(Role.class);
        result.put(Role.STUDENT, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE,
                Permission.STUDENT_RECORD_SELF_READ, Permission.SCORE_SELF_READ,
                Permission.COURSE_READ, Permission.COURSE_ENROLL,
                Permission.ANNOUNCEMENT_READ, Permission.COMPETITION_ENROLL,
                Permission.SRTP_SELF_READ, Permission.CLASSROOM_RESERVE,
                Permission.LIBRARY_READ, Permission.LIBRARY_BORROW,
                Permission.STUDY_ROOM_RESERVE, Permission.STORE_READ,
                Permission.STORE_PURCHASE, Permission.DORM_SELF_READ,
                Permission.DORM_REQUEST, Permission.DORM_BILL_PAY,
                Permission.AI_QUERY));
        result.put(Role.TEACHER, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE,
                Permission.COURSE_READ, Permission.COURSE_TEACH,
                Permission.SCORE_RECORD, Permission.ANNOUNCEMENT_READ,
                Permission.CLASSROOM_RESERVE, Permission.LIBRARY_READ));
        result.put(Role.REGISTRAR, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.STUDENT_RECORD_MANAGE,
                Permission.ANNOUNCEMENT_READ));
        result.put(Role.ACADEMIC_ADMIN, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.COURSE_READ,
                Permission.COURSE_MANAGE, Permission.ANNOUNCEMENT_MANAGE,
                Permission.SCORE_AUDIT,
                Permission.COMPETITION_MANAGE, Permission.SRTP_MANAGE,
                Permission.CLASSROOM_APPROVE));
        result.put(Role.LIBRARIAN, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.LIBRARY_READ,
                Permission.LIBRARY_MANAGE, Permission.STUDY_ROOM_MANAGE,
                Permission.ANNOUNCEMENT_MANAGE));
        result.put(Role.STORE_MANAGER, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.STORE_READ,
                Permission.STORE_MANAGE, Permission.STORE_SALES_READ));
        result.put(Role.DORM_MANAGER, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.DORM_MANAGE,
                Permission.DORM_APPROVE, Permission.DORM_GOVERN,
                Permission.ANNOUNCEMENT_MANAGE));
        // 维修员只拿到「干活」这一条宿舍权限：他要进学生宿舍，但不该顺带看到住宿
        // 名册、水电账单和卫生检查。派单需要的房间号和联系方式随工单一起下发，
        // 而不是让他自己去查全楼的花名册。
        result.put(Role.REPAIR_WORKER, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE,
                Permission.DORM_REPAIR_WORK));
        result.put(Role.AI_KNOWLEDGE_ADMIN, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE,
                Permission.AI_KNOWLEDGE_MANAGE, Permission.SYSTEM_MONITOR));
        result.put(Role.SYSTEM_ADMIN, immutable(
                Permission.PROFILE_READ, Permission.PROFILE_UPDATE, Permission.USER_MANAGE,
                Permission.ROLE_MANAGE, Permission.SYSTEM_MONITOR));
        return Collections.unmodifiableMap(result);
    }

    private static Set<Permission> immutable(Permission first, Permission... rest) {
        EnumSet<Permission> permissions = EnumSet.of(first, rest);
        return Collections.unmodifiableSet(permissions);
    }
}
