package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.ClientServiceException;
import edu.seu.vcampus.client.controller.LoginController;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.FeedbackBanner;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.auth.LoginResult;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
/** 登录表单及其页面内反馈。 */
public final class LoginFormPanel extends JPanel {
    public interface Listener {
        void onLoginSuccess(LoginResult result);
    }

    private final LoginController controller;
    private final Listener listener;
    private final Runnable registerAction;
    private final JTextField accountField = UiFactory.textField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JLabel accountError = errorLabel();
    private final JLabel passwordError = errorLabel();
    private final FeedbackBanner feedback = new FeedbackBanner();
    private final JButton loginButton = new PrimaryButton("登录");

    public LoginFormPanel(LoginController controller, Listener listener) {
        this(controller, listener, null);
    }

    public LoginFormPanel(LoginController controller, Listener listener, Runnable registerAction) {
        if (controller == null) {
            throw new IllegalArgumentException("登录控制器不能为空");
        }
        this.controller = controller;
        this.listener = listener;
        this.registerAction = registerAction;
        UiFactory.styleLoginField(accountField);
        UiFactory.styleLoginField(passwordField);
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(feedback);
        add(Box.createVerticalStrut(12));
        SectionCard form = new SectionCard("登录", null);
        form.setContent(buildFields());
        add(form);
        if (registerAction != null) {
            add(Box.createVerticalStrut(10));
            add(buildLinks());
        }
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public JTextField getAccountField() {
        return accountField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }
    private JPanel buildFields() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        fields.add(UiFactory.body("校园账号 / 学号 / 工号"), c);
        c.gridy++;
        fields.add(accountField, c);
        c.gridy++;
        fields.add(accountError, c);
        c.gridy++;
        c.insets = new Insets(8, 0, 5, 0);
        fields.add(UiFactory.body("登录密码"), c);
        c.gridy++;
        fields.add(passwordField, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        fields.add(passwordError, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        fields.add(loginButton, c);
        loginButton.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { login(); }
        });
        return fields;
    }

    private JPanel buildLinks() {
        JPanel links = UiFactory.horizontal(0);
        JButton register = UiFactory.linkButton("注册账号");
        register.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { registerAction.run(); }
        });
        links.add(register);
        return links;
    }

    private void login() {
        clearErrors();
        final String account = accountField.getText() == null ? "" : accountField.getText().trim();
        final String password = new String(passwordField.getPassword());
        if (account.length() == 0) {
            accountError.setText("请输入校园账号、学号或工号");
        }
        if (password.length() == 0) {
            passwordError.setText("请输入登录密码");
        }
        if (account.length() == 0 || password.length() == 0) {
            feedback.show(FeedbackBanner.Type.ERROR, "请先完善登录信息。");
            return;
        }
        setLoading(true);
        controller.login(account, password, new LoginController.Callback() {
            @Override
            public void onSuccess(LoginResult result) {
                setLoading(false);
                feedback.show(FeedbackBanner.Type.SUCCESS, "登录成功");
                if (listener != null) {
                    listener.onLoginSuccess(result);
                }
            }

            @Override
            public void onFailure(ClientServiceException error) {
                setLoading(false);
                feedback.show(FeedbackBanner.Type.ERROR, error.getMessage());
                passwordError.setText("请检查账号和密码");
            }
        });
    }

    private void clearErrors() {
        accountError.setText("");
        passwordError.setText("");
        feedback.hideBanner();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        accountField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        loginButton.setText(loading ? "登录中…" : "登录");
        if (loading) {
            feedback.show(FeedbackBanner.Type.INFO, "正在验证身份，请稍候。");
        }
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel();
        label.setFont(DesignTokens.regular(11));
        label.setForeground(DesignTokens.ERROR);
        return label;
    }
}
