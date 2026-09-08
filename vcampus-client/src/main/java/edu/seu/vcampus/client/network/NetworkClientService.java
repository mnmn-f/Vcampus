package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;

import java.io.IOException;

/** 为各业务客户端服务统一补充会话令牌和错误映射。 */
public final class NetworkClientService {
    private final ClientGateway gateway;
    private String sessionToken;

    public NetworkClientService(ClientGateway gateway) {
        if (gateway == null) {
            throw new IllegalArgumentException("gateway 不能为空");
        }
        this.gateway = gateway;
    }

    public Message request(String command, java.io.Serializable payload)
            throws NetworkClientException {
        return requestWithToken(command, payload, sessionToken);
    }

    /** 文件分块请求可显式绑定创建传输任务时的会话令牌。 */
    public Message requestWithToken(String command, java.io.Serializable payload, String token)
            throws NetworkClientException {
        Message request = Message.request(command, token, payload);
        try {
            Message response = gateway.send(request);
            if (response == null) {
                throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "服务器没有返回结果");
            }
            if (!response.isSuccess()) {
                throw new NetworkClientException(response.getResultCode(), response.getUserMessage());
            }
            return response;
        } catch (NetworkClientException e) {
            throw e;
        } catch (IOException e) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR,
                    "网络连接中断，请稍后重试", e);
        } catch (ClassNotFoundException e) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR,
                    "通信对象版本不兼容", e);
        }
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public boolean isConnected() {
        return gateway.isConnected();
    }

    public void close() {
        gateway.close();
        sessionToken = null;
    }
}
