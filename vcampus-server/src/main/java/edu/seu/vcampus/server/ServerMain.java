package edu.seu.vcampus.server;

import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.auth.LoginCommandHandler;
import edu.seu.vcampus.server.auth.LoginAuditSink;
import edu.seu.vcampus.server.auth.LogoutCommandHandler;
import edu.seu.vcampus.server.auth.SwitchRoleCommandHandler;
import edu.seu.vcampus.server.ai.registry.AiCommandRegistry;
import edu.seu.vcampus.server.campus.registry.CampusCommandRegistry;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.academic.registry.AcademicCommandRegistry;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.registry.DormCommandRegistry;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.library.registry.LibraryCommandRegistry;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.identity.registry.IdentityCommandRegistry;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlLoginAuditSink;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.network.TcpServer;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import edu.seu.vcampus.server.student.registry.StudentCommandRegistry;
import edu.seu.vcampus.server.student.repository.mysql.MySqlStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.store.registry.StoreCommandRegistry;
import edu.seu.vcampus.server.store.service.StoreService;

/** 服务端启动入口；业务模块通过同一命令路由器继续注册。 */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        final SessionManager sessionManager = new SessionManager();
        JdbcConnectionFactory connections = new JdbcConnectionFactory();
        UserRepository repository = new MySqlUserRepository(connections);
        CommandRouter router = createProductionRouter(repository, new PasswordHasher(),
                sessionManager, connections);
        final int maxConnections = readPositiveInt("vcampus.server.max-connections",
                TcpServer.DEFAULT_MAX_CONNECTIONS);
        final int readTimeout = readPositiveInt("vcampus.server.client-read-timeout",
                TcpServer.DEFAULT_CLIENT_READ_TIMEOUT_MILLIS);
        final TcpServer server = new TcpServer(readPort(args), maxConnections, router,
                readTimeout);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        }, "vcampus-server-shutdown"));
        server.start();
    }

    /** 为集成测试和本地演示提供可替换仓储，但不改变生产入口。 */
    public static CommandRouter createRouter(UserRepository repository,
                                             PasswordHasher passwordHasher,
                                             SessionManager sessionManager) {
        return createRouter(repository, passwordHasher, sessionManager,
                LoginAuditSink.NOOP);
    }

    /** 认证装配扩展点；测试可用 NOOP，生产环境注入持久化审计。 */
    public static CommandRouter createRouter(UserRepository repository,
                                             PasswordHasher passwordHasher,
                                             SessionManager sessionManager,
                                             LoginAuditSink loginAudits) {
        AuthService authService = new AuthService(
                repository, passwordHasher, sessionManager, loginAudits);
        return new CommandRouter(sessionManager)
                .register(Commands.AUTH_LOGIN, new LoginCommandHandler(authService))
                .register(Commands.AUTH_LOGOUT, new LogoutCommandHandler(authService))
                .register(Commands.AUTH_SWITCH_ROLE,
                        new SwitchRoleCommandHandler(authService));
    }

    /** 生产装配入口：所有业务模块共享连接工厂、事务边界和命令路由器。 */
    public static CommandRouter createProductionRouter(UserRepository users,
                                                        PasswordHasher passwordHasher,
                                                        SessionManager sessions,
                                                        JdbcConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("connections is required");
        }
        CommandRouter router = createRouter(users, passwordHasher, sessions,
                new MySqlLoginAuditSink(connections));
        TransactionManager transactions = new TransactionManager(connections);

        StudentRecordService studentService = new StudentRecordService(
                new MySqlStudentRecordRepository(), transactions);
        AcademicService academicService =
                AcademicCommandRegistry.createMySqlService(transactions);
        LibraryService libraryService =
                LibraryCommandRegistry.createMySqlService(transactions);
        DormService dormService = DormCommandRegistry.createMySqlService(transactions);
        StoreService storeService = StoreCommandRegistry.createMySqlService(transactions);
        CampusService campusService = CampusCommandRegistry.createMySqlService(transactions);
        IdentityService identityService = IdentityCommandRegistry.createMySqlService(
                transactions, passwordHasher, sessions);

        StudentCommandRegistry.registerAll(router, studentService);
        AcademicCommandRegistry.registerAll(router, academicService);
        LibraryCommandRegistry.registerAll(router, libraryService);
        DormCommandRegistry.registerAll(router, dormService);
        StoreCommandRegistry.registerAll(router, storeService);
        CampusCommandRegistry.registerAll(router, campusService);
        IdentityCommandRegistry.registerAll(router, identityService);
        AiCommandRegistry.registerAll(router, transactions);
        return router;
    }

    private static int readPort(String[] args) {
        if (args != null && args.length > 0 && !args[0].trim().isEmpty()) {
            return parsePort(args[0].trim());
        }
        String configured = System.getProperty("vcampus.server.port", "" + TcpServer.DEFAULT_PORT);
        return parsePort(configured);
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 0 || port > 65535) {
                throw new NumberFormatException("out of range");
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid server port: " + value, ex);
        }
    }

    private static int readPositiveInt(String key, int fallback) {
        String value = System.getProperty(key, Integer.toString(fallback));
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) throw new NumberFormatException("not positive");
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " 必须是正整数", ex);
        }
    }
}
