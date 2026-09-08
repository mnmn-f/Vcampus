package edu.seu.vcampus.client.view;

import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 面向校园人员的工作台名称、导航和常用业务入口。 */
public final class RoleWorkspace {
    private RoleWorkspace() { }

    public static String homeTitle(Role role) {
        switch (role) {
            case STUDENT: return "学生主页";
            case TEACHER: return "教师主页";
            case REGISTRAR: return "学籍工作台";
            case ACADEMIC_ADMIN: return "教务工作台";
            case LIBRARIAN: return "图书馆工作台";
            case STORE_MANAGER: return "商店工作台";
            case DORM_MANAGER: return "宿管工作台";
            case REPAIR_WORKER: return "报修工作台";
            case AI_KNOWLEDGE_ADMIN: return "知识服务工作台";
            default: return "系统管理工作台";
        }
    }

    public static String navigationLabel(Role role, ModuleId module) {
        if (module == ModuleId.DASHBOARD) return homeTitle(role);
        if (module == ModuleId.PROFILE) return "个人中心";
        if (module == ModuleId.ACADEMIC) {
            if (role == Role.STUDENT) return "选课与课表";
            if (role == Role.TEACHER) return "我的教学";
            return "教务管理";
        }
        if (module == ModuleId.STUDENT_RECORD) {
            return role == Role.STUDENT ? "我的学籍" : "学籍管理";
        }
        if (module == ModuleId.LIBRARY) return role == Role.LIBRARIAN ? "馆务管理" : "图书馆";
        if (module == ModuleId.STORE) return role == Role.STORE_MANAGER ? "商店运营" : "校园商店";
        if (module == ModuleId.DORMITORY) {
            if (role == Role.DORM_MANAGER) return "宿舍管理";
            if (role == Role.REPAIR_WORKER) return "报修工作台";
            return "宿舍生活";
        }
        if (module == ModuleId.USER_ADMIN) return "账号与角色";
        if (module == ModuleId.SYSTEM) return "安全与运行";
        return module.getDisplayName();
    }

    public static String group(ModuleId module) {
        if (module == ModuleId.DASHBOARD || module == ModuleId.PROFILE) return "我的工作台";
        if (module == ModuleId.USER_ADMIN || module == ModuleId.SYSTEM) return "系统管理";
        return "校园业务";
    }

    public static List<Action> actions(Role role) {
        switch (role) {
            case STUDENT: return list(
                    action("选课与课表", "选课、退选和查看课程安排", ModuleId.ACADEMIC),
                    action("我的学籍", "查看学籍与成绩", ModuleId.STUDENT_RECORD),
                    action("宿舍服务", "住宿、请假、报修和账单", ModuleId.DORMITORY),
                    action("图书馆", "检索图书、借阅与自习空间", ModuleId.LIBRARY),
                    action("校园商店", "选购商品和查看订单", ModuleId.STORE),
                    action("校园助手", "问答、查询和确认校园业务", ModuleId.AI_ASSISTANT));
            case TEACHER: return list(
                    action("我的教学", "课程、成绩和教室使用", ModuleId.ACADEMIC),
                    action("图书馆", "检索图书与线上资源", ModuleId.LIBRARY));
            case REGISTRAR: return list(
                    action("学籍管理", "维护学生档案与成绩记录", ModuleId.STUDENT_RECORD));
            case ACADEMIC_ADMIN: return list(
                    action("教务管理", "课程、排课、教室和校园活动", ModuleId.ACADEMIC));
            case LIBRARIAN: return list(
                    action("馆务管理", "书目、借阅台账、空间和线上资源", ModuleId.LIBRARY));
            case STORE_MANAGER: return list(
                    action("商店运营", "商品、订单和销售统计", ModuleId.STORE));
            case DORM_MANAGER: return list(
                    action("宿舍管理", "住宿、审批、报修和空间维护", ModuleId.DORMITORY));
            case REPAIR_WORKER: return list(
                    action("报修工作台", "接单、上报进度和查看入内授权", ModuleId.DORMITORY));
            case AI_KNOWLEDGE_ADMIN: return list(
                    action("知识服务", "维护助手知识库与查看运行状态", ModuleId.AI_ASSISTANT),
                    action("系统运行", "查看服务运行状态", ModuleId.SYSTEM));
            default: return list(
                    action("账号与角色", "维护账号、角色和会话", ModuleId.USER_ADMIN),
                    action("安全与运行", "查看审计和系统状态", ModuleId.SYSTEM));
        }
    }

    private static Action action(String title, String description, ModuleId module) {
        return new Action(title, description, module);
    }

    private static List<Action> list(Action... actions) {
        return Collections.unmodifiableList(Arrays.asList(actions));
    }

    public static final class Action {
        private final String title;
        private final String description;
        private final ModuleId module;
        private Action(String title, String description, ModuleId module) {
            this.title = title; this.description = description; this.module = module;
        }
        public String title() { return title; }
        public String description() { return description; }
        public ModuleId module() { return module; }
    }
}
