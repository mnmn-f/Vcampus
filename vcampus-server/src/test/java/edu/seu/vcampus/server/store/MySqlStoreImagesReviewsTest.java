package edu.seu.vcampus.server.store;

import edu.seu.vcampus.common.dto.store.*;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.mysql.MySqlStoreRecordRepository;
import edu.seu.vcampus.server.store.service.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import org.junit.*;
import static org.junit.Assert.*;

/** 可选真实 MySQL：所有业务写入均在测试结束时回滚。 */
public class MySqlStoreImagesReviewsTest {
    private Connection connection;
    private StoreService service;
    private SessionContext manager, student;
    private ProductDto product;
    private String sku, category;
    @Before public void prepare() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        connection = new JdbcConnectionFactory().open(); connection.setAutoCommit(false);
        manager = session(user("demo_store"), Role.STORE_MANAGER); student = session(user("demo_student"), Role.STUDENT);
        try (Statement s=connection.createStatement(); ResultSet r=s.executeQuery("SELECT code FROM store_categories WHERE active=1 LIMIT 1")) { assertTrue(r.next()); category=r.getString(1); }
        sku="IMAGE-QA-"+java.util.UUID.randomUUID();
        service=new StoreService(new MySqlStoreRecordRepository(), new StoreTransactionRunner() {
            public <T>T execute(TransactionWork<T> work)throws Exception{return work.execute(connection);}
        });
        product=service.saveProduct(manager,write(0,null,StoreImagesAndCandidatesTest.image()));
    }
    @After public void rollback()throws Exception{if(connection!=null){try{connection.rollback();}finally{connection.close();}}}
    @Test public void imagesAreStoredSeparatelyPreservedOnEditAndRemoved()throws Exception{
        String reference=product.getImageUrl(); byte[] bytes=StoreImagesAndCandidatesTest.image();
        assertArrayEquals(bytes,service.getProductImage(student,reference));
        service.saveProduct(manager,write(product.getId(),reference,null));
        assertArrayEquals(bytes,service.getProductImage(student,reference));
        assertEquals(reference,service.searchProducts(student,new ProductQuery(sku,null,null,1,20)).getItems().get(0).getImageUrl());
        ProductDto changed=service.saveProduct(manager,write(product.getId(),null,bytes)); assertNotEquals(reference,changed.getImageUrl());
        assertArrayEquals(bytes,service.getProductImage(student,changed.getImageUrl()));
        service.saveProduct(manager,write(product.getId(),null,null));
        try{service.getProductImage(student,changed.getImageUrl());fail();}catch(StoreServiceException expected){}
    }
    @Test public void candidatesPageBeyondHundredAndDisappearAfterReview()throws Exception{
        for(int i=0;i<105;i++) order();
        ReviewCandidatePage last=service.reviewCandidates(student,new ProductReviewQuery(product.getId(),6,20));
        assertEquals(105,last.getTotal());assertEquals(5,last.getItems().size());
        ReviewCandidateDto item=last.getItems().get(0);
        service.addReview(student,new ProductReviewWriteRequest(item.getOrderId(),product.getId(),5,"已验证图片商品"));
        assertEquals(104,service.reviewCandidates(student,new ProductReviewQuery(product.getId())).getTotal());
        assertEquals(1,service.listReviews(student,new ProductReviewQuery(product.getId(),1,20,"图片商品")).getTotal());
        assertEquals(0,service.listReviews(student,new ProductReviewQuery(product.getId(),1,20,"不存在文字")).getTotal());
        assertEquals(1,service.getProductDetail(student,product.getId()).getRatingCount());
    }
    private void order()throws Exception{
        long id;
        try(PreparedStatement s=connection.prepareStatement("INSERT INTO store_orders(order_no,buyer_id,total_amount,original_amount,discount_amount,status,paid_at,completed_at) VALUES(?,?,10,10,0,'COMPLETED',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))",Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,"IMAGE-QA-"+java.util.UUID.randomUUID());s.setLong(2,student.getUserId());s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();id=r.getLong(1);}
        }
        try(PreparedStatement s=connection.prepareStatement("INSERT INTO store_order_items(order_id,product_id,product_name_snapshot,unit_price_snapshot,quantity,line_amount) VALUES(?,?,?,10,1,10)")){
            s.setLong(1,id);s.setLong(2,product.getId());s.setString(3,"测试图片商品");s.executeUpdate();
        }
    }
    private ProductWriteRequest write(long id,String ref,byte[] bytes){return new ProductWriteRequest(id,sku,"测试图片商品",category,"完整说明",BigDecimal.TEN,20,"ON_SALE",ref,bytes);}
    private long user(String name)throws Exception{try(PreparedStatement s=connection.prepareStatement("SELECT id FROM users WHERE username=?")){s.setString(1,name);try(ResultSet r=s.executeQuery()){assertTrue(r.next());return r.getLong(1);}}}
    private static SessionContext session(long id,Role role){return new SessionContext("mysql-image-"+id,id,"test","测试",Collections.singleton(role),role);}
}
