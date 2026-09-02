package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** 商店管理员的商品和库存行内维护表单。 */
public final class StoreProductEditorPanel extends SectionCard {
    public interface Listener {
        void onSave(ProductWriteRequest request);
        void onAdjust(StockAdjustRequest request);
    }

    private final JTextField sku = field();
    private final JTextField name = field();
    private final JTextField category = field();
    private final JTextField imageUrl = field();
    private final JTextField price = field();
    private final JTextField stock = field();
    private final JTextField delta = field();
    private final JTextField remark = field();
    private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(
            RealUi.options("DRAFT", "ON_SALE", "OFF_SALE", "ARCHIVED"));
    private final JTextArea description = UiFactory.textArea(2, 28);
    private final JLabel error = UiFactory.muted(" ");
    private final Listener listener;
    private long productId;

    public StoreProductEditorPanel(Listener listener) {
        super("商品详情与库存维护", "编辑商品或调整库存；价格、库存和状态需合法。");
        this.listener = listener;
        status.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        add(fields, "商品编码", sku); add(fields, "商品名称", name);
        add(fields, "分类编码", category); add(fields, "图片 URL", imageUrl); add(fields, "单价", price);
        add(fields, "库存", stock); add(fields, "状态", status);
        add(fields, "库存增量", delta); add(fields, "调整备注", remark);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.NORTH);
        content.add(UiFactory.labelledField("商品说明", description), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8);
        JButton clear = new SecondaryButton("新建");
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        JButton save = new PrimaryButton("保存商品");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        JButton adjust = new SecondaryButton("调整库存");
        adjust.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { adjust(); }
        });
        actions.add(clear); actions.add(save); actions.add(adjust); actions.add(error);
        content.add(actions, BorderLayout.SOUTH);
        setContent(content);
        startNew();
    }

    public void startNew() {
        productId = 0L;
        sku.setEditable(true);
        sku.setText(""); name.setText(""); category.setText(""); imageUrl.setText(""); price.setText("");
        stock.setText("0"); delta.setText(""); remark.setText(""); description.setText("");
        status.setSelectedItem(RealUi.option("DRAFT")); error.setText(" ");
    }

    public void showProduct(ProductDto value) {
        if (value == null) { startNew(); return; }
        productId = value.getId(); sku.setEditable(false);
        sku.setText(RealUi.input(value.getSku())); name.setText(RealUi.input(value.getName()));
        category.setText(RealUi.input(value.getCategory())); imageUrl.setText(RealUi.input(value.getImageUrl())); price.setText(RealUi.input(value.getPrice()));
        stock.setText(String.valueOf(value.getStockQty())); status.setSelectedItem(RealUi.option(value.getStatus()));
        description.setText(RealUi.input(value.getDescription())); delta.setText(""); remark.setText("");
        error.setText(" ");
    }

    private void save() {
        try {
            String title = required(name.getText(), "商品名称");
            BigDecimal amount = new BigDecimal(required(price.getText(), "单价"));
            int quantity = Integer.parseInt(required(stock.getText(), "库存"));
            if (amount.signum() < 0 || quantity < 0) throw new IllegalArgumentException("单价和库存不能为负数");
            if (listener != null) listener.onSave(new ProductWriteRequest(productId,
                    RealUi.optional(sku.getText()), title, RealUi.optional(category.getText()),
                    RealUi.optional(description.getText()), amount, quantity,
                    RealUi.code(status.getSelectedItem()), RealUi.optional(imageUrl.getText())));
            error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("单价和库存格式不正确"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private void adjust() {
        try {
            if (productId == 0L) throw new IllegalArgumentException("请先选择已有商品");
            int amount = Integer.parseInt(required(delta.getText(), "库存增量"));
            if (amount == 0) throw new IllegalArgumentException("库存增量不能为 0");
            if (listener != null) listener.onAdjust(new StockAdjustRequest(productId, amount,
                    RealUi.optional(remark.getText())));
            error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("库存增量必须是整数"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static void add(JPanel panel, String label, java.awt.Component component) {
        panel.add(UiFactory.labelledField(label, component));
    }
    private static JTextField field() { return UiFactory.textField(12); }
    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + "不能为空");
        return value.trim();
    }
}
