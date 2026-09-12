package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductStatus;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.sql.Connection;
import java.util.Locale;

/** 商品检索、详情与商店管理员维护。 */
final class StoreProductService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreProductService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        if (repository == null || transactions == null) {
            throw new IllegalArgumentException("store product dependencies are required");
        }
        this.repository = repository;
        this.transactions = transactions;
    }

    ProductPage search(final SessionContext session, ProductQuery query)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_READ);
        final ProductQuery safe = visibleQuery(session, query);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<ProductPage>() {
                    @Override public ProductPage execute(Connection c) {
                        return repository.searchProducts(c, safe);
                    }
                });
    }

    ProductDto detail(final SessionContext session, final long productId)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_READ);
        StoreServiceSupport.positiveId(productId, "商品编号");
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<ProductDto>() {
                    @Override public ProductDto execute(Connection c) throws StoreServiceException {
                        ProductDto found = repository.findProduct(c, productId, false);
                        if (found == null || !visible(session, found)) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "商品不存在");
                        }
                        return found;
                    }
                });
    }

    ProductDto create(final SessionContext session, final ProductWriteRequest request)
            throws StoreServiceException {
        maintainPermission(session);
        validate(request, false);
        StoreImagePolicy.requireExisting(request, null);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<ProductDto>() {
                    @Override public ProductDto execute(Connection c) throws StoreServiceException {
                        if (repository.skuExists(c, request.getSku(), 0L)) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "商品编码已存在");
                        }
                        repository.insertProduct(c, request, session.getUserId());
                        return findBySku(c, request.getSku());
                    }
                });
    }

    ProductDto update(final SessionContext session, final ProductWriteRequest request)
            throws StoreServiceException {
        maintainPermission(session);
        validate(request, true);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<ProductDto>() {
                    @Override public ProductDto execute(Connection c) throws StoreServiceException {
                        ProductDto previous = repository.findProduct(c, request.getId(), true);
                        if (previous == null) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "商品不存在");
                        }
                        StoreImagePolicy.requireExisting(request, previous.getImageUrl());
                        if (repository.skuExists(c, request.getSku(), request.getId())) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "商品编码已存在");
                        }
                        repository.updateProduct(c, request, session.getUserId());
                        return repository.findProduct(c, request.getId(), false);
                    }
                });
    }

    ProductDto save(SessionContext session, ProductWriteRequest request)
            throws StoreServiceException {
        return request != null && request.getId() > 0 ? update(session, request) : create(session, request);
    }

    byte[] image(SessionContext session, String reference) throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_READ);
        if (!StoreImagePolicy.reference(reference)) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "图片引用不正确");
        return StoreServiceSupport.inTransaction(transactions, c -> {
            byte[] data = repository.findProductImage(c, reference, session.getActiveRole() == Role.STORE_MANAGER);
            if (data == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "图片不存在或商品已下架");
            return data;
        });
    }

    ProductDto adjustStock(final SessionContext session, final StockAdjustRequest request)
            throws StoreServiceException {
        maintainPermission(session);
        if (request == null || request.getProductId() <= 0 || request.getDelta() == 0
                || request.getDelta() > 999999 || request.getDelta() < -999999) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "库存调整参数不正确");
        }
        StoreServiceSupport.maxLength(request.getRemark(), 500, "备注");
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<ProductDto>() {
                    @Override public ProductDto execute(Connection c) throws StoreServiceException {
                        if (repository.findProduct(c, request.getProductId(), true) == null) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "商品不存在");
                        }
                        if (!repository.adjustStock(c, request.getProductId(), request.getDelta())) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "库存不能为负数");
                        }
                        return repository.findProduct(c, request.getProductId(), false);
                    }
                });
    }

    private ProductDto findBySku(Connection c, String sku) throws StoreServiceException {
        ProductPage page = repository.searchProducts(c, new ProductQuery(sku, null, null, 1, 2));
        for (ProductDto product : page.getItems()) if (sku.equals(product.getSku())) return product;
        throw new StoreServiceException(ResultCodes.INTERNAL_ERROR, "商品保存后无法读取");
    }

    private static ProductQuery visibleQuery(SessionContext session, ProductQuery query) {
        ProductQuery q = query == null ? new ProductQuery() : query;
        if (session.getActiveRole() == Role.STORE_MANAGER) return q;
        return new ProductQuery(q.getKeyword(), q.getCategory(), "ON_SALE",
                q.getPage(), q.getPageSize());
    }

    private static boolean visible(SessionContext session, ProductDto product) {
        return session.getActiveRole() == Role.STORE_MANAGER
                || "ON_SALE".equalsIgnoreCase(product.getStatus());
    }

    private static void maintainPermission(SessionContext session) throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_MANAGE);
        StoreServiceSupport.requireRole(session, Role.STORE_MANAGER);
    }

    private static void validate(ProductWriteRequest request, boolean update)
            throws StoreServiceException {
        if (request == null || (update && request.getId() <= 0)) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "商品参数不正确");
        }
        StoreServiceSupport.required(request.getSku(), "商品编码");
        StoreServiceSupport.required(request.getName(), "商品名称");
        StoreServiceSupport.maxLength(request.getSku(), 64, "商品编码");
        StoreServiceSupport.maxLength(request.getName(), 200, "商品名称");
        StoreServiceSupport.maxLength(request.getCategory(), 80, "商品分类");
        StoreServiceSupport.maxLength(request.getDescription(), 65535, "商品描述");
        StoreImagePolicy.validate(request);
        StoreServiceSupport.money(request.getPrice(), "商品", true);
        if (request.getStockQty() < 0) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "库存不能为负数");
        }
        validStatus(request.getStatus());
    }

    private static void validStatus(String value) throws StoreServiceException {
        try {
            ProductStatus.valueOf(StoreServiceSupport.required(value, "商品状态").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "商品状态不正确");
        }
    }

}
