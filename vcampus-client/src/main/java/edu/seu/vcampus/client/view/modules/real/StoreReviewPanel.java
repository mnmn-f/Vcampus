package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** 学生提交已完成订单的商品评价并查看评分记录。 */
public final class StoreReviewPanel extends SectionCard {
    private final BasePage page; private final StoreClientService service;
    private final JTextField order=UiFactory.textField(7), product=UiFactory.textField(7);
    private final JSpinner score=new JSpinner(new SpinnerNumberModel(5,1,5,1));
    private final JTextArea content=UiFactory.textArea(2,30); private final JLabel state=UiFactory.muted(" ");
    private final AsyncPagedTable<ProductReviewDto> table;
    public StoreReviewPanel(BasePage page,StoreClientService service){super("商品评价","仅本人已完成订单的明细可以评价一次。");this.page=page;this.service=service;table=new AsyncPagedTable<ProductReviewDto>("评价记录","输入商品编号查看评价。","商品编号",new String[0],new String[]{"商品","评分","内容","时间"},new AsyncPagedTable.Loader<ProductReviewDto>(){@Override public PageSlice<ProductReviewDto> load(int p,String k,String f)throws Exception{Long id=RealUi.number(k);if(id==null)return new PageSlice<ProductReviewDto>(null,0,1,20);ProductReviewPage v=StoreReviewPanel.this.service.listReviews(new ProductReviewQuery(id.longValue(),p,20));return new PageSlice<ProductReviewDto>(v.getItems(),v.getTotal(),p,20);}},new AsyncPagedTable.RowMapper<ProductReviewDto>(){@Override public Object[] values(ProductReviewDto v){return new Object[]{v.getProductName(),v.getScore(),RealUi.text(v.getContent()),RealUi.dateTime(v.getCreatedAt())};}},null);JPanel fields=new JPanel(new GridLayout(0,4,8,8));fields.setOpaque(false);fields.add(UiFactory.labelledField("订单编号",order));fields.add(UiFactory.labelledField("商品编号",product));fields.add(UiFactory.labelledField("评分",score));JButton save=new PrimaryButton("提交评价");save.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){save();}});fields.add(save);JPanel body=new JPanel(new BorderLayout(0,8));body.setOpaque(false);body.add(fields,BorderLayout.NORTH);body.add(UiFactory.labelledField("评价内容",content),BorderLayout.CENTER);body.add(state,BorderLayout.SOUTH);JPanel contentPanel=UiFactory.vertical(10);contentPanel.add(table);contentPanel.add(body);setContent(contentPanel);}
    private void save(){try{final ProductReviewWriteRequest r=new ProductReviewWriteRequest(Long.parseLong(RealUi.required(order.getText(),"订单编号")),Long.parseLong(RealUi.required(product.getText(),"商品编号")),((Number)score.getValue()).intValue(),RealUi.optional(content.getText()));AsyncTask.run(new AsyncTask.Work<ProductReviewDto>(){@Override public ProductReviewDto run()throws Exception{return service.addReview(r);}},new AsyncTask.Callback<ProductReviewDto>(){@Override public void onSuccess(ProductReviewDto v){state.setText("评价已提交");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}catch(Exception e){state.setText("订单和商品编号必须是整数");}}
}
