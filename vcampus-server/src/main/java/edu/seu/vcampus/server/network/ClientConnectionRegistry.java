package edu.seu.vcampus.server.network;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 当前 TCP 客户端集合；停止服务时主动关闭阻塞连接。 */
final class ClientConnectionRegistry {
    private final Set<Socket> sockets = Collections.newSetFromMap(
            new ConcurrentHashMap<Socket, Boolean>());

    void add(Socket socket) {
        sockets.add(socket);
    }

    void remove(Socket socket) {
        sockets.remove(socket);
    }

    Set<Socket> sockets() {
        return sockets;
    }

    void closeAll() {
        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 停止时尽力释放全部连接。
            }
        }
        sockets.clear();
    }
}
