package edu.seu.vcampus.client.view.pet;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 线程安全的桌宠状态；监听回调统一投递到 EDT。 */
public final class PetStateModel implements PetActivityListener {
    public interface Listener {
        void onPetStateChanged();
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private volatile PetMood mood = PetMood.IDLE;
    private volatile String bubble = "";
    private volatile long expiresAt = Long.MAX_VALUE;
    private volatile boolean available;
    private volatile boolean online = true;

    @Override
    public void onMood(PetMood value, String message, long durationMillis) {
        if (!online && value != PetMood.OFFLINE) return;
        mood = value == null ? PetMood.IDLE : value;
        bubble = message == null ? "" : message;
        expiresAt = durationMillis > 0
                ? System.currentTimeMillis() + durationMillis : Long.MAX_VALUE;
        notifyListeners();
    }

    @Override
    public void onAvailabilityChanged(boolean value) {
        available = value;
        if (!value) {
            mood = PetMood.IDLE;
            bubble = "";
            expiresAt = Long.MAX_VALUE;
        }
        notifyListeners();
    }

    @Override
    public void onConnectivityChanged(boolean value) {
        online = value;
        if (!value) {
            mood = PetMood.OFFLINE;
            bubble = "网络离线";
            expiresAt = Long.MAX_VALUE;
        } else if (mood == PetMood.OFFLINE) {
            mood = PetMood.IDLE;
            bubble = "已重新连接";
            expiresAt = System.currentTimeMillis() + 1600L;
        }
        notifyListeners();
    }

    public PetMood getMood() {
        return mood;
    }

    public String getBubble() {
        return bubble;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isOnline() { return online; }

    public void tick(long now) {
        if (expiresAt != Long.MAX_VALUE && now >= expiresAt) {
            mood = PetMood.IDLE;
            bubble = "";
            expiresAt = Long.MAX_VALUE;
            notifyListeners();
        }
    }

    public void addListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        Runnable notification = new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) listener.onPetStateChanged();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) notification.run();
        else SwingUtilities.invokeLater(notification);
    }
}
