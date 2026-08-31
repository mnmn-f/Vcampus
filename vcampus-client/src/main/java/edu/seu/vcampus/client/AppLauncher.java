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

/** VCampus 客户端启动入口。默认连接服务端，Demo 仅用于显式界面预览。 */
public final class AppLauncher {
    private AppLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                UiFactory.configureLookAndFeel();
                ClientSession session = new ClientSession();
                RuntimeServices runtime = createRuntime(session);
                showLogin(runtime.auth, session, runtime.business);
            }
        });
    }

    private static RuntimeServices createRuntime(ClientSession session) {
        String mode = System.getProperty("vcampus.client.mode", "network").trim();
        if ("demo".equalsIgnoreCase(mode)) {
            return new RuntimeServices(new DemoAuthClientService(), null);
        }
        if (!"network".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("vcampus.client.mode 必须是 network 或 demo");
        }
        String host = System.getProperty("vcampus.server.host", "127.0.0.1").trim();
        String portText = System.getProperty("vcampus.server.port", "8888").trim();
        try {
            int port = Integer.parseInt(portText);
            int connectTimeout = readPositiveInt("vcampus.server.connect-timeout", 5000);
            int readTimeout = readPositiveInt("vcampus.server.read-timeout", 5000);
            SocketClientGateway gateway = new SocketClientGateway(host, port,
                    connectTimeout, readTimeout);
            NetworkClientService network = new NetworkClientService(gateway);
            return new RuntimeServices(new NetworkAuthClientService(gateway),
                    new ClientBusinessServices(network, session));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("vcampus.server.port 必须是有效端口", ex);
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

    private static void showLogin(final AuthClientService auth, final ClientSession session,
                                  final ClientBusinessServices business) {
        final LoginFrame[] holder = new LoginFrame[1];
        holder[0] = new LoginFrame(auth, session, new LoginFrame.Listener() {
            @Override public void onLoginSuccess(edu.seu.vcampus.common.dto.auth.LoginResult result) {
                if (business != null) business.synchronizeSession();
                holder[0].setVisible(false);
                holder[0].dispose();
                final AppFrame[] app = new AppFrame[1];
                app[0] = new AppFrame(auth, session, new AppFrame.Listener() {
                    @Override public void onLogout() {
                        app[0].setVisible(false);
                        app[0].dispose();
                        showLogin(auth, session, business);
                    }
                }, new DisabledAiAssistantClientService(), business);
                app[0].setVisible(true);
            }
        }, business == null ? null : business.identity());
        holder[0].setVisible(true);
    }

    private static final class RuntimeServices {
        private final AuthClientService auth;
        private final ClientBusinessServices business;

        private RuntimeServices(AuthClientService auth, ClientBusinessServices business) {
            this.auth = auth;
            this.business = business;
        }
    }
}
