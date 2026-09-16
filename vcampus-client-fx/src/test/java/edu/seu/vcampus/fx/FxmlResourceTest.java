package edu.seu.vcampus.fx;

import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import org.junit.Test;
import org.junit.BeforeClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 不启动 Swing，也不修改业务数据；验证预览页面资源可读且能由 FXMLLoader 构建。 */
public final class FxmlResourceTest {
    @BeforeClass
    public static void startJavaFxToolkit() {
        Platform.startup(() -> { });
    }

    @Test
    public void allViewsAreLoadable() throws IOException {
        for (String name : List.of("Login.fxml", "Main.fxml", "AcademicOverview.fxml", "Store.fxml")) {
            assertNotNull(name, FxPreviewApplication.resource(name));
            assertNotNull(name, new FXMLLoader(FxPreviewApplication.resource(name)).load());
        }
    }

    @Test
    public void stylesheetDefinesProductImagePresentation() throws IOException {
        try (InputStream stream = FxPreviewApplication.resource("app.css").openStream()) {
            String css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(css.contains("#3f6d35"));
            assertTrue(css.contains(".product-image"));
        }
    }

    @Test
    public void storeCardsUseUnsplashImageViewsWithLocalFallback() throws IOException {
        for (StoreController.Product product : StoreController.productsForTest()) {
            assertTrue(product.imageUrl().startsWith("https://images.unsplash.com/"));
        }
        FXMLLoader loader = new FXMLLoader(FxPreviewApplication.resource("Store.fxml"));
        loader.load();
        FlowPane grid = loader.<StoreController>getController().productGridForTest();
        assertEquals(4, grid.getChildren().size());
        for (Node cardNode : grid.getChildren()) {
            VBox card = (VBox) cardNode;
            StackPane imagePanel = (StackPane) card.getChildren().get(0);
            assertTrue(imagePanel.getChildren().stream().anyMatch(ImageView.class::isInstance));
        }
    }
}
