package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.store.StoreCategoryDto;

/** 商店分类下拉项：显示名称，提交时取稳定 code。 */
final class StoreCategoryOption {
    private final String code;
    private final String label;

    private StoreCategoryOption(String code, String label) {
        this.code = code;
        this.label = label;
    }

    static StoreCategoryOption all() { return new StoreCategoryOption(null, "全部分类"); }
    static StoreCategoryOption empty() { return new StoreCategoryOption(null, "请选择分类"); }

    static StoreCategoryOption from(StoreCategoryDto value) {
        if (value == null) return empty();
        String label = value.getName() == null || value.getName().trim().isEmpty()
                ? RealUi.status(value.getCode()) : value.getName().trim();
        return new StoreCategoryOption(value.getCode(), label);
    }

    static StoreCategoryOption fallback(String code) {
        return new StoreCategoryOption(code, RealUi.status(code));
    }

    String getCode() { return code; }
    @Override public String toString() { return label; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof StoreCategoryOption)) return false;
        StoreCategoryOption value = (StoreCategoryOption) other;
        return code == null ? value.code == null : code.equals(value.code);
    }
    @Override public int hashCode() { return code == null ? 0 : code.hashCode(); }
}
