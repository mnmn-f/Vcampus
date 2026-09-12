package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 四人寝平面图：把床位占用画成房间的样子，而不是一张表。
 *
 * <p>床位表能回答「3 号床有没有人」，但回答不了「还剩哪张床、在靠窗还是靠门」——
 * 而宿管分配床位时想的正是后者。所以这里按真实户型摆：窗在外墙，两侧各两张床，
 * 中间是过道，卫生间和门在里侧。实心绿=已占用，虚线框=空闲，扫一眼就够。</p>
 *
 * <p>只画四人寝。其他户型的床位数对不上这张图，这时退回文字说明，而不是硬把六张
 * 床塞进四个格子——画错的图比没有图更误导人。</p>
 */
public final class DormRoomPlan extends JPanel {
    private static final int PLAN_WIDTH = 392;
    private static final int PLAN_HEIGHT = 296;
    private static final Color WALL = new Color(0xB9, 0xC6, 0xBC);
    private static final Color ROOM_FILL = new Color(0xFF, 0xFF, 0xFF, 120);
    private static final Color FREE_FILL = new Color(0xFF, 0xFF, 0xFF, 170);
    private static final Color SOFT = new Color(0xDF, 0xE6, 0xE0);

    private final List<DormBedDto> beds = new ArrayList<DormBedDto>();
    /** 四个床位当前占的矩形，用来判断点在了哪张床上。 */
    private final java.awt.Rectangle[] slots = new java.awt.Rectangle[4];
    private String caption = "选择一个房间查看床位分布";
    private DormBedDto picked;
    private Listener listener;

    /** 点中某张床时回调；宿管端用它把床位编号填进分配表单。 */
    public interface Listener {
        void onBedPicked(DormBedDto bed);
    }

