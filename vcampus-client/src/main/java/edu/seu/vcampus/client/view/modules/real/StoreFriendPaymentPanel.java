package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDecisionRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 学生发起好友代付并处理收到的一次性代付请求。 */
public final class StoreFriendPaymentPanel extends SectionCard {
    private final BasePage page; private final StoreClientService service;
    private final JTextField order=UiFactory.textField(7), friend=UiFactory.textField(12), stateText=UiFactory.textField(7);
    private final JLabel state=UiFactory.muted(" "); private final AsyncPagedTable<FriendPaymentDto> table;
    public StoreFriendPaymentPanel(BasePage page,StoreClientService service){super("好友代付","订单归买家；好友只承担付款，不会看到买家余额。");this.page=page;this.service=service;table=new AsyncPagedTable<FriendPaymentDto>("代付请求","查看我发起或收到的代付请求。","范围：INBOX 或 MINE",new String[0],new String[]{"订单号","买家","付款人","金额","状态","过期时间"},new AsyncPagedTable.Loader<FriendPaymentDto>(){@Override public PageSlice<FriendPaymentDto> load(int p,String k,String f)throws Exception{String scope="MINE".equalsIgnoreCase(k.trim())?"MINE":"INBOX";FriendPaymentPage v=StoreFriendPaymentPanel.this.service.listFriendPayments(new FriendPaymentQuery(scope,p,20));return new PageSlice<FriendPaymentDto>(v.getItems(),v.getTotal(),p,20);}},new AsyncPagedTable.RowMapper<FriendPaymentDto>(){@Override public Object[] values(FriendPaymentDto v){return new Object[]{v.getOrderNo(),v.getBuyerName(),v.getPayerName(),"¥"+v.getAmount(),RealUi.status(v.getStatus()),RealUi.dateTime(v.getExpiresAt())};}},new AsyncPagedTable.SelectionListener<FriendPaymentDto>(){@Override public void onSelected(FriendPaymentDto v){if(v!=null)stateText.setText(String.valueOf(v.getId()));}});JPanel fields=new JPanel(new GridLayout(0,4,8,8));fields.setOpaque(false);fields.add(UiFactory.labelledField("订单编号",order));fields.add(UiFactory.labelledField("好友账号",friend));JButton create=new PrimaryButton("请求代付");create.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){create();}});fields.add(create);JButton accept=new PrimaryButton("接受代付");accept.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){decide("ACCEPT");}});fields.add(accept);JButton reject=new DangerButton("拒绝");reject.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){decide("REJECT");}});fields.add(reject);JButton withdraw=new SecondaryButton("撤回请求");withdraw.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){withdraw();}});fields.add(withdraw);JPanel foot=UiFactory.horizontal(6);foot.add(new JLabel("选中代付编号"));foot.add(stateText);foot.add(state);JPanel content=UiFactory.vertical(10);content.add(fields);content.add(table);content.add(foot);setContent(content);}
    private void create(){try{final FriendPaymentRequest r=new FriendPaymentRequest(Long.parseLong(RealUi.required(order.getText(),"订单编号")),RealUi.required(friend.getText(),"好友账号"));AsyncTask.run(new AsyncTask.Work<FriendPaymentDto>(){@Override public FriendPaymentDto run()throws Exception{return service.createFriendPayment(r);}},new AsyncTask.Callback<FriendPaymentDto>(){@Override public void onSuccess(FriendPaymentDto v){state.setText("代付请求已发出");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}catch(Exception e){state.setText("订单编号必须是整数");}}
    private void decide(final String decision){final Long id=RealUi.number(stateText.getText());if(id==null){state.setText("请先选中代付请求");return;}AsyncTask.run(new AsyncTask.Work<FriendPaymentDto>(){@Override public FriendPaymentDto run()throws Exception{return service.decideFriendPayment(new FriendPaymentDecisionRequest(id.longValue(),decision,"friend-pay-"+id));}},new AsyncTask.Callback<FriendPaymentDto>(){@Override public void onSuccess(FriendPaymentDto v){state.setText("代付请求已处理");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}
    private void withdraw(){final Long id=RealUi.number(stateText.getText());if(id==null){state.setText("请先选中代付请求");return;}if(!RealUi.confirm(this,"确认撤回该好友代付请求？"))return;AsyncTask.run(new AsyncTask.Work<FriendPaymentDto>(){@Override public FriendPaymentDto run()throws Exception{return service.withdrawFriendPayment(id.longValue());}},new AsyncTask.Callback<FriendPaymentDto>(){@Override public void onSuccess(FriendPaymentDto v){state.setText("代付请求已撤回");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}
}
