package edu.seu.vcampus.client;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.auth.DemoAuthClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.auth.NetworkAuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.view.AppFrame;
import edu.seu.vcampus.client.view.LoginFrame;

import javax.swing.SwingUtilities;

/**
 * VCampus 客户端启动入口。
 *
 * 说明：
 * - 默认运行模式为 network（与服务器通信）。
 * - 可以通过系统属性 `vcampus.client.mode=demo` 运行界面预览（不会连接真实服务端）。
 */
public final class AppLauncher {
    // 私有构造器，禁止实例化该工具类
    private AppLauncher() {
    }

    /**
     * 程序主入口。
     * 使用 Swing 的事件分发线程（EDT）来初始化 UI，保证线程安全。
     * 步骤：
     * 1. 设置界面风格（Look & Feel）。
     * 2. 创建 `ClientSession`（客户端会话，保存当前登录用户等状态）。
     * 3. 根据系统属性构建运行时服务（认证/业务服务）。
     * 4. 显示登录窗口。
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                // 配置统一的界面风格（字体、主题等）
                UiFactory.configureLookAndFeel();

                // 创建客户端会话对象，负责保存登录状态、用户信息等
                ClientSession session = new ClientSession();

                // 根据运行模式（demo 或 network）创建运行时需要的服务
                RuntimeServices runtime = createRuntime(session);

                // 显示登录窗口，传入认证服务、会话对象与业务服务
                showLogin(runtime.auth, session, runtime.business);
            }
        });
    }

    /**
     * 根据系统属性创建运行时服务实例。
     * 支持两种模式：
     * - demo：使用 `DemoAuthClientService`，只用于界面预览，不创建业务服务（business 为 null）
     * - network：连接远程服务器，创建网络客户端网关与业务服务
     *
     * 支持的系统属性：
     * - `vcampus.client.mode`：运行模式，默认为 `network`。
     * - `vcampus.server.host`：服务器主机，默认为 `127.0.0.1`。
     * - `vcampus.server.port`：服务器端口，默认为 `8888`。
     * - `vcampus.server.connect-timeout`：连接超时（毫秒），默认为 5000。
     * - `vcampus.server.read-timeout`：读取超时（毫秒），默认为 5000。
     */
    private static RuntimeServices createRuntime(ClientSession session) {
        // 读取运行模式，默认为 network
        String mode = System.getProperty("vcampus.client.mode", "network").trim();

        // 如果是 demo 模式：仅返回一个 Demo 认证客户端，business 为 null（界面预览用途）
        if ("demo".equalsIgnoreCase(mode)) {
            return new RuntimeServices(new DemoAuthClientService(), null);
        }

        // 非 demo 且非 network 时，视为非法参数
        if (!"network".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("vcampus.client.mode 必须是 network 或 demo");
        }

        // network 模式：读取网络连接相关配置
        String host = System.getProperty("vcampus.server.host", "127.0.0.1").trim();
        String portText = System.getProperty("vcampus.server.port", "8888").trim();
        try {
            // 解析端口号（可能抛出 NumberFormatException）
            int port = Integer.parseInt(portText);

            // 读取并验证超时设置（要求为正整数）
            int connectTimeout = readPositiveInt("vcampus.server.connect-timeout", 5000);
            int readTimeout = readPositiveInt("vcampus.server.read-timeout", 5000);

            // 创建与服务器通信的网关（Socket 客户端网关）
            SocketClientGateway gateway = new SocketClientGateway(host, port,
                    connectTimeout, readTimeout);

            // 基于网关创建网络客户端服务，负责发送/接收协议层消息
            NetworkClientService network = new NetworkClientService(gateway);

            // 返回运行时服务：NetworkAuthClientService 用于认证，ClientBusinessServices 提供具体业务能力
            return new RuntimeServices(new NetworkAuthClientService(gateway),
                    new ClientBusinessServices(network, session));
        } catch (NumberFormatException ex) {
            // 若端口号不是有效整数，则抛出更具体的异常
            throw new IllegalArgumentException("vcampus.server.port 必须是有效端口", ex);
        }
    }

    /**
     * 从系统属性读取一个正整数配置，若不合法则抛出 IllegalArgumentException。
     * 用途：确保超时、端口等配置为正整数。
     */
    private static int readPositiveInt(String key, int fallback) {
        // 首先从系统属性中读取，若不存在则使用 fallback 的字符串形式
        String value = System.getProperty(key, Integer.toString(fallback));
        try {
            int parsed = Integer.parseInt(value.trim());
            // 确保为正
            if (parsed <= 0) throw new NumberFormatException("not positive");
            return parsed;
        } catch (NumberFormatException ex) {
            // 将具体的解析异常包装为 IllegalArgumentException，方便上层捕获与定位
            throw new IllegalArgumentException(key + " 必须是正整数", ex);
        }
    }

    /**
     * 显示登录窗口并处理登录成功后的切换逻辑。
     *
     * 说明关键点：
     * - 使用长度为 1 的数组 `holder` 和 `app` 来在匿名内部类中持有对窗口的可变引用，
     *   这是因为在 Java 中外部变量必须是 final 或 effectively final，而数组元素可变从而规避该限制。
     * - 登录成功后，如果存在业务服务，则先同步会话（synchronizeSession），随后关闭登录窗口，
     *   再创建主应用窗口 `AppFrame` 并显示。
     * - 在 `AppFrame` 的登出回调中，会隐藏并释放主窗口资源，然后重新显示登录窗口（实现登出-重登流程）。
     */
    private static void showLogin(final AuthClientService auth, final ClientSession session,
                                  final ClientBusinessServices business) {
        // 使用数组 holder 保存对 LoginFrame 的可变引用，以便在匿名内部类中访问并修改
        final LoginFrame[] holder = new LoginFrame[1];

        // 创建登录窗口，传入认证服务、会话对象以及登录成功回调
        holder[0] = new LoginFrame(auth, session, new LoginFrame.Listener() {
            @Override public void onLoginSuccess(edu.seu.vcampus.common.dto.auth.LoginResult result) {
                // 登录成功后，如果存在业务服务，则尝试与服务器同步会话数据
                if (business != null) business.synchronizeSession();

                // 隐藏并释放登录窗口资源
                holder[0].setVisible(false);
                holder[0].dispose();

                // 使用相同的数组技巧保存 AppFrame 的可变引用
                final AppFrame[] app = new AppFrame[1];

                // 创建主应用窗口，传入认证、会话、登出回调、AI 助手（被禁用的占位实现）和业务服务
                app[0] = new AppFrame(auth, session, new AppFrame.Listener() {
                    @Override public void onLogout() {
                        // 登出时隐藏并释放主窗口资源，然后重新显示登录窗口
                        app[0].setVisible(false);
                        app[0].dispose();
                        showLogin(auth, session, business);
                    }
                }, new DisabledAiAssistantClientService(), business);

                // 显示主应用窗口
                app[0].setVisible(true);
            }
        }, business == null ? null : business.identity());

        // 显示登录窗口（首次启动或登出后再次显示）
        holder[0].setVisible(true);
    }

    /**
     * 简单的容器类，用于携带运行时需要的服务引用（认证与业务服务）。
     */
    private static final class RuntimeServices {
        private final AuthClientService auth;
        private final ClientBusinessServices business;

        private RuntimeServices(AuthClientService auth, ClientBusinessServices business) {
            this.auth = auth;
            this.business = business;
        }
    }
}
