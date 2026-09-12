package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.*;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

public class StoreDetailUploadReviewTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();
    @Test public void detailShowsReviewsWithoutPurchaseAndKeepsFullDescription() throws Exception {
        AtomicReference<ProductReviewQuery> query = new AtomicReference<>();
        AtomicInteger cartChanged = new AtomicInteger();
        ProductDto product = new ProductDto(7, "CUP", "校园杯", "CULTURE", "完整商品说明，保温四小时。", BigDecimal.TEN, 8, "ON_SALE");
        StoreClientService service = service((method, args) -> {
            if (method.equals("getProductDetail")) return product;
            if (method.equals("addCartItem")) { assertEquals(7, ((CartItemRequest) args[0]).getProductId()); return null; }
            if (method.equals("listReviews")) { query.set((ProductReviewQuery) args[0]); return new ProductReviewPage(Collections.singletonList(new ProductReviewDto(3,7,4,"校园杯",5,"质量很好","匿名用户",LocalDateTime.now())),1); }
            throw new AssertionError("详情不应查询本人订单: " + method);
        });
        AtomicReference<StoreProductDetailsPanel> panel = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> panel.set(new StoreProductDetailsPanel(service, product, true, cartChanged::incrementAndGet)));
        AsyncPagedTable<?> reviews = field(panel.get(), "reviews");
        AsyncPagedTableRefreshTest.await(() -> reviews.getTable().getRowCount() == 1);
        AsyncPagedTableRefreshTest.edt(() -> {
            assertEquals(7, query.get().getProductId()); assertEquals("质量很好", reviews.getTable().getValueAt(0,2));
            assertEquals(product.getDescription(), find(panel.get(), JTextArea.class).getText());
            reviews.getTable().setRowSelectionInterval(0, 0);
            try { assertEquals("质量很好", ((JTextArea) field(panel.get(), "reviewText")).getText()); } catch (Exception e) { throw new AssertionError(e); }
            button(panel.get(), "加入购物车").doClick();
        });
        AsyncPagedTableRefreshTest.await(() -> cartChanged.get() == 1);
    }
    @Test public void chosenProductImageIsPreviewedUploadedAndCanBeRemoved() throws Exception {
        AtomicReference<ProductWriteRequest> request = new AtomicReference<>();
        AtomicReference<StoreProductEditorPanel> holder = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> holder.set(new StoreProductEditorPanel(new StoreProductEditorPanel.Listener() {
            public void onSave(ProductWriteRequest value) { request.set(value); }
            public void onAdjust(StockAdjustRequest value) { }
        })));
        StoreProductEditorPanel editor = holder.get(); ProductImageEditor image = field(editor, "productImage");
        File file = imageFile();
        AsyncPagedTableRefreshTest.edt(() -> image.selectFile(file));
        AsyncPagedTableRefreshTest.await(() -> { try { return image.bytes() != null; } catch (IllegalArgumentException loading) { return false; } });
        AsyncPagedTableRefreshTest.edt(() -> {
            editor.setCategories(Collections.singletonList(new StoreCategoryDto(1,"CULTURE","校园文创",true)));
            try { ((javax.swing.JComboBox<?>) field(editor,"category")).setSelectedIndex(1); ((JTextField) field(editor,"name")).setText("新杯子");
                ((JTextField) field(editor,"price")).setText("10"); } catch (Exception e) { throw new AssertionError(e); }
            button(editor,"保存商品").doClick();
            assertNotNull(request.get().getImageData()); assertTrue(request.get().getImageUrl().startsWith("store-image:"));
            button(editor,"移除图片").doClick(); button(editor,"保存商品").doClick(); assertNull(request.get().getImageData()); assertNull(request.get().getImageUrl());
        });
        assertEquals(640, ImageIO.read(new java.io.ByteArrayInputStream(ImageUploadSupport.encode(file,640,480,false))).getWidth());
        try { ImageUploadSupport.encode(files.newFile("bad.png"),640,480,false); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void reviewSubmitDisablesRepeatAndRemovesCandidateAfterSuccess() throws Exception {
        AtomicInteger writes = new AtomicInteger(); CountDownLatch release = new CountDownLatch(1);
        StoreClientService service = service((method,args) -> {
            if (method.equals("reviewCandidates")) return new ReviewCandidatePage(writes.get()==0 ? Collections.singletonList(new ReviewCandidateDto(2,7,"ORDER-2","校园杯",1)) : Collections.emptyList(),writes.get()==0 ? 1:0);
            if (method.equals("addReview")) { writes.incrementAndGet(); assertTrue(release.await(5,TimeUnit.SECONDS)); return new ProductReviewDto(1,7,2,"校园杯",5,"好","匿名用户",LocalDateTime.now()); }
            return null;
        });
        AtomicReference<StoreReviewPanel> holder = new AtomicReference<>();
        AsyncPagedTableRefreshTest.edt(() -> holder.set(new StoreReviewPanel(new BasePage(new ClientSession(),"商店",""){},service)));
        AsyncPagedTable<?> candidates = field(holder.get(),"candidates");
        AsyncPagedTableRefreshTest.await(() -> candidates.getTable().getRowCount()==1);
        try {
            AsyncPagedTableRefreshTest.edt(() -> { candidates.getTable().setRowSelectionInterval(0,0); button(holder.get(),"提交评价").doClick(); button(holder.get(),"提交评价").doClick(); });
            AsyncPagedTableRefreshTest.await(() -> writes.get()==1); release.countDown();
            AsyncPagedTableRefreshTest.await(() -> candidates.getTable().getRowCount()==0);
            assertEquals(1,writes.get());
        } finally { release.countDown(); }
    }
    private File imageFile() throws Exception { File file=files.newFile("product.png"); ImageIO.write(new java.awt.image.BufferedImage(400,300,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",file); return file; }
    @Test public void invalidRechargePictureDoesNotBreakPaymentDialog() throws Exception {
        assertNotNull(StoreRechargeQrDialog.load(files.newFile("invalid-qr.png")));
        assertEquals(400, StoreRechargeQrDialog.load(imageFile()).getWidth());
    }
    private interface Call { Object invoke(String name,Object[] args) throws Throwable; }
    private static StoreClientService service(Call call) { return (StoreClientService) Proxy.newProxyInstance(StoreClientService.class.getClassLoader(),new Class<?>[]{StoreClientService.class},(p,m,a)->call.invoke(m.getName(),a)); }
    @SuppressWarnings("unchecked") private static <T>T field(Object value,String name)throws Exception{java.lang.reflect.Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(value);}
    private static JButton button(Container root,String name){for(Component c:root.getComponents()){if(c instanceof JButton&&name.equals(((JButton)c).getText()))return (JButton)c;if(c instanceof Container){JButton b=button((Container)c,name);if(b!=null)return b;}}return null;}
    private static <T>T find(Container root,Class<T> type){for(Component c:root.getComponents()){if(type.isInstance(c))return type.cast(c);if(c instanceof Container){T result=find((Container)c,type);if(result!=null)return result;}}return null;}
}
