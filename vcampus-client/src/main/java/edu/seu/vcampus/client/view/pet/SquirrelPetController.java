package edu.seu.vcampus.client.view.pet;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.AppShell;
import edu.seu.vcampus.common.module.ModuleId;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** 管理窗口内悬浮形态和最小化后的桌面形态。 */
public final class SquirrelPetController implements PetInteractionHandler {
    private static final String PREF_X = "desktopX";
    private static final String PREF_Y = "desktopY";
    private static final String PREF_WINDOW_X = "windowX";
    private static final String PREF_WINDOW_Y = "windowY";
    private static final String PREF_AFFECTION = "affection";
    private static final int MISSING_POSITION = Integer.MIN_VALUE;

    private final JFrame frame;
    private final AppShell shell;
    private final PetStateModel state;
    private final PetLayer root;
    private final SquirrelPetWidget widget;
    private final WindowStateListener windowStateListener;
    private final Preferences preferences = safePreferences();
    private final AiAssistantClientService aiService;
    private final Timer connectivityTimer;
    private JWindow desktopWindow;
    private boolean desktopMode;
    private boolean disposed;
    private int restoreState = JFrame.NORMAL;
    private Point dragOrigin;

    public SquirrelPetController(JFrame frame, AppShell shell, PetStateModel state) {
        this(frame, shell, state, null);
    }

