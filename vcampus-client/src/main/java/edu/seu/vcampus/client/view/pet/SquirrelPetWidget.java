package edu.seu.vcampus.client.view.pet;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** 可同时挂载在主窗口分层面板与透明桌面窗口中的松鼠组件。 */
public final class SquirrelPetWidget extends JComponent {
    public interface Listener {
        void onPrimaryClick();

        void onDragStarted();

        void onDragged(int deltaX, int deltaY);

        void onDragFinished();

        void onContextMenu(Component source, int x, int y);
    }

    public static final int WIDTH = 146;
    public static final int HEIGHT = 154;
    private static final String IMAGE_RESOURCE =
            "/edu/seu/vcampus/client/pet/squirrel.png";
    private static final String ACTION_SHEET_RESOURCE =
            "/edu/seu/vcampus/client/pet/squirrel-actions.png";
    private static final int ACTION_COLUMNS = 3;
    private static final int ACTION_ROWS = 2;

    private final PetStateModel state;
    private final Listener listener;
    private final BufferedImage image;
    private final BufferedImage actionSheet;
    private final Timer animationTimer;
    private boolean desktopMode;
    private boolean hover;
    private boolean dragged;
    private Point pressScreen;
    private long moodStarted = System.currentTimeMillis();
    private PetMood renderedMood;

