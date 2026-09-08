package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.CartLine;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.sql.Connection;
import java.util.List;

/** 学生购物车读取、添加、修改和移除。 */
final class StoreCartService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreCartService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    CartDto get(final SessionContext session) throws StoreServiceException {
        requirePurchase(session);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<CartDto>() {
                    @Override public CartDto execute(Connection c) {
                        return repository.findCart(c, session.getUserId());
                    }
                });
    }

    CartDto add(final SessionContext session, final CartItemRequest request)
            throws StoreServiceException {
        requirePurchase(session);
        validate(request);
        return change(session, request, 1);
    }

    CartDto update(final SessionContext session, final CartItemRequest request)
            throws StoreServiceException {
        requirePurchase(session);
        validate(request);
        return change(session, request, 2);
    }

    CartDto remove(final SessionContext session, final long productId)
            throws StoreServiceException {
        requirePurchase(session);
        StoreServiceSupport.positiveId(productId, "商品编号");
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<CartDto>() {
                    @Override public CartDto execute(Connection c) {
                        repository.removeCartItem(c, session.getUserId(), productId);
                        return repository.findCart(c, session.getUserId());
                    }
                });
    }

    private CartDto change(final SessionContext session, final CartItemRequest request,
                           final int operation) throws StoreServiceException {
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<CartDto>() {
                    @Override public CartDto execute(Connection c) throws StoreServiceException {
                        ProductDto product = product(c, request.getProductId());
                        if (!"ON_SALE".equalsIgnoreCase(product.getStatus())) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "商品当前不可购买");
                        }
                        repository.ensureCart(c, session.getUserId());
                        if (operation == 1) {
                            int old = quantity(repository.findCartLines(c, session.getUserId(), false),
                                    request.getProductId());
                            if ((long) old + request.getQuantity() > 999999L) {
                                throw new StoreServiceException(ResultCodes.INVALID_INPUT, "购物车数量超限");
                            }
                            repository.addCartItem(c, session.getUserId(), request);
                        } else {
                            repository.updateCartItem(c, session.getUserId(), request);
                        }
                        return repository.findCart(c, session.getUserId());
                    }
                });
    }

    private ProductDto product(Connection c, long productId) throws StoreServiceException {
        ProductDto found = repository.findProduct(c, productId, false);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "商品不存在");
        return found;
    }

    private static int quantity(List<CartLine> lines, long productId) {
        for (CartLine line : lines) if (line.getProductId() == productId) return line.getQuantity();
        return 0;
    }

    private static void requirePurchase(SessionContext session) throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
    }

    private static void validate(CartItemRequest request) throws StoreServiceException {
        if (request == null) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "购物车参数不正确");
        StoreServiceSupport.positiveId(request.getProductId(), "商品编号");
        StoreServiceSupport.quantity(request.getQuantity());
    }
}
