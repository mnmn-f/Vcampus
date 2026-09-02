package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商店分类新增或编辑请求；id 为 0 表示新增。 */
public final class StoreCategoryWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String code;
    private final String name;
    private final boolean active;

    public StoreCategoryWriteRequest(long id, String code, String name, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
    }
    public long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
