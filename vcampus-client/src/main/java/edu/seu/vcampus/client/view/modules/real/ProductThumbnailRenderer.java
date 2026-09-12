package edu.seu.vcampus.client.view.modules.real;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/** 每个商品地址复用一个异步缩略图，滚动和重绘不重复发送图片请求。 */
final class ProductThumbnailRenderer implements TableCellRenderer {
    private final Map<String, ProductImageView> views = new LinkedHashMap<String, ProductImageView>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, ProductImageView> entry) { return size() > 100; }
    };

    @Override public Component getTableCellRendererComponent(final JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        String url = value == null ? "" : String.valueOf(value);
        ProductImageView view = views.get(url);
        if (view == null) {
            view = new ProductImageView(76, 56);
            view.setFont(edu.seu.vcampus.client.ui.DesignTokens.regular(10));
            view.addPropertyChangeListener("icon", e -> table.repaint());
            views.put(url, view);
            view.load(url);
        }
        view.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
        return view;
    }
}
