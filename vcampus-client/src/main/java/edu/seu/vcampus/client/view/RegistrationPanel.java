package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.modules.real.AsyncTask;
import edu.seu.vcampus.client.view.modules.real.RealUi;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** 网络模式登录页的匿名注册入口；注册请求由身份服务异步发送。 */
public final class RegistrationPanel extends SectionCard {
    private final IdentityClientService service; private final Runnable back;
    private final JTextField account = UiFactory.textField(18); private final JTextField name = UiFactory.textField(18);
    private final JTextField email = UiFactory.textField(18); private final JPasswordField password = password();
    private final JPasswordField confirm = password(); private final JLabel error = UiFactory.muted(" ");

    public RegistrationPanel(IdentityClientService service, final Runnable back) {
        super("注册校园账号", "注册成功后默认获得学生角色，请返回登录页使用新账号登录。"); this.service = service; this.back = back;
        UiFactory.styleLoginField(account); UiFactory.styleLoginField(name); UiFactory.styleLoginField(email);
        JPanel fields = fields();
        JPanel actions = UiFactory.horizontal(8); JButton submit = new PrimaryButton("提交注册"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton cancel = new SecondaryButton("返回登录"); cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (back != null) back.run(); }
        }); actions.add(submit); actions.add(cancel); actions.add(error);
        JPanel body = UiFactory.vertical(12); body.add(fields); body.add(actions); setContent(body);
    }

    private JPanel fields() {
        JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = 0;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTH;
        add(fields, c, "校园账号", account); add(fields, c, "显示名", name);
        add(fields, c, "邮箱（可选）", email); add(fields, c, "登录密码", password);
        add(fields, c, "确认密码", confirm); return fields;
    }

    private static void add(JPanel panel, GridBagConstraints c, String label,
                            java.awt.Component field) {
        c.insets = new Insets(0, 0, 11, 0);
        panel.add(UiFactory.labelledField(label, field), c); c.gridy++;
    }

    private void submit() {
        try {
            String pass = new String(password.getPassword()); if (!pass.equals(new String(confirm.getPassword()))) throw new IllegalArgumentException("两次密码不一致");
            final RegistrationRequest request = new RegistrationRequest(RealUi.required(account.getText(), "校园账号"),
                    RealUi.required(pass, "登录密码"), RealUi.required(name.getText(), "显示名"), RealUi.optional(email.getText()), null);
            error.setText("正在提交…");
            AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
                @Override public ProfileDto run() throws Exception { return service.register(request); }
            }, new AsyncTask.Callback<ProfileDto>() {
                @Override public void onSuccess(ProfileDto value) { error.setText("注册成功，请登录"); clear(); if (back != null) back.run(); }
                @Override public void onFailure(Throwable error) { RegistrationPanel.this.error.setText(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private void clear() { account.setText(""); name.setText(""); email.setText(""); password.setText(""); confirm.setText(""); }
    private static JPasswordField password() { JPasswordField value = new JPasswordField(18); UiFactory.styleLoginField(value); value.setFont(DesignTokens.regular(14)); return value; }
}
