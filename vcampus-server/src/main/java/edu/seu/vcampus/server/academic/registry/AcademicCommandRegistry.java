package edu.seu.vcampus.server.academic.registry;

import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.server.academic.handler.AcademicCommandHandler;
import edu.seu.vcampus.server.academic.repository.mysql.MySqlAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.router.CommandRouter;

/** 教务模块的集中接线点；ServerMain 只需创建服务并调用本类。 */
public final class AcademicCommandRegistry {
    private AcademicCommandRegistry() {
    }

    public static CommandRouter register(CommandRouter router, AcademicService service) {
        if (router == null || service == null) {
            throw new IllegalArgumentException("router and service are required");
        }
        return router
                .register(AcademicCommands.COURSE_LIST,
                        new AcademicCommandHandler(AcademicCommands.COURSE_LIST, service))
                .register(AcademicCommands.COURSE_CREATE,
                        new AcademicCommandHandler(AcademicCommands.COURSE_CREATE, service))
                .register(AcademicCommands.COURSE_UPDATE,
                        new AcademicCommandHandler(AcademicCommands.COURSE_UPDATE, service))
                .register(AcademicCommands.SCHEDULE_CREATE,
                        new AcademicCommandHandler(AcademicCommands.SCHEDULE_CREATE, service))
                .register(AcademicCommands.SCHEDULE_UPDATE,
                        new AcademicCommandHandler(AcademicCommands.SCHEDULE_UPDATE, service))
                .register(AcademicCommands.SCHEDULE_DELETE,
                        new AcademicCommandHandler(AcademicCommands.SCHEDULE_DELETE, service))
                .register(AcademicCommands.STUDENT_ENROLL,
                        new AcademicCommandHandler(AcademicCommands.STUDENT_ENROLL, service))
                .register(AcademicCommands.STUDENT_DROP,
                        new AcademicCommandHandler(AcademicCommands.STUDENT_DROP, service))
                .register(AcademicCommands.STUDENT_SCHEDULE,
                        new AcademicCommandHandler(AcademicCommands.STUDENT_SCHEDULE, service))
                .register(AcademicCommands.TEACHER_COURSES,
                        new AcademicCommandHandler(AcademicCommands.TEACHER_COURSES, service));
    }

    public static CommandRouter registerAll(CommandRouter router, AcademicService service) {
        return register(router, service);
    }

    /** 生产接线的唯一创建点；调用方随后将返回的服务注册到路由器。 */
    public static AcademicService productionService(JdbcConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("connections is required");
        }
        return new AcademicService(new MySqlAcademicRepository(connections),
                new TransactionManager(connections));
    }

    /** 与其他业务模块一致的接线方式；事务连接由调用方统一管理。 */
    public static AcademicService createMySqlService(TransactionManager transactions) {
        if (transactions == null) {
            throw new IllegalArgumentException("transactions is required");
        }
        return new AcademicService(new MySqlAcademicRepository(), transactions);
    }
}
