package edu.seu.vcampus.fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/** 学生主框架控制器，负责页面切换和登出。 */
public final class MainController {
    @FXML private StackPane contentHost;
    @FXML private Button academicButton;
    @FXML private Button storeButton;
    private Runnable onLogout;

    @FXML
    private void initialize() {
        showAcademic();
    }

    @FXML
    private void showAcademic() {
        show("AcademicOverview.fxml", academicButton);
    }

    @FXML
    private void showStore() {
        show("Store.fxml", storeButton);
    }

    @FXML
    private void logout() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    private void show(String resourceName, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(FxPreviewApplication.resource(resourceName));
            Node page = loader.load();
            contentHost.getChildren().setAll(page);
            academicButton.getStyleClass().remove("nav-active");
            storeButton.getStyleClass().remove("nav-active");
            activeButton.getStyleClass().add("nav-active");
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载页面: " + resourceName, exception);
        }
    }
}
