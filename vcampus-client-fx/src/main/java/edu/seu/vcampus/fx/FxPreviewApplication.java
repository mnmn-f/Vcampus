package edu.seu.vcampus.fx;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/** JavaFX 视觉预览入口；独立于现有 Swing AppLauncher。 */
public final class FxPreviewApplication extends Application {
    private static final String RESOURCE_ROOT = "/edu/seu/vcampus/fx/";

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        showLogin(stage);
    }

    private void showLogin(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(resource("Login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setOnAuthenticated(() -> showMain(stage));
        stage.setTitle("VCampus · 东南大学校园服务");
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setScene(scene(root));
        stage.show();
    }

    private void showMain(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(resource("Main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setOnLogout(() -> {
                try {
                    showLogin(stage);
                } catch (IOException exception) {
                    throw new IllegalStateException("无法返回登录页", exception);
                }
            });
            stage.setScene(scene(root));
            stage.setTitle("VCampus · 林知远");
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载主页面", exception);
        }
    }

    private Scene scene(Parent root) {
        Scene scene = new Scene(root, 1240, 800);
        scene.getStylesheets().add(resource("app.css").toExternalForm());
        return scene;
    }

    static java.net.URL resource(String name) {
        return Objects.requireNonNull(FxPreviewApplication.class.getResource(RESOURCE_ROOT + name),
                "缺少 JavaFX 资源: " + name);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
