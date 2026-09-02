package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;

import java.sql.Connection;

/** products 表的读写边界。 */
public interface StoreProductRepository {
    ProductPage searchProducts(Connection connection, ProductQuery query);
    ProductDto findProduct(Connection connection, long productId, boolean forUpdate);
    boolean skuExists(Connection connection, String sku, long excludedProductId);
    void insertProduct(Connection connection, ProductWriteRequest request, long actorId);
    void updateProduct(Connection connection, ProductWriteRequest request, long actorId);
    boolean adjustStock(Connection connection, long productId, int delta);
}
