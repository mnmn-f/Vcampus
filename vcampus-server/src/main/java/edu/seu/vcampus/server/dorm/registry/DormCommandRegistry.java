package edu.seu.vcampus.server.dorm.registry;

import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.handler.DormCommandHandler;
import edu.seu.vcampus.server.dorm.repository.mysql.MySqlDormRepository;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.router.CommandRouter;

/** 宿舍模块集中接线点；启动入口只需创建服务并调用 register。 */
public final class DormCommandRegistry {
    private DormCommandRegistry() { }

    public static DormService createMySqlService(TransactionManager transactions) {
        if (transactions == null) throw new IllegalArgumentException("transactions is required");
        return new DormService(new MySqlDormRepository(), transactions);
    }

    public static DormService productionService(JdbcConnectionFactory connections) {
        if (connections == null) throw new IllegalArgumentException("connections is required");
        return createMySqlService(new TransactionManager(connections));
    }

    public static CommandRouter register(CommandRouter router, DormService service) {
        if (router == null || service == null) throw new IllegalArgumentException("dorm dependencies required");
        String[] commands = { DormCommands.BUILDING_LIST, DormCommands.ROOM_LIST, DormCommands.BED_LIST,
                DormCommands.BUILDING_CREATE, DormCommands.BUILDING_UPDATE,
                DormCommands.ROOM_CREATE, DormCommands.ROOM_UPDATE,
                DormCommands.BED_CREATE, DormCommands.BED_UPDATE,
                DormCommands.ACCOMMODATION_MINE, DormCommands.ACCOMMODATION_ASSIGN,
                DormCommands.ACCOMMODATION_TRANSFER, DormCommands.ACCOMMODATION_CHECKOUT,
                DormCommands.REQUEST_SUBMIT, DormCommands.REQUEST_LIST, DormCommands.REQUEST_APPROVE,
                DormCommands.ACCESS_RECORD, DormCommands.ACCESS_LIST, DormCommands.ALERT_LIST,
                DormCommands.ALERT_HANDLE, DormCommands.HYGIENE_LIST, DormCommands.HYGIENE_SAVE,
                DormCommands.REPAIR_LIST, DormCommands.REPAIR_CREATE, DormCommands.REPAIR_UPDATE,
                DormCommands.REPAIR_EVALUATE, DormCommands.LEAVE_SUBMIT, DormCommands.LEAVE_MINE,
                DormCommands.LEAVE_CANCEL, DormCommands.LEAVE_LIST, DormCommands.LEAVE_REVIEW,
                DormCommands.LEAVE_APPROVE, DormCommands.LEAVE_REJECT,
                DormCommands.UTILITY_MINE, DormCommands.UTILITY_MANAGER_LIST, DormCommands.UTILITY_PAY,
                DormCommands.ANNOUNCEMENT_LIST,
                DormCommands.ANNOUNCEMENT_SAVE };
        for (String command : commands) router.register(command, new DormCommandHandler(command, service));
        return router;
    }

    public static CommandRouter registerAll(CommandRouter router, DormService service) {
        return register(router, service);
    }
}
