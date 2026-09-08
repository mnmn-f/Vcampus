package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 宿舍命令统一 ID 请求。 */
public final class DormIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;

    public DormIdRequest(long id) { this.id = id; }
    public long getId() { return id; }
}
