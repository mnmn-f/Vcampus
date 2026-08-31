package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** 网络测试共用的最小认证路由和服务生命周期。 */
final class TcpTestHarness implements AutoCloseable {
    static final String SESSION_PROBE = "test.session-probe";
    final PasswordHasher hasher = new PasswordHasher(4);
    final SessionManager sessions = new SessionManager();
    final CommandRouter router;
    private TcpServer server;

    TcpTestHarness() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        users.save(new UserRecord(1L, "student", hasher.hash("secret"), "学生",
                edu.seu.vcampus.common.security.Role.STUDENT, true));
        router = ServerMain.createRouter(users, hasher, sessions);
        router.register(SESSION_PROBE, new CommandHandler() {
            @Override public Message handle(Message request, SessionContext session) {
                return Message.success(request, "session-alive");
            }

            @Override public Permission requiredPermission() {
                return Permission.PROFILE_READ;
            }

            @Override public boolean requiresAuthentication() {
                return true;
            }
        });
    }

    void start(int maxConnections, int readTimeoutMillis) throws Exception {
        server = new TcpServer(0, maxConnections, router, readTimeoutMillis);
        server.startAsync();
    }

    TcpServer server() {
        if (server == null) throw new IllegalStateException("server not started");
        return server;
    }

    Socket connect() throws Exception {
        return new Socket("127.0.0.1", server().getBoundPort());
    }

    String login(Socket socket) throws Exception {
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        output.writeObject(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("student", "secret")));
        output.flush();
        Message response = (Message) input.readObject();
        return ((LoginResult) response.getPayload()).getSessionToken();
    }

    @Override
    public void close() {
        if (server != null) server.stop();
    }
}
