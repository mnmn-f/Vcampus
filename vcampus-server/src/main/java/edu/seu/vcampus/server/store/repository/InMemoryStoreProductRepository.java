package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;

import org.threeten.bp.LocalDateTime;
import java.sql.Connection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 商品内存仓储，供无数据库测试和本地演示使用。 */
final class InMemoryStoreProductRepository implements StoreProductRepository {
    private final InMemoryStoreState state;

    InMemoryStoreProductRepository(InMemoryStoreState state) { this.state = state; }

    @Override public byte[] findProductImage(Connection c, String reference, boolean manager) {
        synchronized (state) {
            for (ProductDto product : state.products.values()) if (reference.equals(product.getImageUrl())
                    && (manager || "ON_SALE".equals(product.getStatus()))) {
                byte[] bytes = state.productImages.get(product.getId()); return bytes == null ? null : bytes.clone();
            }
            return null;
        }
    }

    @Override
    public ProductPage searchProducts(Connection c, ProductQuery query) {
        ProductQuery q = query == null ? new ProductQuery() : query;
        synchronized (state) {
            List<ProductDto> found = new ArrayList<ProductDto>();
            for (ProductDto product : state.products.values()) {
                if (matches(product, q)) found.add(product);
            }
            Collections.sort(found, new Comparator<ProductDto>() {
                @Override public int compare(ProductDto a, ProductDto b) {
                    int result = a.getName().compareTo(b.getName());
                    return result == 0 ? Long.compare(a.getId(), b.getId()) : result;
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new ProductPage(new ArrayList<ProductDto>(found.subList(from, to)),
                    q.getPage(), q.getPageSize(), found.size());
        }
    }

    @Override
    public ProductDto findProduct(Connection c, long id, boolean lock) {
        synchronized (state) { return state.products.get(id); }
    }

    @Override
    public boolean skuExists(Connection c, String sku, long excludedId) {
        synchronized (state) {
            for (ProductDto product : state.products.values()) {
                if (product.getId() != excludedId && product.getSku().equals(sku)) return true;
            }
            return false;
        }
    }

    @Override
    public void insertProduct(Connection c, ProductWriteRequest request, long actorId) {
        synchronized (state) {
            long id = request.getId() > 0 ? request.getId() : state.productSequence++;
            if (request.getImageData() != null) state.productImages.put(id, request.getImageData());
            LocalDateTime now = LocalDateTime.now();
            state.products.put(id, new ProductDto(id, request.getSku(), request.getName(),
                    request.getCategory(), request.getDescription(), request.getPrice(),
                    request.getStockQty(), request.getStatus(), request.getImageUrl(),
                    BigDecimal.ZERO, 0L, actorId, now, now));
        }
    }

    @Override
    public void updateProduct(Connection c, ProductWriteRequest request, long actorId) {
        synchronized (state) {
            ProductDto old = state.products.get(request.getId());
            if (old == null) throw new StoreRepositoryException("商品不存在");
            if (request.getImageData() != null) state.productImages.put(request.getId(), request.getImageData());
            else if (!java.util.Objects.equals(old.getImageUrl(), request.getImageUrl())) state.productImages.remove(request.getId());
            state.products.put(request.getId(), new ProductDto(request.getId(), request.getSku(),
                    request.getName(), request.getCategory(), request.getDescription(),
                    request.getPrice(), request.getStockQty(), request.getStatus(),
                    request.getImageUrl(), old.getRatingAverage(), old.getRatingCount(),
                    old.getCreatedBy(), old.getCreatedAt(), LocalDateTime.now()));
        }
    }

    @Override
    public boolean adjustStock(Connection c, long productId, int delta) {
        synchronized (state) {
            ProductDto old = state.products.get(productId);
            if (old == null || (long) old.getStockQty() + delta < 0) return false;
            replaceStock(old, old.getStockQty() + delta);
            return true;
        }
    }

    @Override
    public void updateRating(Connection c, long productId, BigDecimal average, long count) {
        synchronized (state) {
            ProductDto old = state.products.get(productId);
            if (old == null) throw new StoreRepositoryException("商品不存在");
            state.products.put(productId, state.withRating(old, average, count));
        }
    }

    void add(ProductDto product) {
        synchronized (state) {
            state.products.put(product.getId(), product);
            state.productSequence = Math.max(state.productSequence, product.getId() + 1L);
        }
    }

    private static boolean matches(ProductDto product, ProductQuery q) {
        if (q.getCategory() != null && !q.getCategory().equals(product.getCategory())) return false;
        if (q.getStatus() != null && !q.getStatus().equalsIgnoreCase(product.getStatus())) return false;
        if (q.getKeyword() == null) return true;
        String value = q.getKeyword().toLowerCase(Locale.ROOT);
        return contains(product.getSku(), value) || contains(product.getName(), value)
                || contains(product.getDescription(), value);
    }

    private void replaceStock(ProductDto old, int stock) {
        state.products.put(old.getId(), state.withStock(old, stock));
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
