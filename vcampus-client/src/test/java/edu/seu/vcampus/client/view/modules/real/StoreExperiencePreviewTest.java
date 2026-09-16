package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.WidthTrackingPanel;
import edu.seu.vcampus.common.dto.store.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import static org.junit.Assert.*;

/** 真实组件与图片读取接口的离屏预览；图形仅为测试图片，不写入演示数据库。 */
public class StoreExperiencePreviewTest {
    @Test public void renderProductDetailAndUploadAtTwoWidths() throws Exception {
        byte[] photo = photo(); String reference = "store-image:ad530d2d-f45b-4c50-b89b-36d5a2e41a13";
        ProductDto product = new ProductDto(7,"SEU-CUP","校园纪念杯","CULTURE",
                "陶瓷杯，容量 350mL。请使用柔软海绵清洗，避免碰撞。",new BigDecimal("25.00"),19,"ON_SALE",reference,
                new BigDecimal("4.80"),1L,null,null,null);
        StoreClientService service = (StoreClientService) Proxy.newProxyInstance(StoreClientService.class.getClassLoader(),
                new Class<?>[]{StoreClientService.class},(proxy,method,args) -> {
                    if (method.getName().equals("getProductDetail")) return product;
                    if (method.getName().equals("getProductImage")) return photo;
                    if (method.getName().equals("searchProducts")) return new ProductPage(Collections.singletonList(product),1,20,1);
                    if (method.getName().equals("listCategories")) return new StoreCategoryPage(Collections.singletonList(new StoreCategoryDto(1,"CULTURE","校园文创",true)),1);
                    if (method.getName().equals("listReviews")) return new ProductReviewPage(Collections.singletonList(
                            new ProductReviewDto(1,7,2,"校园纪念杯",5,"杯子大小合适，包装完整，送货及时。","匿名用户",LocalDateTime.of(2026,9,12,12,30))),1);
                    if (method.getName().equals("getCart")) return new CartDto(1,2,"ACTIVE",
                            Collections.singletonList(new CartItemDto(7,"SEU-CUP","校园纪念杯",new BigDecimal("25.00"),3)),new BigDecimal("75.00"));
                    if (method.getName().equals("listCoupons")) return new CouponPage(Collections.singletonList(
                            new CouponDto(1,"WELCOME10","新生立减券",new BigDecimal("50.00"),new BigDecimal("10.00"),LocalDateTime.now().plusDays(10),true,false)),1);
                    if (method.getName().equals("listPromotions")) return new PromotionPage(Collections.singletonList(
                            new PromotionDto(1,"SEU-PROMO-001","校园生活立减活动","THRESHOLD",
                                    new BigDecimal("50.00"),new BigDecimal("5.00"),"ALL",null,null,
                                    LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(30),false,true)),1);
                    if (method.getName().equals("searchOrders")) return new OrderPage(
                            Collections.singletonList(previewOrder()),1,20,1L);
                    if (method.getName().equals("getOrderDetail")) return previewOrder();
                    if (method.getName().equals("checkoutPreview")) return new CheckoutPreviewDto(Collections.emptyList(),new BigDecimal("75.00"),BigDecimal.ZERO,new BigDecimal("10.00"),new BigDecimal("65.00"),null,"WELCOME10",true);
                    return null;
                });
        AtomicReference<StoreProductDetailsPanel> details = new AtomicReference<>();
        AtomicReference<StoreProductEditorPanel> editor = new AtomicReference<>();
        AtomicReference<StoreProductsPanel> products = new AtomicReference<>();
        AtomicReference<StoreCartPanel> cart = new AtomicReference<>();
        AtomicReference<StorePromotionPanel> promotions = new AtomicReference<>();
        AtomicReference<StoreCategoryPanel> categories = new AtomicReference<>();
        AtomicReference<StoreOrdersPanel> orders = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            UiFactory.configureLookAndFeel(); details.set(new StoreProductDetailsPanel(service,product,true,null));
            editor.set(new StoreProductEditorPanel(new StoreProductEditorPanel.Listener() {
                public void onSave(ProductWriteRequest value) { }
                public void onAdjust(StockAdjustRequest value) { }
            },service));
            editor.get().setCategories(Collections.singletonList(new StoreCategoryDto(1,"CULTURE","校园文创",true)));
            editor.get().showProduct(product);
            products.set(new StoreProductsPanel(new edu.seu.vcampus.client.view.BasePage(new edu.seu.vcampus.client.session.ClientSession(),"商店",""){},service,edu.seu.vcampus.common.security.Role.STUDENT));
            cart.set(new StoreCartPanel(new edu.seu.vcampus.client.view.BasePage(
                    new edu.seu.vcampus.client.session.ClientSession(),"商店",""){},service,null));
            edu.seu.vcampus.client.view.BasePage managerPage = new edu.seu.vcampus.client.view.BasePage(
                    new edu.seu.vcampus.client.session.ClientSession(),"商店管理",""){};
            promotions.set(new StorePromotionPanel(managerPage,service));
            categories.set(new StoreCategoryPanel(managerPage,service));
            orders.set(new StoreOrdersPanel(managerPage,service,edu.seu.vcampus.common.security.Role.STORE_MANAGER));
        });
        AsyncPagedTableRefreshTest.await(() -> AsyncTask.isIdle() && ProductImageView.isIdle());
        for (int width : new int[]{600,840}) snapshot(details.get(),"product-details-"+width,width,680);
        for (int width : new int[]{800,1100}) snapshot(editor.get(),"product-image-editor-"+width,width,760);
        for (int width : new int[]{800,960}) snapshot(products.get(),"product-list-images-"+width,width,760);
        snapshot(cart.get(),"cart-coupon-selector-1100",1100,560);
        snapshot(promotions.get(),"promotion-editor-960",960,720);
        snapshot(categories.get(),"category-editor-960",960,640);
        snapshot(orders.get(),"order-shipping-editor-1100",1100,720);
    }
    private static void snapshot(JComponent content,String name,int width,int height) throws Exception {
        AtomicReference<JScrollPane> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scroll = new JScrollPane(new WidthTrackingPanel(content)); scroll.setSize(width,height);
            holder.set(scroll); for (int i=0;i<5;i++) arrange(scroll);
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                JScrollPane scroll = holder.get();
                for (int i=0;i<5;i++) arrange(scroll);
                assertTrue("内容不能撑宽窗口",scroll.getViewport().getView().getWidth() <= width);
                BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D graphics = image.createGraphics(); scroll.printAll(graphics); graphics.dispose();
                File folder = new File("target/ui-previews"); assertTrue(folder.isDirectory() || folder.mkdirs());
                assertTrue(ImageIO.write(image,"png",new File(folder,name+".png")));
            } catch (java.io.IOException error) { throw new AssertionError(error); }
        });
    }
    private static void arrange(Container root) { root.doLayout(); for(Component child:root.getComponents()) if(child instanceof Container) arrange((Container)child); }
    private static byte[] photo() throws Exception {
        BufferedImage image = new BufferedImage(400,300,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=image.createGraphics(); g.setColor(new java.awt.Color(237,239,232)); g.fillRect(0,0,400,300);
        g.setColor(new java.awt.Color(94,118,47)); g.fillRoundRect(115,65,145,190,25,25);
        g.setStroke(new java.awt.BasicStroke(15)); g.drawOval(235,95,65,90); g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,30)); g.drawString("SEU",152,165); g.dispose();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes);return bytes.toByteArray();
    }
    private static OrderDto previewOrder() {
        LocalDateTime now = LocalDateTime.of(2026,9,14,18,0);
        return new OrderDto(7L,"VC-20260914-0007",10L,new BigDecimal("25.00"),
                new BigDecimal("25.00"),BigDecimal.ZERO,null,null,"SELF","PAID",
                "SHIPPED","SEU-20260914-1","已交九龙湖校区校园驿站",now,now,null,null,
                Collections.singletonList(new OrderItemDto(7L,"校园纪念杯",new BigDecimal("25.00"),1)));
    }
}
