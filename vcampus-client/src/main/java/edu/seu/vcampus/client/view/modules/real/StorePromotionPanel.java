package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 商店管理员维护阈值、百分比和固定减免促销。 */
public final class StorePromotionPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final JTextField code=UiFactory.textField(8), name=UiFactory.textField(10), value=UiFactory.textField(6), threshold=UiFactory.textField(6), product=UiFactory.textField(6), category=UiFactory.textField(8);
    private final JComboBox<RealUi.CodeOption> type=new JComboBox<RealUi.CodeOption>(RealUi.options("THRESHOLD","PERCENT","FIXED"));
    private final JComboBox<RealUi.CodeOption> scope=new JComboBox<RealUi.CodeOption>(RealUi.options("ALL","PRODUCT","CATEGORY"));
    private final JLabel state=UiFactory.muted(" "); private long selectedId; private final AsyncPagedTable<PromotionDto> table;
    public StorePromotionPanel(BasePage page, StoreClientService service){super(new java.awt.BorderLayout());this.page=page;this.service=service;setOpaque(false);table=new AsyncPagedTable<PromotionDto>("促销优惠", "服务端按时间、范围和叠加规则计算优惠。", "促销关键字", new String[0], new String[]{"编码","名称","类型","优惠值","范围","启用"}, new AsyncPagedTable.Loader<PromotionDto>(){@Override public PageSlice<PromotionDto> load(int p,String k,String f)throws Exception{PromotionPage v=StorePromotionPanel.this.service.listPromotions();return new PageSlice<PromotionDto>(v.getItems(),v.getTotal(),1,100);}},new AsyncPagedTable.RowMapper<PromotionDto>(){@Override public Object[] values(PromotionDto v){return new Object[]{v.getCode(),v.getName(),RealUi.status(v.getType()),v.getValue(),RealUi.status(v.getProductScope()),v.isActive()?"启用":"停用"};}},new AsyncPagedTable.SelectionListener<PromotionDto>(){@Override public void onSelected(PromotionDto v){if(v==null)return;selectedId=v.getId();code.setText(v.getCode());name.setText(v.getName());value.setText(String.valueOf(v.getValue()));threshold.setText(v.getThreshold()==null?"":String.valueOf(v.getThreshold()));product.setText(v.getProductId()==null?"":String.valueOf(v.getProductId()));category.setText(RealUi.input(v.getCategoryCode()));type.setSelectedItem(RealUi.option(v.getType()));scope.setSelectedItem(RealUi.option(v.getProductScope()));}});JPanel fields=new JPanel(new GridLayout(0,4,8,8));fields.setOpaque(false);add(fields,"编码",code);add(fields,"名称",name);add(fields,"类型",type);add(fields,"优惠值",value);add(fields,"门槛",threshold);add(fields,"范围",scope);add(fields,"商品ID",product);add(fields,"分类编码",category);JButton save=new PrimaryButton("保存促销");save.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){save();}});fields.add(save);fields.add(state);setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));add(table);add(fields);}
    private void add(JPanel p,String label,java.awt.Component c){p.add(UiFactory.labelledField(label,c));}
    private void save(){try{String t=RealUi.code(type.getSelectedItem());String s=RealUi.code(scope.getSelectedItem());Long pid=RealUi.number(product.getText());if("PRODUCT".equals(s)&&pid==null)throw new IllegalArgumentException("商品范围需要商品ID");if("CATEGORY".equals(s)&&RealUi.optional(category.getText())==null)throw new IllegalArgumentException("分类范围需要分类编码");final PromotionWriteRequest r=new PromotionWriteRequest(selectedId,RealUi.required(code.getText(),"促销编码"),RealUi.required(name.getText(),"促销名称"),t,decimal(threshold.getText()),decimal(value.getText()),s,pid,RealUi.optional(category.getText()),org.threeten.bp.LocalDateTime.now().minusMinutes(1L),org.threeten.bp.LocalDateTime.now().plusYears(1L),false,true);AsyncTask.run(new AsyncTask.Work<PromotionDto>(){@Override public PromotionDto run()throws Exception{return service.savePromotion(r);}},new AsyncTask.Callback<PromotionDto>(){@Override public void onSuccess(PromotionDto v){state.setText("促销规则已保存");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}catch(Exception e){state.setText(e.getMessage());}}
    private static java.math.BigDecimal decimal(String v){String x=RealUi.optional(v);return x==null?null:new java.math.BigDecimal(x);}
    public void reload(){table.reload();}
}
