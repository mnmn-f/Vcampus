package edu.seu.vcampus.server.dorm.ext.registry;

import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.ext.handler.DormExtCommandHandler;
import edu.seu.vcampus.server.dorm.repository.mysql.MySqlDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.router.CommandRouter;

/** 宿舍扩展模块的集中接线点；启动入口只需创建服务并调用 registerAll。 */
public final class DormExtCommandRegistry {
    private DormExtCommandRegistry() { }

    public static DormExtService createMySqlService(TransactionManager transactions) {
        if (transactions == null) {
            throw new IllegalArgumentException("transactions is required");
        }
        return new DormExtService(new MySqlDormExtRepository(), transactions);
    }

    public static DormExtService productionService(JdbcConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("connections is required");
        }
        return createMySqlService(new TransactionManager(connections));
    }

    public static CommandRouter register(CommandRouter router, DormExtService service) {
        if (router == null || service == null) {
            throw new IllegalArgumentException("dorm ext dependencies required");
        }
        String[] commands = { DormExtCommands.STATUS, DormExtCommands.METER_LIST,
                DormExtCommands.METER_SUBMIT, DormExtCommands.BILL_GENERATE,
                DormExtCommands.WARNING_SCAN, DormExtCommands.WARNING_LIST,
                DormExtCommands.WARNING_NOTIFY, DormExtCommands.WARNING_VERIFY,
                DormExtCommands.WARNING_CONFIG_GET, DormExtCommands.WARNING_CONFIG_SET,
                DormExtCommands.VISITOR_SUBMIT, DormExtCommands.VISITOR_MINE,
                DormExtCommands.VISITOR_CANCEL, DormExtCommands.VISITOR_LIST,
                DormExtCommands.VISITOR_AUDIT,
                DormExtCommands.HYGIENE_SUBMIT, DormExtCommands.HYGIENE_DETAIL,
                DormExtCommands.HYGIENE_TASK_GENERATE, DormExtCommands.HYGIENE_TASK_LIST,
                DormExtCommands.HOME_SUMMARY,
                DormExtCommands.REPAIR_QUEUE, DormExtCommands.REPAIR_ASSIGNED,
                DormExtCommands.REPAIR_HISTORY,
                DormExtCommands.REPAIR_WORKERS, DormExtCommands.REPAIR_ASSIGN,
                DormExtCommands.REPAIR_DETAIL, DormExtCommands.REPAIR_REVIEW,
                DormExtCommands.REPAIR_ACCEPT, DormExtCommands.REPAIR_START,
                DormExtCommands.REPAIR_FINISH,
                DormExtCommands.STAY_MINE, DormExtCommands.STAY_LIST,
                DormExtCommands.ACCESS_MINE, DormExtCommands.ACCESS_POLICY_GET,
                DormExtCommands.ACCESS_POLICY_SET, DormExtCommands.REPAIR_PERMIT_SET,
                DormExtCommands.REPAIR_PERMIT_MINE, DormExtCommands.ROOM_DELETE,
                DormExtCommands.NOTICE_MINE, DormExtCommands.NOTICE_LIST,
                DormExtCommands.NOTICE_EXTRA_SET, DormExtCommands.SCHEDULER_RUN };
        for (String command : commands) {
            router.register(command, new DormExtCommandHandler(command, service));
        }
        return router;
    }

    public static CommandRouter registerAll(CommandRouter router, DormExtService service) {
        return register(router, service);
    }
}
