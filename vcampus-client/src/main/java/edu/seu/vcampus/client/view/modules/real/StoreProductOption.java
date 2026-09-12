package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.store.ProductDto;

/** 促销指定商品下拉项：显示商品信息，提交时取商品主键。 */
final class StoreProductOption {
    private final long id;
    private final String label;

    private StoreProductOption(long id, String label) {
        this.id = id;
        this.label = label;
    }

    static StoreProductOption empty() { return new StoreProductOption(0L, "请选择商品"); }
    static StoreProductOption from(ProductDto value) {
        if (value == null) return empty();
        String name = RealUi.text(value.getName());
        String sku = RealUi.optional(value.getSku());
        return new StoreProductOption(value.getId(), sku == null ? name : name + "（" + sku + "）");
    }
    static StoreProductOption fallback(long id) { return new StoreProductOption(id, "当前商品"); }
    long getId() { return id; }
    @Override public String toString() { return label; }
    @Override public boolean equals(Object other) {
        return other instanceof StoreProductOption && id == ((StoreProductOption) other).id;
    }
    @Override public int hashCode() { return Long.valueOf(id).hashCode(); }
}
