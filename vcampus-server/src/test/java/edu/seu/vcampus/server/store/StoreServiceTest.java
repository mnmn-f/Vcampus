package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
/** 商店核心库存、余额、幂等和数据范围测试。 */
public final class StoreServiceTest {
    private InMemoryStoreRecordRepository repository;
    private StoreService service;
    private SessionContext student;
    private SessionContext other;
    private SessionContext manager;
    @Before
    public void setUp() {
        repository = new InMemoryStoreRecordRepository();
        repository.addProduct(new ProductDto(1L, "SKU-1", "纪念杯", "文创", null,
                new BigDecimal("12.50"), 2, "ON_SALE"));
        repository.addProduct(new ProductDto(2L, "SKU-2", "下架商品", "文创", null,
                new BigDecimal("5.00"), 5, "OFF_SALE"));
        repository.addAccount(new AccountDto(1L, 10L, new BigDecimal("20.00"), "ACTIVE"));
        repository.addAccount(new AccountDto(2L, 11L, new BigDecimal("20.00"), "ACTIVE"));
        service = new StoreService(repository,
                new InMemoryStoreTransactionRunner(repository));
        student = session(10L, Role.STUDENT);
        other = session(11L, Role.STUDENT);
        manager = session(99L, Role.STORE_MANAGER);
    }

    @Test
    public void studentsSeeOnlyOnSaleProducts() throws Exception {
        ProductPage result = service.searchProducts(student,
                new ProductQuery(null, null, "OFF_SALE", 1, 20));
        assertEquals(1L, result.getTotal());
        assertEquals("SKU-1", result.getItems().get(0).getSku());
    }

    @Test
    public void managerMaintainsProductsButStudentCannot() throws Exception {
        try {
            service.updateProduct(student, new edu.seu.vcampus.common.dto.store.ProductWriteRequest(
                    1L, "SKU-1", "改名", "文创", null, new BigDecimal("10.00"), 2, "ON_SALE"));
            fail("student must not maintain products");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        assertEquals("改名", service.updateProduct(manager,
                new edu.seu.vcampus.common.dto.store.ProductWriteRequest(1L, "SKU-1", "改名",
                        "文创", null, new BigDecimal("10.00"), 2, "ON_SALE")).getName());
    }

    @Test
    public void paymentAtomicallyChangesOrderStockBalanceAndLedger() throws Exception {
        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto order = service.createOrder(student);
        assertEquals(2, repository.getStock(1L));
        OrderDto paid = service.payOrder(student, new PaymentRequest(order.getId(), "PAY-1"));
        assertEquals("PAID", paid.getStatus());
        assertEquals(1, repository.getStock(1L));
        assertEquals(new BigDecimal("7.50"), repository.getAccount(10L).getBalance());
        assertEquals(1L, service.getAccountLedger(student, null).getTotal());
    }

    @Test
    public void duplicatePaymentIsIdempotent() throws Exception {
        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto order = service.createOrder(student);
        service.payOrder(student, new PaymentRequest(order.getId(), "PAY-2"));
        service.payOrder(student, new PaymentRequest(order.getId(), "PAY-2"));
        assertEquals(1, repository.getStock(1L));
        assertEquals(new BigDecimal("7.50"), repository.getAccount(10L).getBalance());
    }

    @Test
    public void insufficientStockLeavesPaymentStateUnchanged() throws Exception {
        service.addCartItem(student, new CartItemRequest(1L, 3));
        OrderDto order = service.createOrder(student);
        try {
            service.payOrder(student, new PaymentRequest(order.getId(), "PAY-3"));
            fail("insufficient stock must fail");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.CONFLICT, ex.getResultCode());
        }
        assertEquals(2, repository.getStock(1L));
        assertEquals(new BigDecimal("20.00"), repository.getAccount(10L).getBalance());
        assertEquals("CREATED", repository.getOrder(order.getId()).getStatus());
    }

    @Test
    public void paymentCannotCrossUserOrderBoundary() throws Exception {
        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto order = service.createOrder(student);
        try {
            service.payOrder(other, new PaymentRequest(order.getId(), "PAY-4"));
            fail("other user must not pay order");
        } catch (StoreServiceException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
        }
    }

    @Test
    public void concurrentPaymentsCannotOversellStock() throws Exception {
        repository.addProduct(new ProductDto(3L, "SKU-3", "限量商品", "文创", null,
                new BigDecimal("3.00"), 1, "ON_SALE"));
        StoreService otherService = new StoreService(repository,
                new InMemoryStoreTransactionRunner(repository));
        service.addCartItem(student, new CartItemRequest(3L, 1));
        otherService.addCartItem(other, new CartItemRequest(3L, 1));
        final OrderDto first = service.createOrder(student);
        final OrderDto second = otherService.createOrder(other);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(new PaymentCall(service, student, first));
            Future<Boolean> b = pool.submit(new PaymentCall(otherService, other, second));
            assertEquals(1, (a.get() ? 1 : 0) + (b.get() ? 1 : 0));
            assertEquals(0, repository.getStock(3L));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void inMemoryTransactionRestoresStateAfterFailure() throws Exception {
        InMemoryStoreTransactionRunner runner = new InMemoryStoreTransactionRunner(repository);
        try {
            runner.execute(new TransactionWork<Object>() {
                @Override public Object execute(java.sql.Connection connection) {
                    repository.adjustStock(connection, 1L, -1);
                    throw new IllegalStateException("forced failure");
                }
            });
            fail("failed transaction must throw");
        } catch (IllegalStateException expected) {
            assertEquals(2, repository.getStock(1L));
        }
    }

    @Test
    public void managerStatusFlowCompletesAndRefundsAtomically() throws Exception {
        service.addCartItem(student, new CartItemRequest(1L, 1));
        OrderDto order = service.createOrder(student);
        service.payOrder(student, new PaymentRequest(order.getId(), "PAY-5"));
        assertEquals("COMPLETED", service.updateOrderStatus(manager,
                new OrderStatusUpdateRequest(order.getId(), "COMPLETED")).getStatus());
        assertEquals("REFUNDED", service.updateOrderStatus(manager,
                new OrderStatusUpdateRequest(order.getId(), "REFUNDED")).getStatus());
        assertEquals(2, repository.getStock(1L));
        assertEquals(new BigDecimal("20.00"), repository.getAccount(10L).getBalance());
    }

    private static final class PaymentCall implements Callable<Boolean> {
        private final StoreService service;
        private final SessionContext session;
        private final OrderDto order;

        private PaymentCall(StoreService service, SessionContext session, OrderDto order) {
            this.service = service;
            this.session = session;
            this.order = order;
        }

        @Override public Boolean call() {
            try {
                service.payOrder(session, new PaymentRequest(order.getId(), "CONCURRENT-" + order.getId()));
                return Boolean.TRUE;
            } catch (StoreServiceException ex) {
                return Boolean.FALSE;
            }
        }
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("store-" + id + "-" + role.name(), id, "user" + id,
                "用户" + id, EnumSet.of(role), role);
    }
}
