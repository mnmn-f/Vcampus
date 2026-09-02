package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商店商品分类的稳定编码与展示名称。 */
public final class StoreCategoryDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String code;
    private final String name;
    private final boolean active;

    public StoreCategoryDto(long id, String code, String name, boolean active) {
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
