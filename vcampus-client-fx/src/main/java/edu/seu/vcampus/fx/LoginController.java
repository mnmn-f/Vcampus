package edu.seu.vcampus.fx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** 登录页控制器；登录后由服务端身份回填区域直接进入学生主界面。 */
public final class LoginController {
    @FXML private TextField accountField;
    @FXML private PasswordField passwordField;
    @FXML private Label feedback;
    private Runnable onAuthenticated;

    @FXML
    private void initialize() {
        accountField.setText("2023012345");
        accountField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> submit());
    }

    @FXML
    private void submit() {
        if (accountField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty()) {
            feedback.setText("请输入校园账号和密码");
            return;
        }
        feedback.setText("");
        if (onAuthenticated != null) {
            onAuthenticated.run();
        }
    }

    void setOnAuthenticated(Runnable onAuthenticated) {
        this.onAuthenticated = onAuthenticated;
    }
}