    public DormRoomPlan() {
        setOpaque(false);
        setPreferredSize(new Dimension(PLAN_WIDTH, PLAN_HEIGHT + 26));
        setMinimumSize(new Dimension(PLAN_WIDTH, PLAN_HEIGHT + 26));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { pick(e.getPoint()); }
        });
    }

    public void setListener(Listener value) { listener = value; }

    /** 当前点中的床位；没点或点在空白处时为 null。 */
    public DormBedDto pickedBed() { return picked; }

    private void pick(java.awt.Point point) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null || !slots[i].contains(point)) continue;
            DormBedDto bed = bedAt(i);
            if (bed == null) return;
            picked = bed;
            repaint();
            if (listener != null) listener.onBedPicked(bed);
            return;
        }
    }

    /** 换一间房；床位按 bedNo 自然序排，1/2 在左，3/4 在右。 */
    public void showRoom(String roomCaption, List<DormBedDto> roomBeds) {
        caption = roomCaption;
        picked = null;
        beds.clear();
        if (roomBeds != null) beds.addAll(roomBeds);
        Collections.sort(beds, new Comparator<DormBedDto>() {
            @Override public int compare(DormBedDto left, DormBedDto right) {
                return order(left).compareTo(order(right));
            }
        });
        repaint();
    }

    /** 床位号可能是 "1" 也可能是 "A"，统一成可比较的字符串，数字优先。 */
    private static String order(DormBedDto bed) {
        String no = bed == null || bed.getBedNo() == null ? "" : bed.getBedNo().trim();
        return no.length() == 1 ? "0" + no : no;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (!beds.isEmpty() && beds.size() != 4) {
            // 户型对不上就不画：六张床塞进四个格子会让人以为这间房只有四个床位。
            g.setFont(DesignTokens.medium(13));
            g.setColor(DesignTokens.TEXT_PRIMARY);
            g.drawString(caption == null ? "" : caption, 16, 24);
            g.setFont(DesignTokens.regular(13));
            g.setColor(DesignTokens.TEXT_SECONDARY);
            g.drawString("该房间有 " + beds.size() + " 个床位，平面图目前只画四人寝；床位明细见下方表格。", 16, 48);
            g.dispose();
            return;
        }

        int top = 22;
        int left = 16;
        int width = PLAN_WIDTH - left * 2;
        int height = PLAN_HEIGHT - top - 16;

        // 外墙
        g.setColor(ROOM_FILL);
        g.fillRoundRect(left, top, width, height, 8, 8);
        g.setColor(WALL);
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(left, top, width, height, 8, 8);

        // 外墙上的窗
        int windowLeft = left + width / 2 - 58;
        g.setColor(DesignTokens.INFO);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(windowLeft, top, windowLeft + 116, top);
        g.setStroke(new BasicStroke(1f));
        g.setFont(DesignTokens.regular(12));
        centered(g, "窗", left + width / 2, top - 6, DesignTokens.INFO);

        // 中间过道
        int aisleLeft = left + 118;
        int aisleWidth = width - 236;
        g.setColor(new Color(0xF4, 0xF6, 0xF2, 210));
        g.fillRoundRect(aisleLeft, top + 20, aisleWidth, height - 40, 6, 6);
        g.setColor(SOFT);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{5f, 4f}, 0f));
        g.drawRoundRect(aisleLeft, top + 20, aisleWidth, height - 40, 6, 6);
        g.setStroke(new BasicStroke(1f));
        centered(g, "过 道", aisleLeft + aisleWidth / 2, top + height / 2 - 4, DesignTokens.TEXT_PLACEHOLDER);

        // 四张床：左 1、2，右 3、4
        int bedWidth = 100;
        int bedHeight = 72;
        int rightLeft = left + width - bedWidth - 14;
        slots[0] = new java.awt.Rectangle(left + 14, top + 20, bedWidth, bedHeight);
        slots[1] = new java.awt.Rectangle(left + 14, top + 104, bedWidth, bedHeight);
        slots[2] = new java.awt.Rectangle(rightLeft, top + 20, bedWidth, bedHeight);
        slots[3] = new java.awt.Rectangle(rightLeft, top + 104, bedWidth, bedHeight);
        for (int i = 0; i < slots.length; i++) {
            paintBed(g, bedAt(i), slots[i].x, slots[i].y, bedWidth, bedHeight, String.valueOf(i + 1));
        }

        // 卫生间与门
        int bathTop = top + height - 62;
        g.setColor(FREE_FILL);
        g.fillRoundRect(left + 14, bathTop, bedWidth, 48, 6, 6);
        g.setColor(SOFT);
        g.drawRoundRect(left + 14, bathTop, bedWidth, 48, 6, 6);
        centered(g, "卫生间", left + 14 + bedWidth / 2, bathTop + 29, DesignTokens.TEXT_SECONDARY);

        int doorLeft = rightLeft;
        int doorY = top + height;
        g.setColor(DesignTokens.PAGE_BACKGROUND);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(doorLeft, doorY, doorLeft + bedWidth, doorY);
        g.setColor(new Color(0xC9, 0xD3, 0xCC));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{3f, 3f}, 0f));
        g.drawArc(doorLeft, doorY - bedWidth, bedWidth, bedWidth * 2, 0, 90);
        g.setStroke(new BasicStroke(1f));
        centered(g, "门", doorLeft + bedWidth / 2, doorY - 8, DesignTokens.TEXT_SECONDARY);

        // 图例兼房间标题
        g.setFont(DesignTokens.medium(13));
        g.setColor(DesignTokens.TEXT_PRIMARY);
        g.drawString(caption == null ? "" : caption, left, top - 8);
        g.setFont(DesignTokens.regular(12));
        g.setColor(DesignTokens.TEXT_SECONDARY);
        g.drawString(picked == null ? "实心 = 已占用　虚线 = 空闲　点床位可选中"
                : "已选中 " + RealUiPlanText.bedLabel(picked), left, PLAN_HEIGHT + 16);
        g.dispose();
    }

    private DormBedDto bedAt(int index) {
        return index < beds.size() ? beds.get(index) : null;
    }

    private void paintBed(Graphics2D g, DormBedDto bed, int x, int y, int w, int h, String fallbackNo) {
        String no = bed == null || bed.getBedNo() == null ? fallbackNo : bed.getBedNo().trim();
        boolean chosen = bed != null && picked != null && bed.getId() == picked.getId();
        boolean occupied = bed != null && "OCCUPIED".equals(bed.getStatus());
        boolean maintenance = bed != null && "MAINTENANCE".equals(bed.getStatus());
        if (occupied) {
            g.setColor(DesignTokens.PRIMARY);
            g.fillRoundRect(x, y, w, h, 6, 6);
            if (chosen) outline(g, x, y, w, h);
            centeredBold(g, no + " 号床", x + w / 2, y + 30, Color.WHITE);
            String who = "OCCUPIED".equals(bed.getStatus()) ? "已入住" : RealUi.status(bed.getStatus());
            centered(g, who, x + w / 2, y + 52, new Color(0xDD, 0xE4, 0xCE));
            return;
        }
        g.setColor(maintenance ? DesignTokens.WARNING_BACKGROUND : FREE_FILL);
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(maintenance ? DesignTokens.WARNING : new Color(0xC9, 0xD3, 0xCC));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{4f, 3f}, 0f));
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setStroke(new BasicStroke(1f));
        if (chosen) outline(g, x, y, w, h);
        centeredBold(g, no + " 号床", x + w / 2, y + 30,
                maintenance ? DesignTokens.WARNING : DesignTokens.TEXT_SECONDARY);
        centered(g, maintenance ? "维护中" : "空闲", x + w / 2, y + 52,
                maintenance ? DesignTokens.WARNING : DesignTokens.TEXT_PLACEHOLDER);
    }

    /** 选中的床外面套一圈金色描边——占用和空闲已经用了实心与虚线，这里只能靠第三种记号。 */
    private void outline(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(DesignTokens.GOLD);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x - 2, y - 2, w + 4, h + 4, 8, 8);
        g.setStroke(new BasicStroke(1f));
    }

    private void centered(Graphics2D g, String text, int centerX, int baseline, Color color) {
        draw(g, text, centerX, baseline, color, DesignTokens.regular(12));
    }

    private void centeredBold(Graphics2D g, String text, int centerX, int baseline, Color color) {
        draw(g, text, centerX, baseline, color, DesignTokens.medium(14));
    }

    private void draw(Graphics2D g, String text, int centerX, int baseline, Color color, Font font) {
        Font previous = g.getFont();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
        g.setFont(previous);
    }
}
