package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 商店自然日销售趋势；轻量 Swing 折线图，不阻塞界面线程。 */
public final class StoreSalesTrendPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final JTextField start = UiFactory.textField(10);
    private final JTextField end = UiFactory.textField(10);
    private final JLabel state = UiFactory.muted(" ");
    private final TrendCanvas canvas = new TrendCanvas();

    public StoreSalesTrendPanel(BasePage page, StoreClientService service) {
        super("销售趋势", "按自然日查看已支付和已完成订单；退款不计入。 ");
        if (page == null || service == null) throw new IllegalArgumentException("趋势依赖不能为空");
        this.page = page; this.service = service;
        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("开始日期", start)); fields.add(UiFactory.labelledField("结束日期", end));
        JButton query = new SecondaryButton("查询趋势"); query.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(); } });
        fields.add(query); JPanel content = UiFactory.vertical(10); content.add(fields); content.add(canvas); content.add(state); setContent(content); load();
    }

    public void reload() { load(); }
    private void load() {
        if (invalidDate(start.getText()) || invalidDate(end.getText())) { state.setText("日期格式应为 yyyy-MM-dd"); return; }
        final org.threeten.bp.LocalDate from = date(start.getText());
        final org.threeten.bp.LocalDate to = date(end.getText());
        if (from != null && to != null && to.isBefore(from)) { state.setText("结束日期不能早于开始日期"); return; }
        state.setText("正在加载…");
        AsyncTask.run(new AsyncTask.Work<StoreSalesTrendPage>() { @Override public StoreSalesTrendPage run() throws Exception { return service.salesTrend(new StoreSalesTrendQuery(from, to)); } }, new AsyncTask.Callback<StoreSalesTrendPage>() {
            @Override public void onSuccess(StoreSalesTrendPage value) { canvas.setItems(value == null ? null : value.getItems()); state.setText(canvas.items.isEmpty() ? "暂无销售数据" : "共 " + canvas.items.size() + " 天"); }
            @Override public void onFailure(Throwable error) { state.setText(AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
        });
    }
    private static org.threeten.bp.LocalDate date(String value) { String v=RealUi.optional(value); if(v==null)return null; try{return org.threeten.bp.LocalDate.parse(v);}catch(RuntimeException ex){return null;} }
    private static boolean invalidDate(String value) { return RealUi.optional(value) != null && date(value) == null; }

    private static final class TrendCanvas extends JPanel {
        private List<StoreSalesTrendDto> items = Collections.emptyList();
        TrendCanvas() { setPreferredSize(new Dimension(760, 280)); setMinimumSize(new Dimension(0, 220)); setBackground(Color.WHITE); setToolTipText(""); }
        void setItems(List<StoreSalesTrendDto> value) { items=value==null?Collections.<StoreSalesTrendDto>emptyList():value; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int left=48, bottom=getHeight()-38, right=getWidth()-20, top=20; g.setColor(new Color(0xD9,0xE2,0xDE)); g.drawLine(left,bottom,right,bottom); g.drawLine(left,top,left,bottom);
            if(items.isEmpty()){g.setColor(new Color(0x8A,0x98,0x92));g.drawString("选择日期后暂无销售数据",left+20,(top+bottom)/2);return;}
            BigDecimal max=BigDecimal.ZERO; for(StoreSalesTrendDto v:items)if(v.getAmount().compareTo(max)>0)max=v.getAmount(); if(max.signum()==0)max=BigDecimal.ONE;
            g.setColor(new Color(0x4D,0x7B,0x2A)); g.setStroke(new BasicStroke(2f)); int count=items.size(); int lastX=-1,lastY=-1;
            for(int i=0;i<count;i++){StoreSalesTrendDto v=items.get(i); int x=left+(right-left)*i/Math.max(1,count-1); int y=bottom-(int)(v.getAmount().doubleValue()/max.doubleValue()*(bottom-top)); if(lastX>=0)g.drawLine(lastX,lastY,x,y);g.fillOval(x-4,y-4,8,8); if(count<=12||i==0||i==count-1)g.drawString(v.getDate().toString(),x-28,bottom+22); lastX=x;lastY=y;}
            g.setColor(new Color(0x5B,0x67,0x61));g.drawString("销售额（元）",left,top-5);
        }
        @Override public String getToolTipText(java.awt.event.MouseEvent event) { if(items.isEmpty())return null; int left=48,right=getWidth()-20; int index=Math.round((event.getX()-left)*(items.size()-1)/(float)Math.max(1,right-left)); if(index<0||index>=items.size())return null; StoreSalesTrendDto v=items.get(index); return v.getDate()+"　销量 "+v.getQuantity()+"　销售额 ¥"+v.getAmount(); }
    }
}
