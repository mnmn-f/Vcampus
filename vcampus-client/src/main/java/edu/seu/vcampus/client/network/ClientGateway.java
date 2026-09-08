package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.protocol.Message;

import java.io.IOException;

/** 客户端到服务器的最小网络边界。 */
public interface ClientGateway {
    Message send(Message request) throws IOException, ClassNotFoundException;

    boolean isConnected();

    void close();
}
