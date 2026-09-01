package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 商店管理员维护稳定商品分类编码。 */
public final class StoreCategoryPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final JTextField code = UiFactory.textField(12);
    private final JTextField name = UiFactory.textField(12);
    private final JLabel state = UiFactory.muted(" ");
    private long selectedId;
    private final AsyncPagedTable<StoreCategoryDto> table;

    public StoreCategoryPanel(BasePage page, StoreClientService service) {
        super("商品分类", "使用稳定分类编码筛选商品；停用分类不会删除历史订单。");
        this.page = page; this.service = service;
        table = new AsyncPagedTable<StoreCategoryDto>("分类列表", "", "分类关键字", new String[0], new String[]{"编号", "编码", "名称", "状态"}, new AsyncPagedTable.Loader<StoreCategoryDto>() {
            @Override public PageSlice<StoreCategoryDto> load(int p, String k, String f) throws Exception { StoreCategoryPage v=StoreCategoryPanel.this.service.listCategories(); return new PageSlice<StoreCategoryDto>(v.getItems(),v.getTotal(),1,100); }
        }, new AsyncPagedTable.RowMapper<StoreCategoryDto>() { @Override public Object[] values(StoreCategoryDto v){return new Object[]{v.getId(),RealUi.text(v.getCode()),RealUi.text(v.getName()),v.isActive()?"启用":"停用"};} }, new AsyncPagedTable.SelectionListener<StoreCategoryDto>() { @Override public void onSelected(StoreCategoryDto v){ if(v==null)return;selectedId=v.getId();code.setText(v.getCode());name.setText(v.getName()); } });
        JPanel fields=new JPanel(new GridLayout(1,3,10,8));fields.setOpaque(false);fields.add(UiFactory.labelledField("编码",code));fields.add(UiFactory.labelledField("名称",name));JButton save=new PrimaryButton("保存分类");save.addActionListener(new java.awt.event.ActionListener(){@Override public void actionPerformed(java.awt.event.ActionEvent e){save();}});fields.add(save);JPanel content=UiFactory.vertical(10);content.add(table);content.add(fields);content.add(state);setContent(content);
    }
    public void reload(){table.reload();}
    private void save(){try{final StoreCategoryWriteRequest r=new StoreCategoryWriteRequest(selectedId,RealUi.required(code.getText(),"分类编码"),RealUi.required(name.getText(),"分类名称"),true);AsyncTask.run(new AsyncTask.Work<StoreCategoryDto>(){@Override public StoreCategoryDto run()throws Exception{return service.saveCategory(r);}},new AsyncTask.Callback<StoreCategoryDto>(){@Override public void onSuccess(StoreCategoryDto v){state.setText("分类已保存");table.reload();}@Override public void onFailure(Throwable e){state.setText(AsyncTask.message(e));page.showError(AsyncTask.message(e));}});}catch(IllegalArgumentException e){state.setText(e.getMessage());}}
}
