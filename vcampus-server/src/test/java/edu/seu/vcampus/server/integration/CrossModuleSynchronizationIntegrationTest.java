package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.BorrowRequest;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 多客户端显式刷新必须读取同一服务端最新状态；本人范围仍由令牌决定。 */
public final class CrossModuleSynchronizationIntegrationTest {
    private IntegrationFixture fixture;

    @Before public void setUp() { fixture = new IntegrationFixture(); }

    @Test public void profileUpdateIsVisibleToAnotherSessionWithoutChangingAcademicData() throws Exception {
        String editor = login("student2");
        String reader = login("student2");
        Message saved = route(IdentityCommands.PROFILE_UPDATE, editor,
                new ProfileUpdateRequest("学生二的新显示名", "two@seu.edu.cn", "13800138000"));
        assertTrue(saved.isSuccess());
        ProfileDto profile = (ProfileDto) route(IdentityCommands.PROFILE_SELF, reader, null).getPayload();
        assertEquals("学生二的新显示名", profile.getDisplayName());
        assertFalse("身份显示名不应伪造学籍档案", fixture.students.profileExists(null, 2L));
    }

    @Test public void profileRejectsNonNumericPhoneBeforePersisting() {
        String token = login("student2");
        Message response = route(IdentityCommands.PROFILE_UPDATE, token,
                new ProfileUpdateRequest("学生二", null, "138-0013-8000"));
        assertEquals(ResultCodes.INVALID_INPUT, response.getResultCode());
    }

    @Test public void libraryBorrowAndReturnRefreshesInventoryForAnotherStudent() {
        fixture.books.seed(new BookDetail(51L, "978-sync", "同步测试书", "作者", "出版社",
                "计算机", 1, 1, "A-1", "", "ON_SHELF", LocalDateTime.now(),
                LocalDateTime.now()));
        String borrower = login("student2");
        String observer = login("student3");
        BorrowRecordView record = payload(route(LibraryCommands.BOOK_BORROW, borrower,
                new BorrowRequest(51L)), BorrowRecordView.class);
        BookDetail borrowed = payload(route(LibraryCommands.BOOK_DETAIL, observer,
                new LibraryIdRequest(51L)), BookDetail.class);
        assertEquals(0, borrowed.getAvailableCopies());
        assertEquals(LibraryCommands.BOOK_UNAVAILABLE,
                route(LibraryCommands.BOOK_BORROW, observer, new BorrowRequest(51L)).getResultCode());
        assertTrue(route(LibraryCommands.BOOK_RETURN, borrower,
                new ReturnBorrowRequest(record.getId())).isSuccess());
        BookDetail returned = payload(route(LibraryCommands.BOOK_DETAIL, observer,
                new LibraryIdRequest(51L)), BookDetail.class);
        assertEquals(1, returned.getAvailableCopies());
    }

    @Test public void cartProductAndOrderStateAreImmediatelyConsistentAcrossUsers() {
        String first = login("student2");
        String second = login("student2");
        CartDto added = payload(route(StoreCommands.CART_ADD_ITEM, first,
                new CartItemRequest(1L, 1)), CartDto.class);
        assertEquals(1, added.getItems().size());
        assertEquals(1, payload(route(StoreCommands.CART_GET, second, null), CartDto.class)
                .getItems().size());
        assertEquals(0, payload(route(StoreCommands.CART_REMOVE_ITEM, first, Long.valueOf(1L)), CartDto.class)
                .getItems().size());
        assertEquals(0, payload(route(StoreCommands.CART_GET, second, null), CartDto.class)
                .getItems().size());
        assertEquals(1, payload(route(StoreCommands.CART_ADD_ITEM, second,
                new CartItemRequest(1L, 1)), CartDto.class).getItems().size());

        OrderDto order = payload(route(StoreCommands.ORDER_CREATE, first, null), OrderDto.class);
        assertEquals("PAID", payload(route(StoreCommands.ORDER_PAY, first,
                new PaymentRequest(order.getId(), "sync-pay-1")), OrderDto.class).getStatus());
        Message duplicate = route(StoreCommands.ORDER_PAY, first,
                new PaymentRequest(order.getId(), "sync-pay-2"));
        assertEquals(ResultCodes.CONFLICT, duplicate.getResultCode());
        assertEquals(new BigDecimal("95.00"), fixture.store.getAccount(2L).getBalance());
        OrderPage refreshed = payload(route(StoreCommands.ORDER_MINE, second,
                new OrderQuery(null, null, 1, 20)), OrderPage.class);
        assertEquals("PAID", refreshed.getItems().get(0).getStatus());
    }

    @Test public void managerProductAndShippingChangesAppearOnStudentRefresh() {
        String manager = managerLogin();
        String student = login("student2");
        ProductWriteRequest edit = new ProductWriteRequest(1L, "SKU-1", "更新后的商品",
                "测试", null, new BigDecimal("6.00"), 5, "ON_SALE");
        assertTrue(route(StoreCommands.PRODUCT_UPDATE, manager, edit).isSuccess());
        ProductPage products = payload(route(StoreCommands.PRODUCT_SEARCH, student,
                new ProductQuery("更新后的商品", null, "ON_SALE", 1, 20)), ProductPage.class);
        assertEquals("更新后的商品", products.getItems().get(0).getName());
        route(StoreCommands.CART_ADD_ITEM, student, new CartItemRequest(1L, 1));
        OrderDto order = payload(route(StoreCommands.ORDER_CREATE, student, null), OrderDto.class);
        assertEquals("PAID", payload(route(StoreCommands.ORDER_PAY, student,
                new PaymentRequest(order.getId(), "sync-shipping-pay")), OrderDto.class).getStatus());
        OrderShippingUpdateRequest shipping = new OrderShippingUpdateRequest(order.getId(),
                "SHIPPED", "YT-SYNC-1", "已交承运方");
        assertTrue(route(StoreCommands.ORDER_SHIPPING_UPDATE, manager, shipping).isSuccess());
        OrderDto refreshed = payload(route(StoreCommands.ORDER_DETAIL, student,
                Long.valueOf(order.getId())), OrderDto.class);
        assertEquals("SHIPPED", refreshed.getShippingStatus());
        assertEquals("YT-SYNC-1", refreshed.getTrackingNo());
    }

    private Message route(String command, String token, java.io.Serializable payload) {
        return fixture.router.route(Message.request(command, token, payload));
    }

    private String login(String account) {
        Message response = fixture.router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest(account, IntegrationFixture.MULTI_PASSWORD)));
        assertTrue(response.isSuccess());
        return ((LoginResult) response.getPayload()).getSessionToken();
    }

    private String managerLogin() {
        String token = login("multi");
        Message switched = route(Commands.AUTH_SWITCH_ROLE, token,
                new SwitchRoleRequest(Role.STORE_MANAGER));
        assertTrue(switched.isSuccess());
        return token;
    }

    private static <T> T payload(Message response, Class<T> type) {
        assertTrue(response.isSuccess());
        assertTrue(type.isInstance(response.getPayload()));
        return type.cast(response.getPayload());
    }
}
