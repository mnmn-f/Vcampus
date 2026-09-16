package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.auth.NetworkAuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.modules.real.AsyncTask;
import edu.seu.vcampus.client.view.modules.real.RealStorePage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import org.junit.Assume;
import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import edu.seu.vcampus.common.protocol.Message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/** 可选的真实服务器只读验收，覆盖学生进入商店时会并行加载的全部数据。 */
public final class StoreLiveReadAcceptanceTest {
    @Test
    public void studentStoreReadsFromRunningServer() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.store.acceptance"));
        SocketClientGateway gateway = new SocketClientGateway("127.0.0.1",
                Integer.getInteger("vcampus.test.server.port", 8888), 3000, 15000);
        NetworkAuthClientService auth = new NetworkAuthClientService(gateway);
        try {
            ClientSession session = new ClientSession();
            session.open(auth.login("demo_student", "student123"));
            NetworkStoreClientService store = new NetworkStoreClientService(gateway, session);
            ProductPage products = store.searchProducts(new ProductQuery());
            assertNotNull(products);
            assertNotNull(store.listCategories());
            CartDto cart = store.getCart();
            assertNotNull(cart);
            assertNotNull(store.getOwnOrders(new OrderQuery()));
            CouponPage coupons = store.listCoupons();
            assertNotNull(coupons);
            assertNotNull(store.reviewCandidates(new ProductReviewQuery(0L)));
            assertNotNull(store.getAccount());
            assertNotNull(store.getAccountLedger(new AccountLedgerQuery()));
            assertNotNull(store.checkoutPreview(null));
            for (CouponDto coupon : coupons.getItems()) {
                if (!coupon.isUsed() && (coupon.getThreshold() == null
                        || cart.getTotalAmount().compareTo(coupon.getThreshold()) >= 0)) {
                    CouponDto usable = coupon.isClaimed() ? coupon
                            : store.claimCoupon(new CouponClaimRequest(coupon.getCode()));
                    assertNotNull(store.checkoutPreview(usable.getCode()));
                    break;
                }
            }
            if (!products.getItems().isEmpty()) {
                ProductDto product = products.getItems().get(0);
                assertNotNull(store.getProductDetail(product.getId()));
                assertNotNull(store.listReviews(new ProductReviewQuery(product.getId())));
                if (product.getImageUrl() != null
                        && product.getImageUrl().startsWith("store-image:")) {
                    assertNotNull(store.getProductImage(product.getImageUrl()));
                }
            }
        } finally {
            try { auth.logout(); } finally { gateway.close(); }
        }
    }

    @Test public void realStudentStorePageLoadsWithoutFailedCommands() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.store.acceptance"));
        RecordingGateway gateway = new RecordingGateway();
        NetworkAuthClientService auth = new NetworkAuthClientService(gateway);
        try {
            ClientSession session = new ClientSession(); session.open(auth.login("demo_student", "student123"));
            ClientBusinessServices services = new ClientBusinessServices(new NetworkClientService(gateway), session);
            AtomicReference<RealStorePage> page = new AtomicReference<RealStorePage>();
            SwingUtilities.invokeAndWait(() -> page.set(new RealStorePage(session, services)));
            long deadline = System.currentTimeMillis() + 15000L;
            while (!AsyncTask.isIdle() && System.currentTimeMillis() < deadline) Thread.sleep(20L);
            assertTrue("商店页面异步请求未结束", AsyncTask.isIdle());
            assertTrue(String.join("\n", gateway.failures), gateway.failures.isEmpty());
        } finally { try { auth.logout(); } finally { gateway.close(); } }
    }

    private static final class RecordingGateway implements ClientGateway {
        private final SocketClientGateway delegate = new SocketClientGateway("127.0.0.1",
                Integer.getInteger("vcampus.test.server.port", 8888), 3000, 15000);
        private final List<String> failures = Collections.synchronizedList(new ArrayList<String>());
        @Override public Message send(Message request) throws IOException, ClassNotFoundException {
            Message response = delegate.send(request);
            if (response != null && !response.isSuccess()) failures.add(request.getCommand() + " -> "
                    + response.getResultCode() + ": " + response.getUserMessage());
            return response;
        }
        @Override public boolean isConnected() { return delegate.isConnected(); }
        @Override public void close() { delegate.close(); }
    }
}
