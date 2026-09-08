package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 仅携带一个服务端资源编号的请求。 */
public final class LibraryIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;

    public LibraryIdRequest(long id) { this.id = id; }
    public long getId() { return id; }
}
