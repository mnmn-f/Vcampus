package edu.seu.vcampus.client.view.pet;

import org.junit.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PetInteractionMathTest {
    @Test
    public void distinguishesClickFromDragAtFivePixels() {
        assertFalse(PetInteractionMath.isDrag(3, 4));
        assertTrue(PetInteractionMath.isDrag(4, 4));
    }

    @Test
    public void clampsPersistedPositionIntoVisibleMonitor() {
        Point clamped = PetInteractionMath.clamp(new Point(5000, -100),
                new Dimension(146, 154), Arrays.asList(
                        new Rectangle(0, 0, 1920, 1040),
                        new Rectangle(1920, 0, 1920, 1040)));
        assertEquals(new Point(3694, 0), clamped);
    }

    @Test
    public void clampsWindowPetInsideLayerAfterDragOrResize() {
        Dimension pet = new Dimension(146, 154);
        Dimension layer = new Dimension(900, 600);
        assertEquals(new Point(0, 446), PetInteractionMath.clampToContainer(
                new Point(-80, 900), pet, layer));
        assertEquals(new Point(754, 0), PetInteractionMath.clampToContainer(
                new Point(1200, -30), pet, layer));
    }
}
