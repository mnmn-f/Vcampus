package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TCP 半连接、非协议对象和反序列化拒绝路径不会占住服务端线程。 */
public final class TcpServerProtocolResilienceTest {
    private TcpTestHarness harness;

    @Before
    public void setUp() throws Exception {
        harness = new TcpTestHarness();
        harness.start(2, 150);
    }

    @After
    public void tearDown() {
        if (harness != null) harness.close();
    }

    @Test
    public void idleHandshakeTimesOutAndServerContinuesAccepting() throws Exception {
        try (Socket idle = harness.connect()) {
            idle.setSoTimeout(1500);
            ObjectInputStream input = new ObjectInputStream(idle.getInputStream());
            long started = System.currentTimeMillis();
            assertClosed(input);
            assertTrue(System.currentTimeMillis() - started < 1200L);
        }
        try (Socket valid = harness.connect()) {
            assertNotNull(harness.login(valid));
        }
    }

    @Test
    public void halfCloseAfterLoginKeepsSessionForReconnect() throws Exception {
        try (Socket socket = harness.connect()) {
            socket.setSoTimeout(1500);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            output.writeObject(Message.request(Commands.AUTH_LOGIN, null,
                    new LoginRequest("student", "secret")));
            output.flush();
            Message response = (Message) input.readObject();
            assertTrue(response.isSuccess());
            assertNotNull(((LoginResult) response.getPayload()).getSessionToken());
            assertEquals(1, harness.sessions.activeSessionCount());
            socket.shutdownOutput();
            waitForSessions(1);
        }
    }

    @Test
    public void idleDisconnectKeepsTokenForBusinessReconnectUntilServerStop() throws Exception {
        String token;
        try (Socket socket = harness.connect()) {
            socket.setSoTimeout(250);
            token = harness.login(socket);
            waitForPeerClose(socket.getInputStream());
        }
        assertTrue(harness.sessions.contains(token));
        try (Socket reconnect = harness.connect()) {
            ObjectOutputStream output = new ObjectOutputStream(reconnect.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(reconnect.getInputStream());
            output.writeObject(Message.request(TcpTestHarness.SESSION_PROBE, token, "ping"));
            output.flush();
            Message response = (Message) input.readObject();
            assertTrue(response.isSuccess());
            assertEquals("session-alive", response.getPayload());
        }
        harness.server().stop();
        assertFalse(harness.sessions.contains(token));
        Message afterStop = harness.router.route(Message.request(
                TcpTestHarness.SESSION_PROBE, token, "ping"));
        assertEquals(ResultCodes.UNAUTHORIZED, afterStop.getResultCode());
    }

    @Test
    public void nonMessageAndTruncatedFrameAreClosed() throws Exception {
        try (Socket socket = harness.connect()) {
            socket.setSoTimeout(1500);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            output.writeObject("not-a-message");
            output.flush();
            assertClosed(input);
        }
        try (Socket partial = harness.connect()) {
            OutputStream output = partial.getOutputStream();
            output.write(new byte[]{(byte) 0xAC, (byte) 0xED, 0, 5});
            output.flush();
        }
        try (Socket valid = harness.connect()) {
            assertNotNull(harness.login(valid));
        }
    }

    @Test
    public void nonWhitelistedClassIsRejectedWithoutStackTraceResponse() throws Exception {
        try (Socket socket = harness.connect()) {
            socket.setSoTimeout(1500);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            output.writeObject(new ForbiddenPayload());
            output.flush();
            assertClosed(input);
        }
    }

    private void waitForSessions(int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 1200L;
        while (harness.sessions.activeSessionCount() != expected
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        assertEquals(expected, harness.sessions.activeSessionCount());
    }

    private static void waitForPeerClose(InputStream input) throws Exception {
        long deadline = System.currentTimeMillis() + 1200L;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (input.read() < 0) return;
            } catch (SocketTimeoutException expected) {
                // The server's idle timeout may not have elapsed yet.
            }
        }
        fail("server did not close the idle socket");
    }

    private static void assertClosed(ObjectInputStream input) throws Exception {
        try {
            input.readObject();
            fail("connection should close without a response");
        } catch (EOFException expected) {
            // Expected after the protocol violation or timeout.
        } catch (SocketException expected) {
            // Expected when the handler closes the socket concurrently.
        }
    }

    private static final class ForbiddenPayload implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
