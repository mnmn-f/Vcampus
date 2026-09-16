package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 服务端商品图片的版本引用，不是本地文件路径。 */
public final class ProductImageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String reference;
    private final String variant;
    public ProductImageRequest(String reference) { this(reference, "FULL"); }
    public ProductImageRequest(String reference, String variant) {
        this.reference = reference;
        this.variant = variant == null ? "FULL" : variant;
    }
    public String getReference() { return reference; }
    public String getVariant() { return variant; }
}
