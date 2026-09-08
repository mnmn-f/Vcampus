package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TcpServerIntegrationTest {
    private TcpServer server;
    private SessionManager sessionManager;
    private CommandRouter router;

    @Before
    public void setUp() throws Exception {
        PasswordHasher hasher = new PasswordHasher(4);
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.save(new UserRecord(1L, "student", hasher.hash("secret"), "学生",
                edu.seu.vcampus.common.security.Role.STUDENT, true));
        sessionManager = new SessionManager();
        router = ServerMain.createRouter(repository, hasher,
                sessionManager);
        server = new TcpServer(0, 2, router);
        server.startAsync();
        assertTrue(server.isRunning());
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void objectStreamConnectionRoutesLoginAndUnknownCommand() throws Exception {
        LoginResult result;
        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(3000);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            output.writeObject(Message.request(Commands.AUTH_LOGIN, null,
                    new LoginRequest("student", "secret")));
            output.flush();
            Message login = (Message) input.readObject();
            assertTrue(login.isSuccess());
            result = (LoginResult) login.getPayload();
            assertNotNull(result.getSessionToken());

            output.writeObject(Message.request("not.registered", result.getSessionToken(), null));
            output.flush();
            Message unknown = (Message) input.readObject();
            assertEquals("COMMON.UNKNOWN_COMMAND", unknown.getResultCode());
        }
        assertEquals(1, sessionManager.activeSessionCount());
        Message stillValid = router.route(Message.request("not.registered",
                result.getSessionToken(), null));
        assertEquals("COMMON.UNKNOWN_COMMAND", stillValid.getResultCode());

        server.stop();
        assertEquals(0, sessionManager.activeSessionCount());
        Message afterStop = router.route(Message.request("auth.logout",
                result.getSessionToken(), null));
        assertEquals(ResultCodes.UNAUTHORIZED, afterStop.getResultCode());
    }
}
