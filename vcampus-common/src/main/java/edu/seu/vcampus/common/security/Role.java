package edu.seu.vcampus.common.security;

/** 系统角色。管理员按业务职责拆分，避免获得无关模块权限。 */
public enum Role {
    STUDENT("学生"),
    TEACHER("任课教师"),
    REGISTRAR("学籍管理员"),
    ACADEMIC_ADMIN("教务老师"),
    LIBRARIAN("图书管理员"),
    STORE_MANAGER("商店管理员"),
    DORM_MANAGER("宿管员"),
    AI_KNOWLEDGE_ADMIN("AI知识管理员"),
    SYSTEM_ADMIN("系统管理员");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

