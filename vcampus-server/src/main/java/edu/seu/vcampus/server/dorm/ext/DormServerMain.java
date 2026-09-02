package edu.seu.vcampus.server.dorm.ext;

import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.dorm.ext.registry.DormExtCommandRegistry;
import edu.seu.vcampus.server.dorm.ext.schedule.DormScheduler;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.network.TcpServer;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;

/**
 * 带宿舍扩展能力的服务端启动入口。
 *
 * <p>不修改 {@code ServerMain}：先调用它的 {@code createProductionRouter} 复用原有
 * 7 个模块的完整装配，再把宿舍扩展命令注册到同一个 {@code CommandRouter} 上，
 * 因此既有模块的行为、权限和事务边界完全不变。</p>
 *
 * <p>启动方式（不需要改 pom 的 mainClass，用 -cp 指定本类即可）：</p>
 * <pre>
 * java "-Dvcampus.db.url=jdbc:mysql://127.0.0.1:3306/vcampus" "-Dvcampus.db.user=vcampus"
 *      "-Dvcampus.db.password=***" -cp vcampus-server/target/vCampusServer.jar
 *      edu.seu.vcampus.server.dorm.ext.DormServerMain
 * </pre>
 */
public final class DormServerMain {
    private DormServerMain() { }

    // 服务端的生命周期就是进程的生命周期：start() 会一直阻塞到 stop()，
    // 而 stop() 由下面的关闭钩子调用（TcpServer.close() 内部也只是转调 stop()），
    // 因此这里不需要也不应该用 try-with-resources 提前关闭。
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        SessionManager sessions = new SessionManager();
        JdbcConnectionFactory connections = new JdbcConnectionFactory();
        UserRepository users = new MySqlUserRepository(connections);

        CommandRouter router = ServerMain.createProductionRouter(
                users, new PasswordHasher(), sessions, connections);
        int baseline = router.registeredCommandCount();

        DormExtService dormExt = DormExtCommandRegistry.productionService(connections);
        DormExtCommandRegistry.registerAll(router, dormExt);
        int added = router.registeredCommandCount() - baseline;

        final DormScheduler scheduler = startScheduler(dormExt);

        int port = port(args);
        say("模块版本 " + DormExtService.MODULE_VERSION);
        say("已复用 ServerMain 装配的 " + baseline + " 个命令，追加宿舍扩展命令 " + added
                + " 个，当前共 " + router.registeredCommandCount() + " 个");
        say("正在监听 TCP 端口 " + port + "，按 Ctrl+C 停止");

        final TcpServer server = new TcpServer(port, setting("vcampus.server.max-connections",
                TcpServer.DEFAULT_MAX_CONNECTIONS), router,
                setting("vcampus.server.client-read-timeout",
                        TcpServer.DEFAULT_CLIENT_READ_TIMEOUT_MILLIS));
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                say("正在停止服务端");
                if (scheduler != null) {
                    scheduler.stop();
                }
                server.stop();
            }
        }, "vcampus-dorm-ext-shutdown"));
        server.start();
    }

    /**
     * 启动定时任务；未启用时返回 null。
     *
     * <p>调度失败不应该拖垮命令服务，因此这里只在启用时接线，任务体内部的异常
     * 由调度器自己收口。用 {@code -Dvcampus.dorm.scheduler.enabled=false} 可以
     * 完全关掉，服务端就退化成纯命令响应模式。</p>
     */
    private static DormScheduler startScheduler(DormExtService service) {
        if (!DormScheduler.enabled()) {
            say("定时任务已按 " + DormScheduler.KEY_ENABLED + "=false 关闭");
            return null;
        }
        DormScheduler scheduler = new DormScheduler(service);
        service.attachTaskRunner(scheduler);
        scheduler.start();
        say("定时任务已启动，共 " + scheduler.taskCount() + " 项");
        for (String line : scheduler.describe()) {
            say("  - " + line);
        }
        if (System.getProperty(DormScheduler.KEY_OPERATOR) == null) {
            say("提示：未配置 " + DormScheduler.KEY_OPERATOR
                    + "，月度出账任务将跳过（其余四项不受影响）");
        }
        return scheduler;
    }

    /** 服务端运行日志统一带模块前缀，便于在控制台里和其他模块区分。 */
    private static void say(String text) {
        System.out.println("[dorm-ext] " + text);
    }

    private static int port(String[] args) {
        String value = args != null && args.length > 0 && !args[0].trim().isEmpty()
                ? args[0].trim()
                : System.getProperty("vcampus.server.port", "" + TcpServer.DEFAULT_PORT);
        int parsed = number(value, "vcampus.server.port");
        if (parsed < 0 || parsed > 65535) {
            throw new IllegalArgumentException("vcampus.server.port 必须在 0 到 65535 之间");
        }
        return parsed;
    }

    private static int setting(String key, int fallback) {
        int parsed = number(System.getProperty(key, Integer.toString(fallback)), key);
        if (parsed <= 0) {
            throw new IllegalArgumentException(key + " 必须是正整数");
        }
        return parsed;
    }

    private static int number(String value, String key) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " 不是有效整数: " + value, ex);
        }
    }
}
