package edu.seu.vcampus.server.identity.registry;

import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.identity.handler.IdentityCommandHandler;
import edu.seu.vcampus.server.identity.repository.DelegatingIdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentityAuditRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlAccountCancellationRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentityMonitorRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentityRoleRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentitySessionRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentityUserRepository;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.identity.service.IdentityTransactionManagerRunner;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;

/** 身份模块的唯一命令注册与生产仓储组合入口。 */
public final class IdentityCommandRegistry {
    private IdentityCommandRegistry() { }

    public static IdentityService createMySqlService(TransactionManager transactions,
                                                       PasswordHasher hasher,
                                                       SessionManager sessions) {
        if (transactions == null || hasher == null) throw new IllegalArgumentException("identity dependencies required");
        IdentityRecordRepository repository = new DelegatingIdentityRecordRepository(
                new MySqlIdentityUserRepository(), new MySqlIdentityRoleRepository(),
                new MySqlIdentitySessionRepository(), new MySqlIdentityAuditRepository(),
                new MySqlIdentityMonitorRepository(), new MySqlAccountCancellationRepository());
        return new IdentityService(repository, hasher,
                sessions == null ? new SessionManager() : sessions,
                new IdentityTransactionManagerRunner(transactions));
    }

    public static IdentityService createMySqlService(TransactionManager transactions,
                                                       PasswordHasher hasher) {
        return createMySqlService(transactions, hasher, new SessionManager());
    }

    public static IdentityService createInMemoryService(InMemoryIdentityRecordRepository repository,
                                                         PasswordHasher hasher) {
        if (repository == null || hasher == null) throw new IllegalArgumentException("identity dependencies required");
        return new IdentityService(repository, hasher, repository.getSessionManager(),
                new edu.seu.vcampus.server.identity.service.InMemoryIdentityTransactionRunner(repository));
    }

    public static CommandRouter registerAll(CommandRouter router, IdentityService service) {
        if (router == null || service == null) throw new IllegalArgumentException("identity registry dependencies required");
        router.register(IdentityCommands.REGISTER, new IdentityCommandHandler(IdentityCommands.REGISTER, service));
        register(router, IdentityCommands.PROFILE_SELF, service);
        register(router, IdentityCommands.PROFILE_UPDATE, service);
        register(router, IdentityCommands.PASSWORD_CHANGE, service);
        register(router, IdentityCommands.USER_SEARCH, service);
        register(router, IdentityCommands.USER_STATUS_UPDATE, service);
        register(router, IdentityCommands.USER_PASSWORD_RESET, service);
        register(router, IdentityCommands.ROLE_LIST, service);
        register(router, IdentityCommands.ROLE_ASSIGN, service);
        register(router, IdentityCommands.ROLE_REVOKE, service);
        register(router, IdentityCommands.SESSION_LIST, service);
        register(router, IdentityCommands.SESSION_REVOKE, service);
        register(router, IdentityCommands.LOGIN_AUDIT_PAGE, service);
        register(router, IdentityCommands.BUSINESS_AUDIT_PAGE, service);
        register(router, IdentityCommands.SYSTEM_MONITOR, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_WITHDRAW, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_APPROVE, service);
        register(router, IdentityCommands.ACCOUNT_CANCELLATION_REJECT, service);
        return router;
    }

    public static CommandRouter register(CommandRouter router, IdentityService service) {
        return registerAll(router, service);
    }

    private static void register(CommandRouter router, String command, IdentityService service) {
        router.register(command, new IdentityCommandHandler(command, service));
    }
}
