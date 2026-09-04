package edu.seu.vcampus.server.network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/** 连接池有界且服务停止会主动回收阻塞客户端和监听端口。 */
public final class TcpServerLifecycleTest {
    private TcpTestHarness harness;

    @Before
    public void setUp() {
        harness = new TcpTestHarness();
    }

    @After
    public void tearDown() {
        if (harness != null) harness.close();
    }

    @Test
    public void stopClosesOpenConnectionAndReleasesPort() throws Exception {
        harness.start(1, 30000);
        TcpServer server = harness.server();
        int port = server.getBoundPort();
        Socket socket = harness.connect();
        try {
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            server.stop();
            try {
                assertEquals(-1, input.read());
            } catch (EOFException expected) {
                // Closing an object stream may report EOF instead of -1.
            }
            assertFalse(server.isRunning());
            try (ServerSocket reopened = new ServerSocket(port)) {
                assertEquals(port, reopened.getLocalPort());
            }
        } finally {
            socket.close();
        }
    }

    @Test
    public void connectionPoolRejectsConnectionsBeyondActiveAndQueueBounds() throws Exception {
        harness.start(1, 10000);
        Socket first = harness.connect();
        Socket second = harness.connect();
        Socket third = harness.connect();
        try {
            first.setSoTimeout(1000);
            readHeader(first.getInputStream());
            third.setSoTimeout(2000);
            Thread.sleep(150L);
            assertEquals(-1, third.getInputStream().read());
        } finally {
            first.close();
            second.close();
            third.close();
        }
    }

    @Test
    public void desktopDefaultAllowsNormalIdleReadingTime() {
        assertEquals(30 * 60 * 1000, TcpServer.DEFAULT_CLIENT_READ_TIMEOUT_MILLIS);
    }

    private static void readHeader(InputStream input) throws Exception {
        byte[] header = new byte[4];
        int offset = 0;
        while (offset < header.length) {
            int count = input.read(header, offset, header.length - offset);
            if (count < 0) fail("server closed the active connection");
            offset += count;
        }
        assertEquals((byte) 0xAC, header[0]);
        assertEquals((byte) 0xED, header[1]);
    }
}