    public SquirrelPetController(JFrame frame, AppShell shell, PetStateModel state,
            AiAssistantClientService aiService) {
        this.frame = frame;
        this.shell = shell;
        this.state = state;
        this.aiService = aiService;
        widget = new SquirrelPetWidget(state, new SquirrelPetWidget.Listener() {
            @Override
            public void onPrimaryClick() {
                onPrimaryAction();
            }

            @Override
            public void onDragStarted() {
                dragOrigin = desktopMode && desktopWindow != null
                        ? desktopWindow.getLocation() : root.getPetLocation();
            }

            @Override
            public void onDragged(int deltaX, int deltaY) {
                if (desktopMode) moveDesktopPet(deltaX, deltaY);
                else moveWindowPet(deltaX, deltaY);
            }

            @Override
            public void onDragFinished() {
                if (desktopMode) persistDesktopPosition();
                else persistWindowPosition();
            }

            @Override
            public void onContextMenu(Component source, int x, int y) {
                showContextMenu(source, x, y);
            }
        });
        root = new PetLayer(shell, widget, loadWindowPosition());
        windowStateListener = new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent event) {
                if (!state.isAvailable() || disposed || desktopMode) return;
                if ((event.getNewState() & JFrame.ICONIFIED) != 0) {
                    restoreState = event.getOldState() & ~JFrame.ICONIFIED;
                    if (restoreState == 0) restoreState = JFrame.NORMAL;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            collapseToDesktopPet();
                        }
                    });
                }
            }
        };
        frame.addWindowStateListener(windowStateListener);
        state.addListener(new PetStateModel.Listener() {
            @Override
            public void onPetStateChanged() {
                applyAvailability();
            }
        });
        connectivityTimer = new Timer(12000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { probeConnectivity(); }
        });
        connectivityTimer.setCoalesce(true);
        if (aiService != null) connectivityTimer.start();
        applyAvailability();
        probeConnectivity();
    }

    public Container getRoot() {
        return root;
    }

    @Override
    public void onPrimaryAction() {
        if (desktopMode) {
            onRestoreRequested();
        } else if (shell.getActiveModule() == ModuleId.AI_ASSISTANT) {
            state.onMood(PetMood.SUCCESS, "我在这里！", 1200L);
        } else {
            shell.open(ModuleId.AI_ASSISTANT);
            state.onMood(PetMood.SUCCESS, "一起聊聊吧", 1400L);
        }
    }

    @Override
    public void onRestoreRequested() {
        if (!desktopMode || disposed) return;
        attachToMainWindow();
        frame.setExtendedState(restoreState);
        frame.setVisible(true);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setExtendedState(restoreState);
                frame.toFront();
                frame.requestFocus();
            }
        });
    }

    @Override
    public void onOpenAssistantRequested() {
        if (desktopMode) onRestoreRequested();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                shell.open(ModuleId.AI_ASSISTANT);
                state.onMood(PetMood.SUCCESS, "校园助手已打开", 1400L);
            }
        });
    }

    @Override
    public void onFeedRequested() {
        addAffection(2);
        state.onMood(PetMood.SUCCESS, "松果真香！好感 +2", 1900L);
    }

    @Override
    public void onPetRequested() {
        addAffection(1);
        state.onMood(PetMood.HOVER, "蹭蹭～好感 +1", 1700L);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        connectivityTimer.stop();
        frame.removeWindowStateListener(windowStateListener);
        widget.dispose();
        if (desktopWindow != null) {
            desktopWindow.setVisible(false);
            desktopWindow.dispose();
            desktopWindow = null;
        }
    }

    private void probeConnectivity() {
        if (disposed || aiService == null || !state.isAvailable()) return;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try { return Boolean.valueOf(aiService.ping()); }
                catch (Exception ex) { return Boolean.FALSE; }
            }
            @Override protected void done() {
                if (disposed) return;
                try { state.onConnectivityChanged(get().booleanValue()); }
                catch (Exception ex) { state.onConnectivityChanged(false); }
            }
        }.execute();
    }

    private void collapseToDesktopPet() {
        if (desktopMode || disposed || !state.isAvailable()) return;
        ensureDesktopWindow();
        root.detachPet();
        desktopWindow.getContentPane().removeAll();
        desktopWindow.getContentPane().add(widget);
        widget.setDesktopMode(true);
        desktopWindow.pack();
        desktopWindow.setLocation(loadDesktopPosition());
        desktopMode = true;
        frame.setVisible(false);
        desktopWindow.setVisible(true);
        desktopWindow.toFront();
    }

    private void attachToMainWindow() {
        if (!desktopMode) return;
        if (desktopWindow != null) {
            persistDesktopPosition();
            desktopWindow.setVisible(false);
            desktopWindow.getContentPane().remove(widget);
        }
        widget.setDesktopMode(false);
        root.attachPet();
        desktopMode = false;
    }

    private void ensureDesktopWindow() {
        if (desktopWindow != null) return;
        desktopWindow = new JWindow((Window) null);
        desktopWindow.setAlwaysOnTop(true);
        desktopWindow.setType(Window.Type.UTILITY);
        desktopWindow.getContentPane().setLayout(new java.awt.BorderLayout());
        try {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            boolean translucent = device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
            desktopWindow.setBackground(translucent
                    ? new Color(0, 0, 0, 0) : DesignTokens.PAGE_BACKGROUND);
        } catch (RuntimeException ignored) {
            desktopWindow.setBackground(DesignTokens.PAGE_BACKGROUND);
        }
    }

    private void moveDesktopPet(int deltaX, int deltaY) {
        if (desktopWindow == null || dragOrigin == null) return;
        Point wanted = new Point(dragOrigin.x + deltaX, dragOrigin.y + deltaY);
        desktopWindow.setLocation(PetInteractionMath.clamp(wanted,
                desktopWindow.getSize(), visibleScreens()));
    }

    private void moveWindowPet(int deltaX, int deltaY) {
        if (dragOrigin == null) return;
        root.setPetLocation(new Point(dragOrigin.x + deltaX, dragOrigin.y + deltaY));
    }

    private Point loadDesktopPosition() {
        Dimension size = desktopWindow.getSize();
        int x = readPreference(PREF_X);
        int y = readPreference(PREF_Y);
        Point wanted;
        if (x == MISSING_POSITION || y == MISSING_POSITION) {
            Rectangle screen = frame.getGraphicsConfiguration() == null
                    ? GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds()
                    : usableBounds(frame.getGraphicsConfiguration());
            wanted = new Point(screen.x + screen.width - size.width - 24,
                    screen.y + screen.height - size.height - 24);
        } else {
            wanted = new Point(x, y);
        }
        return PetInteractionMath.clamp(wanted, size, visibleScreens());
    }

    private void persistDesktopPosition() {
        if (desktopWindow == null || preferences == null) return;
        try {
            Point point = desktopWindow.getLocation();
            preferences.putInt(PREF_X, point.x);
            preferences.putInt(PREF_Y, point.y);
        } catch (SecurityException ignored) {
            // 受限环境不支持 Preferences 时仅放弃持久化。
        }
    }

    private Point loadWindowPosition() {
        int x = readPreference(PREF_WINDOW_X);
        int y = readPreference(PREF_WINDOW_Y);
        return x == MISSING_POSITION || y == MISSING_POSITION ? null : new Point(x, y);
    }

    private void persistWindowPosition() {
        if (preferences == null) return;
        try {
            Point point = root.getPetLocation();
            preferences.putInt(PREF_WINDOW_X, point.x);
            preferences.putInt(PREF_WINDOW_Y, point.y);
        } catch (SecurityException ignored) {
            // 受限环境不支持 Preferences 时仅放弃持久化。
        }
    }

    private int readPreference(String key) {
        if (preferences == null) return MISSING_POSITION;
        try {
            return preferences.getInt(key, MISSING_POSITION);
        } catch (SecurityException ignored) {
            return MISSING_POSITION;
        }
    }

    private void showContextMenu(Component source, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem feed = new JMenuItem("喂一颗松果");
        feed.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { onFeedRequested(); }
        });
        JMenuItem pet = new JMenuItem("摸摸小松鼠");
        pet.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { onPetRequested(); }
        });
        menu.add(feed); menu.add(pet);
        if (!desktopMode) { menu.show(source, x, y); return; }
        menu.addSeparator();
        JMenuItem restore = new JMenuItem("展开主界面");
        restore.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onRestoreRequested();
            }
        });
        JMenuItem openAssistant = new JMenuItem("打开校园助手");
        openAssistant.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onOpenAssistantRequested();
            }
        });
        menu.add(restore);
        menu.add(openAssistant);
        menu.show(source, x, y);
    }

    private void addAffection(int amount) {
        if (preferences == null) return;
        try {
            int current = preferences.getInt(PREF_AFFECTION, 0);
            preferences.putInt(PREF_AFFECTION, Math.min(100, Math.max(0, current + amount)));
        } catch (SecurityException ignored) {
            // 受限环境下互动动画仍可使用，只是不持久化好感度。
        }
    }

    private void applyAvailability() {
        if (disposed) return;
        boolean available = state.isAvailable();
        if (!available && desktopMode) onRestoreRequested();
        widget.setVisible(available);
        root.revalidate();
        root.repaint();
    }

    private static List<Rectangle> visibleScreens() {
        List<Rectangle> screens = new ArrayList<Rectangle>();
        try {
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getScreenDevices();
            for (GraphicsDevice device : devices) {
                for (GraphicsConfiguration configuration : device.getConfigurations()) {
                    Rectangle bounds = usableBounds(configuration);
                    if (!screens.contains(bounds)) screens.add(bounds);
                }
            }
        } catch (RuntimeException ignored) {
            screens.add(new Rectangle(0, 0, 1024, 768));
        }
        return screens;
    }

    private static Rectangle usableBounds(GraphicsConfiguration configuration) {
        Rectangle bounds = new Rectangle(configuration.getBounds());
        try {
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
        } catch (RuntimeException ignored) {
            // 无法读取任务栏边距时仍使用显示器边界。
        }
        return bounds;
    }

    private static Preferences safePreferences() {
        try {
            return Preferences.userNodeForPackage(SquirrelPetController.class);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    /** 保持业务布局尺寸不变，仅在更高层摆放桌宠。 */
    private static final class PetLayer extends JLayeredPane {
        private final Component content;
        private final Component pet;
        private Point petLocation;

        private PetLayer(Component content, Component pet, Point savedLocation) {
            this.content = content;
            this.pet = pet;
            this.petLocation = savedLocation == null ? null : new Point(savedLocation);
            add(content, JLayeredPane.DEFAULT_LAYER);
            add(pet, JLayeredPane.PALETTE_LAYER);
        }

        @Override
        public void doLayout() {
            content.setBounds(0, 0, getWidth(), getHeight());
            if (pet.getParent() == this) {
                Dimension size = pet.getPreferredSize();
                if (petLocation == null) {
                    petLocation = new Point(Math.max(0, getWidth() - size.width - 18),
                            Math.max(0, getHeight() - size.height - 18));
                }
                petLocation = PetInteractionMath.clampToContainer(petLocation, size, getSize());
                pet.setBounds(petLocation.x, petLocation.y, size.width, size.height);
            }
        }

        private Point getPetLocation() {
            return petLocation == null ? new Point(pet.getLocation()) : new Point(petLocation);
        }

        private void setPetLocation(Point wanted) {
            petLocation = PetInteractionMath.clampToContainer(wanted,
                    pet.getPreferredSize(), getSize());
            pet.setLocation(petLocation);
            repaint();
        }

        private void detachPet() {
            remove(pet);
            revalidate();
            repaint();
        }

        private void attachPet() {
            if (pet.getParent() != this) add(pet, JLayeredPane.PALETTE_LAYER);
            revalidate();
            repaint();
        }
    }
}
