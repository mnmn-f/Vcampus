package edu.seu.vcampus.client.view.pet;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SquirrelPetWidgetTest {
    @Test
    public void rendersOffscreenWithTransparentBackground() throws Exception {
        final BufferedImage[] rendered = new BufferedImage[1];
        final boolean[] rasterAsset = new boolean[1];
        final boolean[] actionSheet = new boolean[1];
        String oldAnimation = System.getProperty("vcampus.pet.animation");
        System.setProperty("vcampus.pet.animation", "false");
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    PetStateModel model = new PetStateModel();
                    model.onMood(PetMood.TALKING, "正在回答…", 0L);
                    SquirrelPetWidget widget = new SquirrelPetWidget(model, null);
                    rasterAsset[0] = widget.isUsingRasterAsset();
                    actionSheet[0] = widget.isUsingActionSheet();
                    widget.setSize(widget.getPreferredSize());
                    BufferedImage canvas = new BufferedImage(SquirrelPetWidget.WIDTH,
                            SquirrelPetWidget.HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = canvas.createGraphics();
                    widget.paint(graphics);
                    graphics.dispose();
                    widget.dispose();
                    rendered[0] = canvas;
                    try {
                        File directory = new File("target/ui-previews");
                        directory.mkdirs();
                        ImageIO.write(canvas, "png",
                                new File(directory, "squirrel-pet.png"));
                    } catch (Exception ex) {
                        throw new AssertionError("桌宠预览写入失败", ex);
                    }
                }
            });
        } finally {
            if (oldAnimation == null) System.clearProperty("vcampus.pet.animation");
            else System.setProperty("vcampus.pet.animation", oldAnimation);
        }
        assertTrue(rasterAsset[0]);
        assertTrue(actionSheet[0]);
        URL resource = SquirrelPetWidget.class.getResource(
                "/edu/seu/vcampus/client/pet/squirrel.png");
        assertTrue(resource != null);
        assertTrue(ImageIO.read(resource).getColorModel().hasAlpha());
        URL actionResource = SquirrelPetWidget.class.getResource(
                "/edu/seu/vcampus/client/pet/squirrel-actions.png");
        assertTrue(actionResource != null);
        BufferedImage actions = ImageIO.read(actionResource);
        assertTrue(actions.getColorModel().hasAlpha());
        assertEquals(0, actions.getRGB(0, 0) >>> 24);
        assertEquals(0, actions.getWidth() % 3);
        assertEquals(0, actions.getHeight() % 2);
        int opaquePixels = 0;
        int transparentPixels = 0;
        for (int y = 0; y < rendered[0].getHeight(); y++) {
            for (int x = 0; x < rendered[0].getWidth(); x++) {
                int alpha = rendered[0].getRGB(x, y) >>> 24;
                if (alpha == 0) transparentPixels++;
                else opaquePixels++;
            }
        }
        assertEquals(SquirrelPetWidget.WIDTH, rendered[0].getWidth());
        assertTrue(opaquePixels > 2500);
        assertTrue(transparentPixels > 2500);
    }

    @Test
    public void selectsDistinctFramesForPetMoods() {
        assertEquals(0, SquirrelPetWidget.actionFrameFor(PetMood.IDLE, 0L));
        assertEquals(1, SquirrelPetWidget.actionFrameFor(PetMood.HOVER, 0L));
        assertEquals(0, SquirrelPetWidget.actionFrameFor(PetMood.HOVER, 420L));
        assertEquals(0, SquirrelPetWidget.actionFrameFor(PetMood.THINKING, 0L));
        assertEquals(1, SquirrelPetWidget.actionFrameFor(PetMood.TALKING, 0L));
        assertEquals(0, SquirrelPetWidget.actionFrameFor(PetMood.TALKING, 180L));
        assertEquals(3, SquirrelPetWidget.actionFrameFor(PetMood.ATTENTION, 0L));
        assertEquals(4, SquirrelPetWidget.actionFrameFor(PetMood.SUCCESS, 0L));
        assertEquals(5, SquirrelPetWidget.actionFrameFor(PetMood.ERROR, 0L));
        assertEquals(5, SquirrelPetWidget.actionFrameFor(PetMood.OFFLINE, 0L));
    }
}
