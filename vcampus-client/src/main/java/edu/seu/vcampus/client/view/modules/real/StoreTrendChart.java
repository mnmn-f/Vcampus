package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 可横向滚动、完整标注自然日的销售额折线图。 */
final class StoreTrendChart extends JPanel {
    private static final int POINT_WIDTH = 88;
    private List<StoreSalesTrendDto> items = Collections.emptyList();

    StoreTrendChart() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(760, 300));
        setToolTipText("");
    }

    void setItems(List<StoreSalesTrendDto> value) {
        items = value == null ? Collections.<StoreSalesTrendDto>emptyList()
                : new ArrayList<StoreSalesTrendDto>(value);
        setPreferredSize(new Dimension(Math.max(760, items.size() * POINT_WIDTH + 100), 300));
        revalidate();
        repaint();
    }

    int itemCount() { return items.size(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 58;
        int right = getWidth() - 58;
        int top = 28;
        int bottom = getHeight() - 48;
        g.setColor(new Color(0xD9, 0xE2, 0xDE));
        g.drawLine(left, bottom, right, bottom);
        g.drawLine(left, top, left, bottom);
        g.setColor(new Color(0x5B, 0x67, 0x61));
        g.drawString("销售额（元）", left, 18);
        if (items.isEmpty()) {
            g.drawString("暂无销售数据", left + 20, (top + bottom) / 2);
            return;
        }
        BigDecimal max = BigDecimal.ONE;
        for (StoreSalesTrendDto item : items) {
            if (item.getAmount().compareTo(max) > 0) max = item.getAmount();
        }
        g.setColor(new Color(0x4D, 0x7B, 0x2A));
        g.setStroke(new BasicStroke(2f));
        int previousX = -1;
        int previousY = -1;
        for (int i = 0; i < items.size(); i++) {
            StoreSalesTrendDto item = items.get(i);
            int x = left + (right - left) * i / Math.max(1, items.size() - 1);
            int y = bottom - (int) (item.getAmount().doubleValue()
                    / max.doubleValue() * (bottom - top));
            if (previousX >= 0) g.drawLine(previousX, previousY, x, y);
            g.fillOval(x - 4, y - 4, 8, 8);
            String date = item.getDate().toString();
            g.drawString(date, x - g.getFontMetrics().stringWidth(date) / 2, bottom + 22);
            previousX = x;
            previousY = y;
        }
    }

    @Override public String getToolTipText(java.awt.event.MouseEvent event) {
        if (items.isEmpty()) return null;
        int left = 58;
        int right = getWidth() - 58;
        int index = Math.round((event.getX() - left) * (items.size() - 1)
                / (float) Math.max(1, right - left));
        if (index < 0 || index >= items.size()) return null;
        StoreSalesTrendDto item = items.get(index);
        return item.getDate() + "　销量 " + item.getQuantity()
                + "　销售额 ¥" + item.getAmount();
    }
}