    public SquirrelPetWidget(PetStateModel state, Listener listener) {
        this.state = state;
        this.listener = listener;
        this.image = loadImage(IMAGE_RESOURCE);
        this.actionSheet = loadImage(ACTION_SHEET_RESOURCE);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(getPreferredSize());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("单击打开校园助手，右键可喂松果或摸摸它");
        state.addListener(new PetStateModel.Listener() {
            @Override
            public void onPetStateChanged() {
                moodStarted = System.currentTimeMillis();
                renderedMood = null;
                repaint();
            }
        });
        animationTimer = new Timer(40, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                state.tick(System.currentTimeMillis());
                repaint();
            }
        });
        animationTimer.setCoalesce(true);
        installMouseHandling();
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent event) {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    syncAnimation();
                }
            }
        });
    }

    public void setDesktopMode(boolean value) {
        desktopMode = value;
        setToolTipText(desktopMode
                ? "拖动小松鼠调整桌面位置，单击恢复主界面"
                : "拖动小松鼠调整窗口内位置，单击打开校园助手");
    }

    public boolean isUsingRasterAsset() {
        return actionSheet != null || image != null;
    }

    public boolean isUsingActionSheet() {
        return actionSheet != null;
    }

    public void dispose() {
        animationTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            PetMood mood = state.getMood();
            if (mood == PetMood.IDLE && hover) mood = PetMood.HOVER;
            long now = System.currentTimeMillis();
            if (mood != renderedMood) {
                renderedMood = mood;
                moodStarted = now;
            }
            long elapsedMillis = animationEnabled()
                    ? Math.max(0L, now - moodStarted) : 0L;
            double elapsed = elapsedMillis / 1000.0;
            double bob = verticalOffset(mood, elapsed);
            double scale = scale(mood, elapsed);
            double angle = tilt(mood, elapsed);

            g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
            g.setColor(new Color(32, 39, 27));
            double shadowScale = mood == PetMood.SUCCESS
                    ? 0.82 + Math.abs(Math.cos(elapsed * 4.5)) * 0.18 : 1.0;
            g.fill(new Ellipse2D.Double(76 - 46 * shadowScale, 134,
                    92 * shadowScale, 12));
            g.setComposite(AlphaComposite.SrcOver);

            AffineTransform original = g.getTransform();
            g.translate(WIDTH / 2.0, 88 + bob);
            g.rotate(angle);
            g.scale(scale, scale);
            g.translate(-WIDTH / 2.0, -88);
            if (actionSheet != null) {
                drawActionFrame(g, actionFrameFor(mood, elapsedMillis));
            } else if (image != null) {
                g.drawImage(image, 12, 27, 122, 122, null);
            } else {
                drawFallbackSquirrel(g, mood);
            }
            g.setTransform(original);
            drawBubble(g, state.getBubble(), mood);
        } finally {
            g.dispose();
        }
    }

    static int actionFrameFor(PetMood mood, long elapsedMillis) {
        if (mood == PetMood.HOVER) {
            return elapsedMillis / 420L % 2L == 0L ? 1 : 0;
        }
        // 原思考帧的手掌遮住了面部，在小尺寸缩放后看起来像脸部缺损。
        // 改用完整正脸，思考感由倾斜、呼吸和气泡动画表达。
        if (mood == PetMood.THINKING) return 0;
        if (mood == PetMood.TALKING) {
            return elapsedMillis / 180L % 2L == 0L ? 1 : 0;
        }
        if (mood == PetMood.ATTENTION) return 3;
        if (mood == PetMood.SUCCESS) return 4;
        if (mood == PetMood.ERROR) return 5;
        if (mood == PetMood.OFFLINE) return 5;
        return 0;
    }

    private void drawActionFrame(Graphics2D g, int frameIndex) {
        int frameWidth = actionSheet.getWidth() / ACTION_COLUMNS;
        int frameHeight = actionSheet.getHeight() / ACTION_ROWS;
        int column = frameIndex % ACTION_COLUMNS;
        int row = frameIndex / ACTION_COLUMNS;
        int sourceX = column * frameWidth;
        int sourceY = row * frameHeight;
        g.drawImage(actionSheet,
                8, 26, 138, 150,
                sourceX, sourceY, sourceX + frameWidth, sourceY + frameHeight,
                null);
    }

    private void installMouseHandling() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hover = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (showPopupIfNeeded(event)) return;
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                pressScreen = event.getLocationOnScreen();
                dragged = false;
                if (listener != null) listener.onDragStarted();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (pressScreen == null || listener == null) return;
                Point current = event.getLocationOnScreen();
                int deltaX = current.x - pressScreen.x;
                int deltaY = current.y - pressScreen.y;
                if (!dragged && PetInteractionMath.isDrag(deltaX, deltaY)) dragged = true;
                if (dragged) listener.onDragged(deltaX, deltaY);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (showPopupIfNeeded(event)) {
                    pressScreen = null;
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(event) || pressScreen == null) return;
                if (dragged && listener != null) listener.onDragFinished();
                else if (!dragged && listener != null) listener.onPrimaryClick();
                pressScreen = null;
                dragged = false;
            }

            private boolean showPopupIfNeeded(MouseEvent event) {
                if (event.isPopupTrigger() && listener != null) {
                    listener.onContextMenu(SquirrelPetWidget.this,
                            event.getX(), event.getY());
                    return true;
                }
                return false;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void syncAnimation() {
        if (animationEnabled() && isShowing()) {
            moodStarted = System.currentTimeMillis();
            if (!animationTimer.isRunning()) animationTimer.start();
        } else {
            animationTimer.stop();
        }
    }

    private boolean animationEnabled() {
        return !"false".equalsIgnoreCase(
                System.getProperty("vcampus.pet.animation", "true"));
    }

    private static double moodSpeed(PetMood mood) {
        if (mood == PetMood.THINKING) return 5.2;
        if (mood == PetMood.TALKING) return 7.0;
        if (mood == PetMood.ATTENTION || mood == PetMood.ERROR || mood == PetMood.OFFLINE) return 8.0;
        if (mood == PetMood.SUCCESS) return 9.0;
        return 2.5;
    }

    private static double moodAmplitude(PetMood mood) {
        if (mood == PetMood.SUCCESS) return 7.0;
        if (mood == PetMood.TALKING || mood == PetMood.ATTENTION) return 3.5;
        return 2.0;
    }

    private static double verticalOffset(PetMood mood, double elapsed) {
        if (mood == PetMood.SUCCESS) {
            return -Math.abs(Math.sin(elapsed * 4.5)) * moodAmplitude(mood);
        }
        if (mood == PetMood.ERROR || mood == PetMood.OFFLINE) {
            return 2.5 + Math.sin(elapsed * moodSpeed(mood)) * 0.8;
        }
        return Math.sin(elapsed * moodSpeed(mood)) * moodAmplitude(mood);
    }

    private static double scale(PetMood mood, double elapsed) {
        if (mood == PetMood.ATTENTION) {
            return 1.0 + Math.sin(elapsed * 7.0) * 0.022;
        }
        if (mood == PetMood.SUCCESS) {
            return 1.0 + Math.abs(Math.sin(elapsed * 4.5)) * 0.025;
        }
        if (mood == PetMood.ERROR || mood == PetMood.OFFLINE) {
            return 0.985 + Math.sin(elapsed * 2.0) * 0.006;
        }
        return 1.0 + Math.sin(elapsed * 2.1) * 0.012;
    }

    private static double tilt(PetMood mood, double elapsed) {
        if (mood == PetMood.THINKING) return -0.055;
        if (mood == PetMood.ERROR) return Math.sin(elapsed * 14.0) * 0.035;
        if (mood == PetMood.OFFLINE) return -0.035;
        if (mood == PetMood.HOVER) return 0.05;
        return Math.sin(elapsed * 1.7) * 0.018;
    }

    private static BufferedImage loadImage(String resource) {
        try {
            if (SquirrelPetWidget.class.getResource(resource) == null) return null;
            return ImageIO.read(SquirrelPetWidget.class.getResource(resource));
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void drawFallbackSquirrel(Graphics2D g, PetMood mood) {
        Color fur = new Color(0xC9, 0x70, 0x2C);
        Color furLight = new Color(0xF2, 0xB5, 0x69);
        Color cream = new Color(0xFF, 0xE2, 0xB0);
        Color outline = new Color(0x72, 0x3C, 0x1E);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        g.setPaint(new GradientPaint(93, 45, furLight, 125, 121, fur));
        g.fill(new Ellipse2D.Double(77, 43, 56, 88));
        g.setColor(outline);
        g.draw(new Ellipse2D.Double(77, 43, 56, 88));
        g.setPaint(new GradientPaint(106, 60, cream, 121, 105, furLight));
        g.fill(new Ellipse2D.Double(97, 59, 22, 55));

        g.setPaint(new GradientPaint(48, 69, furLight, 85, 132, fur));
        g.fill(new Ellipse2D.Double(38, 68, 59, 67));
        g.setColor(outline);
        g.draw(new Ellipse2D.Double(38, 68, 59, 67));
        g.setColor(cream);
        g.fill(new Ellipse2D.Double(51, 85, 34, 43));

        Path2D leftEar = new Path2D.Double();
        leftEar.moveTo(39, 52); leftEar.lineTo(43, 29); leftEar.lineTo(57, 48);
        leftEar.closePath();
        Path2D rightEar = new Path2D.Double();
        rightEar.moveTo(79, 47); rightEar.lineTo(90, 30); rightEar.lineTo(94, 55);
        rightEar.closePath();
        g.setColor(fur); g.fill(leftEar); g.fill(rightEar);
        g.setColor(outline); g.draw(leftEar); g.draw(rightEar);
        g.setColor(new Color(0xF1, 0xA0, 0x84));
        g.fill(new Ellipse2D.Double(44, 35, 7, 12));
        g.fill(new Ellipse2D.Double(85, 36, 6, 12));

        g.setPaint(new GradientPaint(44, 45, furLight, 87, 80, fur));
        g.fill(new Ellipse2D.Double(31, 42, 67, 55));
        g.setColor(outline); g.draw(new Ellipse2D.Double(31, 42, 67, 55));
        g.setColor(cream); g.fill(new Ellipse2D.Double(45, 67, 45, 27));

        int eyeY = mood == PetMood.SUCCESS ? 62 : 59;
        g.setColor(new Color(0x2E, 0x20, 0x18));
        if (mood == PetMood.SUCCESS) {
            g.setStroke(new BasicStroke(2.7f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.drawArc(47, eyeY, 12, 7, 15, 150);
            g.drawArc(75, eyeY, 12, 7, 15, 150);
        } else {
            g.fill(new Ellipse2D.Double(49, eyeY, 8, 10));
            g.fill(new Ellipse2D.Double(77, eyeY, 8, 10));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(51, eyeY + 1, 2.5, 3));
            g.fill(new Ellipse2D.Double(79, eyeY + 1, 2.5, 3));
        }
        g.setColor(new Color(0x59, 0x2D, 0x25));
        g.fill(new Ellipse2D.Double(64, 72, 8, 6));
        g.setStroke(new BasicStroke(1.8f));
        g.drawArc(57, 76, 10, 8, 200, 125);
        g.drawArc(68, 76, 10, 8, 215, 125);
        g.setColor(new Color(0xF1, 0x91, 0x82, 150));
        g.fill(new Ellipse2D.Double(39, 72, 8, 5));
        g.fill(new Ellipse2D.Double(87, 72, 8, 5));

        g.setColor(DesignTokens.PRIMARY);
        g.fill(new RoundRectangle2D.Double(39, 84, 57, 11, 8, 8));
        g.setColor(DesignTokens.GOLD);
        g.fill(new RoundRectangle2D.Double(78, 89, 12, 31, 7, 7));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(42, 87, 92, 87);

        g.setColor(furLight);
        g.fill(new Ellipse2D.Double(29, 91, 23, 13));
        g.fill(new Ellipse2D.Double(83, 92, 23, 13));
        g.setColor(outline);
        g.draw(new Ellipse2D.Double(29, 91, 23, 13));
        g.draw(new Ellipse2D.Double(83, 92, 23, 13));
        g.setColor(fur);
        g.fill(new Ellipse2D.Double(30, 124, 29, 12));
        g.fill(new Ellipse2D.Double(75, 124, 29, 12));
    }

    private static void drawBubble(Graphics2D g, String text, PetMood mood) {
        if (text == null || text.isEmpty()) return;
        g.setFont(DesignTokens.medium(12));
        FontMetrics metrics = g.getFontMetrics();
        int width = Math.min(WIDTH - 12, metrics.stringWidth(text) + 22);
        int x = (WIDTH - width) / 2;
        Color border = mood == PetMood.ERROR || mood == PetMood.OFFLINE ? DesignTokens.ERROR
                : mood == PetMood.ATTENTION ? DesignTokens.WARNING
                : DesignTokens.PRIMARY;
        g.setColor(new Color(255, 255, 255, 242));
        g.fill(new RoundRectangle2D.Double(x, 3, width, 27, 14, 14));
        Path2D tail = new Path2D.Double();
        tail.moveTo(WIDTH / 2.0 - 4, 29);
        tail.lineTo(WIDTH / 2.0 + 4, 29);
        tail.lineTo(WIDTH / 2.0, 36);
        tail.closePath();
        g.fill(tail);
        g.setColor(border);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, 3, width, 27, 14, 14));
        g.setColor(DesignTokens.TEXT_PRIMARY);
        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, 21);
    }
}
