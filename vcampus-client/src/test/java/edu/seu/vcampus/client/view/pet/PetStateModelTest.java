package edu.seu.vcampus.client.view.pet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PetStateModelTest {
    @Test
    public void transientMoodReturnsToIdle() {
        PetStateModel model = new PetStateModel();
        model.onAvailabilityChanged(true);
        model.onMood(PetMood.SUCCESS, "办好啦！", 10L);
        assertEquals(PetMood.SUCCESS, model.getMood());
        model.tick(System.currentTimeMillis() + 20L);
        assertEquals(PetMood.IDLE, model.getMood());
        assertEquals("", model.getBubble());
    }

    @Test
    public void removingPermissionClearsVisibleState() {
        PetStateModel model = new PetStateModel();
        model.onAvailabilityChanged(true);
        model.onMood(PetMood.THINKING, "让我查查～", 0L);
        assertTrue(model.isAvailable());
        model.onAvailabilityChanged(false);
        assertFalse(model.isAvailable());
        assertEquals(PetMood.IDLE, model.getMood());
    }

    @Test
    public void disconnectedNetworkHasPersistentOfflineMoodUntilRecovery() {
        PetStateModel model = new PetStateModel();
        model.onAvailabilityChanged(true);
        model.onConnectivityChanged(false);
        assertFalse(model.isOnline());
        assertEquals(PetMood.OFFLINE, model.getMood());
        assertEquals("网络离线", model.getBubble());
        model.onMood(PetMood.SUCCESS, "不应覆盖", 10L);
        assertEquals(PetMood.OFFLINE, model.getMood());
        model.onConnectivityChanged(true);
        assertTrue(model.isOnline());
        assertEquals(PetMood.IDLE, model.getMood());
    }
}
