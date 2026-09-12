package edu.seu.vcampus.server.student.registry;

import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.student.handler.StudentCommandHandler;
import edu.seu.vcampus.server.student.service.StudentRecordService;

/** 学籍模块的唯一命令注册入口，供 ServerMain 组合路由器时调用。 */
public final class StudentCommandRegistry {
    private StudentCommandRegistry() {
    }

    public static CommandRouter registerAll(CommandRouter router,
                                             StudentRecordService service) {
        if (router == null || service == null) {
            throw new IllegalArgumentException("student registry dependencies are required");
        }
        register(router, StudentCommands.SELF_PROFILE,
                Permission.STUDENT_RECORD_SELF_READ, service);
        register(router, StudentCommands.SELF_GRADES,
                Permission.SCORE_SELF_READ, service);
        register(router, StudentCommands.SELF_GRADE_REPORT,
                Permission.SCORE_SELF_READ, service);
        register(router, StudentCommands.SELF_GRADE_EXPORT,
                Permission.SCORE_SELF_READ, service);
        register(router, StudentCommands.PROFILE_SEARCH,
                Permission.STUDENT_RECORD_MANAGE, service);
        register(router, StudentCommands.PROFILE_CANDIDATES,
                Permission.STUDENT_RECORD_MANAGE, service);
        register(router, StudentCommands.PROFILE_DETAIL,
                Permission.STUDENT_RECORD_MANAGE, service);
        register(router, StudentCommands.PROFILE_CREATE,
                Permission.STUDENT_RECORD_MANAGE, service);
        register(router, StudentCommands.PROFILE_UPDATE,
                Permission.STUDENT_RECORD_MANAGE, service);
        register(router, StudentCommands.GRADE_RECORD,
                Permission.SCORE_RECORD, service);
        register(router, StudentCommands.GRADE_REVIEW,
                Permission.SCORE_AUDIT, service);
        return router;
    }

    private static void register(CommandRouter router, String command,
                                 Permission permission, StudentRecordService service) {
        router.register(command, new StudentCommandHandler(command, permission, service));
    }
}
