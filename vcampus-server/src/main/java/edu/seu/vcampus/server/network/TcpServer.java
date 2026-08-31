package edu.seu.vcampus.server.network;

import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP 服务端监听器，使用有界队列的固定大小线程池承载客户端连接。
 */
public final class TcpServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 8888;
    public static final int DEFAULT_MAX_CONNECTIONS = 32;
    public static final int DEFAULT_CLIENT_READ_TIMEOUT_MILLIS = 30000;

    private static final Logger LOGGER =
            Logger.getLogger(TcpServer.class.getName());

    private final int port;
    private final int maxConnections;
    private final int clientReadTimeoutMillis;
    private final CommandRouter commandRouter;
    private final SessionManager sessionManager;
    private final ThreadPoolExecutor connectionPool;
    private final ClientConnectionRegistry clients = new ClientConnectionRegistry();
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public TcpServer(int port, CommandRouter commandRouter) {
        this(port, DEFAULT_MAX_CONNECTIONS, commandRouter,
                DEFAULT_CLIENT_READ_TIMEOUT_MILLIS);
    }

    public TcpServer(int port, int maxConnections, CommandRouter commandRouter) {
        this(port, maxConnections, commandRouter, DEFAULT_CLIENT_READ_TIMEOUT_MILLIS);
    }

    public TcpServer(int port, int maxConnections, CommandRouter commandRouter,
                     int clientReadTimeoutMillis) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("maxConnections must be positive");
        }
        if (commandRouter == null) {
            throw new IllegalArgumentException("commandRouter is required");
        }
        if (clientReadTimeoutMillis < 0) {
            throw new IllegalArgumentException("client read timeout cannot be negative");
        }
        this.port = port;
        this.maxConnections = maxConnections;
        this.clientReadTimeoutMillis = clientReadTimeoutMillis;
        this.commandRouter = commandRouter;
        this.sessionManager = commandRouter.getSessionManager();
        this.connectionPool = new ThreadPoolExecutor(
                maxConnections, maxConnections, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(maxConnections),
                new ConnectionThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** 在当前线程阻塞监听，直到 {@link #stop()} 被调用。 */
    public void start() throws IOException {
        synchronized (this) {
            bindAndMarkRunning();
        }
        try {
            acceptLoop();
        } finally {
            closeServerSocket();
            clients.closeAll();
            synchronized (this) {
                running = false;
            }
        }
    }

    /** 在后台线程监听，便于 Swing 启动器或集成测试使用。 */
    public synchronized void startAsync() throws IOException {
        bindAndMarkRunning();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    acceptLoop();
                } finally {
                    closeServerSocket();
                    clients.closeAll();
                    synchronized (TcpServer.this) {
                        running = false;
                    }
                }
            }
        }, "vcampus-acceptor");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        if (!running && serverSocket == null) {
            return;
        }
        running = false;
        closeServerSocket();
        clients.closeAll();
        connectionPool.shutdownNow();
        sessionManager.invalidateAll();
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return running;
    }

    public int getBoundPort() {
        ServerSocket current = serverSocket;
        return current == null ? -1 : current.getLocalPort();
    }

    public int getMaxConnections() {
        return maxConnections;
    }
    private void bindAndMarkRunning() throws IOException {
        if (running) {
            throw new IllegalStateException("server is already running");
        }
        if (connectionPool.isShutdown()) {
            throw new IllegalStateException("server cannot be restarted after stop");
        }
        ServerSocket created = new ServerSocket();
        created.setReuseAddress(true);
        created.bind(new InetSocketAddress(port));
        serverSocket = created;
        running = true;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                clients.add(client);
                try {
                    connectionPool.execute(new ClientConnectionHandler(
                            client, commandRouter, sessionManager,
                            clientReadTimeoutMillis, clients.sockets()));
                } catch (RejectedExecutionException ex) {
                    LOGGER.warning("connection capacity reached; rejecting client");
                    clients.remove(client);
                    close(client);
                }
            } catch (SocketException ex) {
                if (running) {
                    LOGGER.log(Level.WARNING, "server socket failed", ex);
                }
                break;
            } catch (IOException ex) {
                if (running) {
                    LOGGER.log(Level.WARNING, "failed to accept client", ex);
                }
            }
        }
    }

    private synchronized void closeServerSocket() {
        ServerSocket current = serverSocket;
        serverSocket = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // 停止路径中无需向调用方传播关闭异常。
            }
        }
    }

    private static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 拒绝连接时尽力释放资源。
        }
    }
}
