package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.protocol.Message;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 客户端断线处理：坏连接关闭，下一次显式请求才建立新连接。 */
public final class SocketClientGatewayResilienceTest {
    @Test
    public void ioFailureClosesConnectionAndNextRequestReconnectsOnce() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final AtomicInteger received = new AtomicInteger();
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            final CountDownLatch ready = new CountDownLatch(1);
            Thread worker = new Thread(new Runnable() {
                @Override public void run() { serveTwoConnections(server, received, failure, ready); }
            });
            worker.setDaemon(true);
            worker.start();
            ready.await(1, TimeUnit.SECONDS);
            SocketClientGateway gateway = new SocketClientGateway("127.0.0.1",
                    server.getLocalPort(), 1000, 1000);
            try {
                try {
                    gateway.send(Message.request("first", null, "once"));
                } catch (IOException expected) {
                    // The first connection is deliberately closed before a response.
                }
                assertFalse(gateway.isConnected());
                assertTrue(gateway.send(Message.request("second", null, "explicit"))
                        .isSuccess());
                assertEquals(2, received.get());
            } finally {
                gateway.close();
            }
            worker.join(2000L);
            assertNull(failure.get());
        }
    }

    @Test
    public void multipleSmallMessagesReuseConnectionWithinCumulativeBudget() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final AtomicInteger received = new AtomicInteger();
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            final CountDownLatch ready = new CountDownLatch(1);
            Thread worker = new Thread(new Runnable() {
                @Override public void run() { serveMany(server, received, failure, ready); }
            });
            worker.setDaemon(true);
            worker.start();
            ready.await(1, TimeUnit.SECONDS);
            SocketClientGateway gateway = new SocketClientGateway("127.0.0.1",
                    server.getLocalPort(), 1000, 1000);
            try {
                for (int index = 0; index < 20; index++) {
                    assertTrue(gateway.send(Message.request("small", null,
                            "payload-" + index)).isSuccess());
                }
                assertTrue(gateway.isConnected());
            } finally {
                gateway.close();
            }
            worker.join(2000L);
            assertEquals(20, received.get());
            assertNull(failure.get());
        }
    }

    @Test
    public void readTimeoutIsAppliedAndClosesStalledConnection() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final CountDownLatch accepted = new CountDownLatch(1);
            Thread worker = new Thread(new Runnable() {
                @Override public void run() { stall(server, accepted); }
            });
            worker.setDaemon(true);
            worker.start();
            SocketClientGateway gateway = new SocketClientGateway("127.0.0.1",
                    server.getLocalPort(), 1000, 100);
            try {
                accepted.await(1, TimeUnit.SECONDS);
                try {
                    gateway.send(Message.request("stall", null, null));
                } catch (IOException expected) {
                    // The peer intentionally sends no response.
                }
                assertFalse(gateway.isConnected());
            } finally {
                gateway.close();
            }
            worker.join(1500L);
        }
    }

    private static void serveTwoConnections(ServerSocket server, AtomicInteger received,
                                             AtomicReference<Throwable> failure,
                                             CountDownLatch ready) {
        try {
            ready.countDown();
            receiveAndClose(server.accept(), received);
            try (Socket socket = server.accept()) {
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                Message request = (Message) input.readObject();
                received.incrementAndGet();
                output.writeObject(Message.success(request, null));
                output.flush();
            }
        } catch (Throwable error) {
            failure.set(error);
        }
    }

    private static void receiveAndClose(Socket socket, AtomicInteger received) throws Exception {
        try (Socket value = socket) {
            ObjectOutputStream output = new ObjectOutputStream(value.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(value.getInputStream());
            input.readObject();
            received.incrementAndGet();
        }
    }

    private static void serveMany(ServerSocket server, AtomicInteger received,
                                  AtomicReference<Throwable> failure,
                                  CountDownLatch ready) {
        ready.countDown();
        try (Socket socket = server.accept()) {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            for (int index = 0; index < 20; index++) {
                Message request = (Message) input.readObject();
                received.incrementAndGet();
                output.writeObject(Message.success(request, "ok"));
                output.flush();
                output.reset();
            }
        } catch (Throwable error) {
            failure.set(error);
        }
    }

    private static void stall(ServerSocket server, CountDownLatch accepted) {
        try (Socket socket = server.accept()) {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            accepted.countDown();
            Thread.sleep(500L);
        } catch (EOFException ignored) {
            accepted.countDown();
        } catch (Exception ignored) {
            accepted.countDown();
        }
    }
}
