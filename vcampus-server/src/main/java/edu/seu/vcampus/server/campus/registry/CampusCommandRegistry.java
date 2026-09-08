package edu.seu.vcampus.server.campus.registry;

import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.server.campus.handler.CampusCommandHandler;
import edu.seu.vcampus.server.campus.repository.mysql.MySqlCampusRepository;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.router.CommandRouter;

/** 教务扩展模块集中接线点；启动入口只需创建服务并调用 register。 */
public final class CampusCommandRegistry {
    private CampusCommandRegistry() { }

    public static CampusService createMySqlService(TransactionManager transactions) {
        if (transactions == null) throw new IllegalArgumentException("transactions is required");
        return new CampusService(new MySqlCampusRepository(), transactions);
    }

    public static CampusService productionService(JdbcConnectionFactory connections) {
        if (connections == null) throw new IllegalArgumentException("connections is required");
        return createMySqlService(new TransactionManager(connections));
    }

    public static CommandRouter register(CommandRouter router, CampusService service) {
        if (router == null || service == null) throw new IllegalArgumentException("campus dependencies required");
        String[] commands = { CampusCommands.ANNOUNCEMENT_LIST, CampusCommands.ANNOUNCEMENT_SAVE,
                CampusCommands.ANNOUNCEMENT_REVOKE, CampusCommands.COMPETITION_LIST,
                CampusCommands.COMPETITION_SAVE, CampusCommands.COMPETITION_REGISTER,
                CampusCommands.COMPETITION_CANCEL, CampusCommands.COMPETITION_ROSTER,
                CampusCommands.COMPETITION_MINE,
                CampusCommands.SRTP_MINE, CampusCommands.SRTP_LIST, CampusCommands.SRTP_SAVE,
                CampusCommands.SRTP_REVIEW, CampusCommands.CLASSROOM_LIST,
                CampusCommands.CLASSROOM_APPLY, CampusCommands.CLASSROOM_MINE,
                CampusCommands.CLASSROOM_REVIEW, CampusCommands.CLASSROOM_CANCEL,
                CampusCommands.CLASSROOM_REQUEST_LIST };
        for (String command : commands) router.register(command, new CampusCommandHandler(command, service));
        return router;
    }

    public static CommandRouter registerAll(CommandRouter router, CampusService service) {
        return register(router, service);
    }
}
