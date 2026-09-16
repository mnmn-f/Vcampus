package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.store.StoreSalesDto;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 按销量显示畅销商品占比。 */
final class StoreSalesPieChart extends JPanel {
    private static final Color[] COLORS = {
        new Color(0x4D, 0x7B, 0x2A), new Color(0xD4, 0x91, 0x2A),
        new Color(0x4D, 0x83, 0xA6), new Color(0xA6, 0x67, 0x83),
        new Color(0x76, 0x9B, 0x72), new Color(0xB8, 0xBE, 0xB9)
    };
    private List<Slice> slices = Collections.emptyList();

    StoreSalesPieChart() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(320, 300));
        setMinimumSize(new Dimension(280, 300));
    }

    void setItems(List<StoreSalesDto> values) {
        List<StoreSalesDto> sorted = new ArrayList<StoreSalesDto>();
        if (values != null) for (StoreSalesDto value : values) {
            if (value != null && value.getQuantitySold() > 0) sorted.add(value);
        }
        Collections.sort(sorted, new Comparator<StoreSalesDto>() {
            @Override public int compare(StoreSalesDto left, StoreSalesDto right) {
                return Long.compare(right.getQuantitySold(), left.getQuantitySold());
            }
        });
        List<Slice> next = new ArrayList<Slice>();
        long other = 0;
        for (int i = 0; i < sorted.size(); i++) {
            StoreSalesDto value = sorted.get(i);
            if (i < 5) next.add(new Slice(RealUi.text(value.getProductName()),
                    value.getQuantitySold()));
            else other += value.getQuantitySold();
        }
        if (other > 0) next.add(new Slice("其他商品", other));
        slices = next;
        repaint();
    }

    int itemCount() { return slices.size(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1E, 0x2A, 0x24));
        g.drawString("商品销量占比", 16, 20);
        if (slices.isEmpty()) {
            g.setColor(new Color(0x8A, 0x98, 0x92));
            g.drawString("暂无商品销售数据", 16, 54);
            return;
        }
        long total = 0;
        for (Slice slice : slices) total += slice.quantity;
        int diameter = 142;
        int start = 90;
        int used = 0;
        for (int i = 0; i < slices.size(); i++) {
            int angle = i == slices.size() - 1 ? 360 - used
                    : (int) Math.round(slices.get(i).quantity * 360.0 / total);
            g.setColor(COLORS[i % COLORS.length]);
            g.fillArc(16, 44, diameter, diameter, start, -angle);
            start -= angle;
            used += angle;
        }
        int y = 202;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            g.setColor(COLORS[i % COLORS.length]);
            g.fillRect(16, y - 10, 10, 10);
            g.setColor(new Color(0x3F, 0x4B, 0x45));
            String name = slice.name.length() > 10
                    ? slice.name.substring(0, 10) + "…" : slice.name;
            long percent = Math.round(slice.quantity * 100.0 / total);
            g.drawString(name + "  " + slice.quantity + "件（" + percent + "%）", 34, y);
            y += 17;
        }
    }

    private static final class Slice {
        private final String name;
        private final long quantity;
        private Slice(String name, long quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }
}
