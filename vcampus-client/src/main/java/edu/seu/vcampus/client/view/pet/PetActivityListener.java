package edu.seu.vcampus.client.view.pet;

/** AI 页面向桌宠发送的最小状态接口。 */
public interface PetActivityListener {
    PetActivityListener NONE = new PetActivityListener() {
        @Override
        public void onMood(PetMood mood, String bubble, long durationMillis) {
        }
    };

    void onMood(PetMood mood, String bubble, long durationMillis);

    default void onAvailabilityChanged(boolean available) {
    }

    default void onConnectivityChanged(boolean online) {
    }
}
