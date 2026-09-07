package edu.seu.vcampus.server.library;

import edu.seu.vcampus.server.library.bootstrap.LibrarySchemaBootstrap;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 自动数据库升级所用的九张封面必须随服务端 JAR 一起发布。 */
public final class LibrarySchemaBootstrapResourcesTest {
    @Test public void nineSeedCoversAreReadablePngImages() throws Exception {
        String[] files = {"01-software.png", "02-java.png", "03-algorithm.png",
                "04-systems.png", "05-database.png", "06-red-chamber.png",
                "07-history.png", "08-solitude.png", "09-living.png"};
        for (String file : files) {
            try (InputStream input = LibrarySchemaBootstrap.class.getResourceAsStream(
                    "/library/covers/" + file)) {
                assertNotNull(file, input);
                BufferedImage image = ImageIO.read(input);
                assertNotNull(file, image);
                assertEquals(150, image.getWidth());
                assertEquals(220, image.getHeight());
            }
        }
    }
}
