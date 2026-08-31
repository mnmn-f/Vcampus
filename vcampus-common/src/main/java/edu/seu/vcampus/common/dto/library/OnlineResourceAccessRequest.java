package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 线上资源访问请求；身份和客户端地址只由服务端会话提供。 */
public final class OnlineResourceAccessRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long resourceId;

    public OnlineResourceAccessRequest(long resourceId) {
        this.resourceId = resourceId;
    }

    public long getResourceId() {
        return resourceId;
    }
}
