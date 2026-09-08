package edu.seu.vcampus.common.dto.store;

import java.util.List;

/** 商品分页结果。 */
public final class ProductPage extends StorePage<ProductDto> {
    private static final long serialVersionUID = 1L;

    public ProductPage(List<ProductDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public ProductPage(int page, int pageSize, long total, List<ProductDto> items) {
        this(items, page, pageSize, total);
    }

    public List<ProductDto> getProducts() { return getItems(); }
}
