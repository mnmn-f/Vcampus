package edu.seu.vcampus.client.ui;

import edu.seu.vcampus.common.module.ModuleId;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** 轻量线性图标，使用几何图形绘制，不依赖外部图片资源。 */
public final class LineIcon implements Icon {
    public enum Kind {
        BRAND, DASHBOARD, PROFILE, STUDENT_RECORD, ACADEMIC, LIBRARY,
        STORE, DORMITORY, AI_ASSISTANT, USER_ADMIN, SYSTEM
    }

    private final Kind kind;
    private final Color color;
    private final int size;

    private LineIcon(Kind kind, Color color, int size) {
        this.kind = kind == null ? Kind.DASHBOARD : kind;
        this.color = color == null ? DesignTokens.TEXT_PRIMARY : color;
        this.size = Math.max(16, size);
    }

    public static LineIcon of(ModuleId module, Color color) {
        return new LineIcon(kindOf(module), color, 20);
    }

    public static LineIcon brand(Color color, int size) {
        return new LineIcon(Kind.BRAND, color, size);
    }

    public static LineIcon of(Kind kind, Color color, int size) {
        return new LineIcon(kind, color, size);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1.4f, size / 12f), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        float s = size;
        switch (kind) {
            case BRAND: building(g, s); break;
            case PROFILE: profile(g, s); break;
            case STUDENT_RECORD: document(g, s); break;
            case ACADEMIC: book(g, s); break;
            case LIBRARY: books(g, s); break;
            case STORE: bag(g, s); break;
            case DORMITORY: house(g, s); break;
            case AI_ASSISTANT: assistant(g, s); break;
            case USER_ADMIN: users(g, s); break;
            case SYSTEM: system(g, s); break;
            default: dashboard(g, s); break;
        }
        g.dispose();
    }

    private static void dashboard(Graphics2D g, float s) {
        float q = s * .18f;
        g.draw(new RoundRectangle2D.Float(q, q, s * .64f, s * .64f, s * .12f, s * .12f));
        g.draw(new Line2D.Float(s * .28f, s * .50f, s * .47f, s * .50f));
        g.draw(new Line2D.Float(s * .28f, s * .63f, s * .62f, s * .63f));
        g.fill(new Ellipse2D.Float(s * .58f, s * .30f, s * .12f, s * .12f));
    }

    private static void profile(Graphics2D g, float s) {
        g.draw(new Ellipse2D.Float(s * .34f, s * .14f, s * .32f, s * .32f));
        g.drawArc(Math.round(s * .18f), Math.round(s * .48f), Math.round(s * .64f),
                Math.round(s * .46f), 0, 180);
    }

    private static void document(Graphics2D g, float s) {
        Path2D page = new Path2D.Float();
        page.moveTo(s * .28f, s * .14f); page.lineTo(s * .65f, s * .14f);
        page.lineTo(s * .77f, s * .26f); page.lineTo(s * .77f, s * .84f);
        page.lineTo(s * .28f, s * .84f); page.closePath(); g.draw(page);
        g.draw(new Line2D.Float(s * .65f, s * .14f, s * .65f, s * .28f));
        g.draw(new Line2D.Float(s * .65f, s * .28f, s * .77f, s * .28f));
        g.draw(new Line2D.Float(s * .39f, s * .47f, s * .65f, s * .47f));
        g.draw(new Line2D.Float(s * .39f, s * .64f, s * .65f, s * .64f));
    }

    private static void book(Graphics2D g, float s) {
        g.draw(new Line2D.Float(s * .50f, s * .20f, s * .50f, s * .82f));
        g.draw(new RoundRectangle2D.Float(s * .13f, s * .24f, s * .34f, s * .54f,
                s * .06f, s * .06f));
        g.draw(new RoundRectangle2D.Float(s * .53f, s * .24f, s * .34f, s * .54f,
                s * .06f, s * .06f));
    }

    private static void books(Graphics2D g, float s) {
        g.draw(new RoundRectangle2D.Float(s * .18f, s * .25f, s * .22f, s * .56f,
                s * .04f, s * .04f));
        g.draw(new RoundRectangle2D.Float(s * .41f, s * .16f, s * .22f, s * .65f,
                s * .04f, s * .04f));
        g.draw(new RoundRectangle2D.Float(s * .64f, s * .31f, s * .18f, s * .50f,
                s * .04f, s * .04f));
    }

    private static void bag(Graphics2D g, float s) {
        g.draw(new RoundRectangle2D.Float(s * .20f, s * .33f, s * .60f, s * .50f,
                s * .08f, s * .08f));
        g.drawArc(Math.round(s * .34f), Math.round(s * .10f), Math.round(s * .32f),
                Math.round(s * .42f), 0, 180);
    }

    private static void house(Graphics2D g, float s) {
        Path2D roof = new Path2D.Float();
        roof.moveTo(s * .14f, s * .46f); roof.lineTo(s * .50f, s * .15f);
        roof.lineTo(s * .86f, s * .46f); g.draw(roof);
        g.draw(new RoundRectangle2D.Float(s * .24f, s * .43f, s * .52f, s * .40f,
                s * .03f, s * .03f));
        g.draw(new Line2D.Float(s * .48f, s * .83f, s * .48f, s * .59f));
        g.draw(new Line2D.Float(s * .52f, s * .83f, s * .52f, s * .59f));
    }

    private static void assistant(Graphics2D g, float s) {
        g.draw(new Ellipse2D.Float(s * .18f, s * .18f, s * .64f, s * .64f));
        g.draw(new Line2D.Float(s * .50f, s * .33f, s * .50f, s * .67f));
        g.draw(new Line2D.Float(s * .33f, s * .50f, s * .67f, s * .50f));
        g.fill(new Ellipse2D.Float(s * .76f, s * .12f, s * .10f, s * .10f));
    }

    private static void users(Graphics2D g, float s) {
        g.draw(new Ellipse2D.Float(s * .18f, s * .18f, s * .24f, s * .24f));
        g.draw(new Ellipse2D.Float(s * .56f, s * .18f, s * .24f, s * .24f));
        g.drawArc(Math.round(s * .08f), Math.round(s * .48f), Math.round(s * .46f),
                Math.round(s * .38f), 0, 180);
        g.drawArc(Math.round(s * .46f), Math.round(s * .48f), Math.round(s * .46f),
                Math.round(s * .38f), 0, 180);
    }

    private static void system(Graphics2D g, float s) {
        g.draw(new Ellipse2D.Float(s * .29f, s * .29f, s * .42f, s * .42f));
        g.draw(new Ellipse2D.Float(s * .43f, s * .43f, s * .14f, s * .14f));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4d;
            float x1 = (float) (s * .50f + Math.cos(angle) * s * .33f);
            float y1 = (float) (s * .50f + Math.sin(angle) * s * .33f);
            float x2 = (float) (s * .50f + Math.cos(angle) * s * .43f);
            float y2 = (float) (s * .50f + Math.sin(angle) * s * .43f);
            g.draw(new Line2D.Float(x1, y1, x2, y2));
        }
    }

    private static void building(Graphics2D g, float s) {
        g.draw(new RoundRectangle2D.Float(s * .18f, s * .26f, s * .64f, s * .58f,
                s * .03f, s * .03f));
        g.draw(new Line2D.Float(s * .14f, s * .26f, s * .86f, s * .26f));
        for (int i = 0; i < 3; i++) {
            float x = s * (.30f + i * .20f);
            g.draw(new RoundRectangle2D.Float(x, s * .40f, s * .08f, s * .16f,
                    s * .02f, s * .02f));
        }
        g.draw(new Line2D.Float(s * .50f, s * .84f, s * .50f, s * .64f));
    }

    private static Kind kindOf(ModuleId module) {
        if (module == null) return Kind.DASHBOARD;
        switch (module) {
            case PROFILE: return Kind.PROFILE;
            case STUDENT_RECORD: return Kind.STUDENT_RECORD;
            case ACADEMIC: return Kind.ACADEMIC;
            case LIBRARY: return Kind.LIBRARY;
            case STORE: return Kind.STORE;
            case DORMITORY: return Kind.DORMITORY;
            case AI_ASSISTANT: return Kind.AI_ASSISTANT;
            case USER_ADMIN: return Kind.USER_ADMIN;
            case SYSTEM: return Kind.SYSTEM;
            default: return Kind.DASHBOARD;
        }
    }
}
