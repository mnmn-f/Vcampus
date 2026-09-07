package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 学生领取和查看可用优惠券。 */
public final class StoreCouponPanel extends SectionCard {
    private final BasePage page; private final StoreClientService service;
    private final JTextField code=UiFactory.textField(12); private final JLabel state=UiFactory.muted(" ");
    private final AsyncPagedTable<CouponDto> table;
    public StoreCouponPanel(BasePage page,StoreClientService service){super("优惠券","领取后的优惠券可在购物车结算时填写编码使用。");this.page=page;this.service=service;table=new AsyncPagedTable<CouponDto>("我的优惠券","查看已领取和可领取优惠券。","",new String[0],new String[]{"编码","名称","门槛","减免","过期时间","状态"},new AsyncPagedTable.Loader<CouponDto>(){@Override public PageSlice<CouponDto> load(int p,String k,String f)throws Exception{CouponPage v=StoreCouponPanel.this.service.listCoupons();return new PageSlice<CouponDto>(v.getItems(),v.getTotal(),1,100);}},new AsyncPagedTable.RowMapper<CouponDto>(){@Override public Object[] values(CouponDto v){return new Object[]{v.getCode(),v.getName(),v.getThreshold(),v.getDiscountAmount(),RealUi.dateTime(v.getExpiresAt()),v.isUsed()?"已使用":v.isClaimed()?"已领取":"可领取"};}},null);JPanel fields=new JPanel(new GridLayout(1,3,8,8));fields.setOpaque(false);fields.add(UiFactory.labelledField("优惠券编码",code));JButton claim=new PrimaryButton("领取");claim.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){claim();}});fields.add(claim);fields.add(state);JPanel body=UiFactory.vertical(10);body.add(fields);body.add(table);setContent(body);}
    private void claim(){final String value=RealUi.optional(code.getText());if(value==null){state.setText("请输入优惠券编码");return;}AsyncTask.run(new AsyncTask.Work<CouponDto>(){@Override public CouponDto run()throws Exception{return service.claimCoupon(new CouponClaimRequest(value));}},new AsyncTask.Callback<CouponDto>(){@Override public void onSuccess(CouponDto v){state.setText("优惠券已领取");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}
}
