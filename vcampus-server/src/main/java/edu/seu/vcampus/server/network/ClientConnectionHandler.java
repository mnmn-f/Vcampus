package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.router.StreamWriter;
import edu.seu.vcampus.server.security.SessionManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 单个客户端连接的读取、路由和响应循环。
 *
 * <p>ObjectOutputStream 必须先创建并 flush，双方再创建 ObjectInputStream，
 * 否则两端可能同时等待对象流头而死锁。</p>
 */
public final class ClientConnectionHandler implements Runnable {
    private static final Logger LOGGER =
            Logger.getLogger(ClientConnectionHandler.class.getName());

    private final Socket socket;
    private final CommandRouter commandRouter;
    private final SessionManager sessionManager;
    private final int readTimeoutMillis;
    private final Set<Socket> activeSockets;

    public ClientConnectionHandler(Socket socket, CommandRouter commandRouter) {
        this(socket, commandRouter,
                commandRouter == null ? null : commandRouter.getSessionManager(), 0, null);
    }

    public ClientConnectionHandler(Socket socket, CommandRouter commandRouter,
                                   SessionManager sessionManager) {
        this(socket, commandRouter, sessionManager, 0, null);
    }

    public ClientConnectionHandler(Socket socket, CommandRouter commandRouter,
                                   SessionManager sessionManager, int readTimeoutMillis) {
        this(socket, commandRouter, sessionManager, readTimeoutMillis, null);
    }

    ClientConnectionHandler(Socket socket, CommandRouter commandRouter,
                            SessionManager sessionManager, int readTimeoutMillis,
                            Set<Socket> activeSockets) {
        if (socket == null || commandRouter == null || sessionManager == null) {
            throw new IllegalArgumentException("connection dependencies are required");
        }
        if (readTimeoutMillis < 0) {
            throw new IllegalArgumentException("read timeout cannot be negative");
        }
        this.socket = socket;
        this.commandRouter = commandRouter;
        this.sessionManager = sessionManager;
        this.readTimeoutMillis = readTimeoutMillis;
        this.activeSockets = activeSockets;
    }

    @Override
    public void run() {
        String ownedSessionToken = null;
        try {
            socket.setTcpNoDelay(true);
            if (readTimeoutMillis > 0) socket.setSoTimeout(readTimeoutMillis);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new SafeObjectInputStream(socket.getInputStream());
            while (!socket.isClosed()) {
                Object incoming;
                try {
                    incoming = input.readObject();
                } catch (EOFException ex) {
                    break;
                } catch (SocketTimeoutException ex) {
                    LOGGER.log(Level.FINE, "idle client connection timed out");
                    break;
                }
                if (!(incoming instanceof Message)) {
                    LOGGER.warning("ignored non-message payload from client");
                    break;
                }
                Message request = (Message) incoming;
                if (commandRouter.isStreamingCommand(request.getCommand())) {
                    final ObjectOutputStream streamOutput = output;
                    commandRouter.routeStream(request, new StreamWriter() {
                        @Override public void write(Message message) {
                            writeMessage(streamOutput, message);
                        }
                    });
                    continue;
                }
                Message response = commandRouter.route(request);
                if (response != null) {
                    writeMessage(output, response);
                    if (Commands.AUTH_LOGIN.equals(response.getCommand())
                            && response.getPayload() instanceof LoginResult) {
                        if (ownedSessionToken != null) {
                            sessionManager.invalidate(ownedSessionToken);
                        }
                        ownedSessionToken = ((LoginResult) response.getPayload())
                                .getSessionToken();
                    } else if (Commands.AUTH_LOGOUT.equals(response.getCommand())
                            && response.isSuccess()) {
                        ownedSessionToken = null;
                    }
                }
            }
        } catch (StreamWriteException ex) {
            LOGGER.log(Level.FINE, "client connection write failed", ex.getCause());
        } catch (SocketException ex) {
            LOGGER.log(Level.FINE, "client connection closed", ex);
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "client connection failed", ex);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, "client sent an unknown serialized class", ex);
        } finally {
            closeSocket();
            if (activeSockets != null) activeSockets.remove(socket);
        }
    }

    private static void writeMessage(ObjectOutputStream output, Message message) {
        if (message == null) return;
        synchronized (output) {
            try {
                output.writeObject(message);
                output.flush();
                output.reset();
            } catch (IOException ex) {
                throw new StreamWriteException(ex);
            }
        }
    }

    private static final class StreamWriteException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private StreamWriteException(IOException cause) { super(cause); }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 连接已结束，关闭失败不应影响线程池回收。
        }
    }
}
