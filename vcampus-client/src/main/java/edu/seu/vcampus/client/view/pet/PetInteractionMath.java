package edu.seu.vcampus.client.view.pet;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/** 桌宠拖动阈值和跨屏可见区域计算。 */
public final class PetInteractionMath {
    public static final int DRAG_THRESHOLD = 5;

    private PetInteractionMath() {
    }

    public static boolean isDrag(int deltaX, int deltaY) {
        return deltaX * deltaX + deltaY * deltaY
                > DRAG_THRESHOLD * DRAG_THRESHOLD;
    }

    /** 将窗口内桌宠完整限制在分层面板的可见范围内。 */
    public static Point clampToContainer(Point wanted, Dimension size,
                                         Dimension container) {
        if (wanted == null) wanted = new Point(0, 0);
        if (size == null) size = new Dimension(1, 1);
        if (container == null) container = new Dimension(0, 0);
        int maxX = Math.max(0, container.width - size.width);
        int maxY = Math.max(0, container.height - size.height);
        return new Point(Math.max(0, Math.min(wanted.x, maxX)),
                Math.max(0, Math.min(wanted.y, maxY)));
    }

    public static Point clamp(Point wanted, Dimension size, List<Rectangle> screens) {
        if (wanted == null) wanted = new Point(0, 0);
        if (size == null) size = new Dimension(1, 1);
        if (screens == null || screens.isEmpty()) return new Point(wanted);
        Rectangle target = new Rectangle(wanted, size);
        Rectangle chosen = screens.get(0);
        long bestArea = -1L;
        for (Rectangle screen : screens) {
            Rectangle overlap = target.intersection(screen);
            long area = overlap.isEmpty() ? 0L
                    : (long) overlap.width * overlap.height;
            if (area > bestArea) {
                chosen = screen;
                bestArea = area;
            }
        }
        if (bestArea == 0L) {
            long bestDistance = Long.MAX_VALUE;
            for (Rectangle screen : screens) {
                long dx = wanted.x - screen.getCenterX() < 0
                        ? (long) (screen.getCenterX() - wanted.x)
                        : (long) (wanted.x - screen.getCenterX());
                long dy = wanted.y - screen.getCenterY() < 0
                        ? (long) (screen.getCenterY() - wanted.y)
                        : (long) (wanted.y - screen.getCenterY());
                long distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    chosen = screen;
                    bestDistance = distance;
                }
            }
        }
        int maxX = chosen.x + Math.max(0, chosen.width - size.width);
        int maxY = chosen.y + Math.max(0, chosen.height - size.height);
        return new Point(Math.max(chosen.x, Math.min(wanted.x, maxX)),
                Math.max(chosen.y, Math.min(wanted.y, maxY)));
    }
}
