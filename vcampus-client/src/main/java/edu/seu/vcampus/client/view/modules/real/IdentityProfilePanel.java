package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 所有已登录角色共用的个人资料和修改密码卡片。 */
public final class IdentityProfilePanel extends JPanel {
    private final BasePage page; private final IdentityClientService service;
    private final JTextField displayName = field(); private final JTextField email = field();
    private final JTextField phone = field(); private String avatarData;
    private final JPasswordField currentPassword = password(); private final JPasswordField newPassword = password();
    private final JPasswordField confirmPassword = password(); private final JLabel state = UiFactory.muted("正在加载个人资料…");
    private final AvatarImageView avatarPreview = new AvatarImageView();
    private final SectionCard passwordSettings;
    private final Runnable passwordChanged;
    private final ClientSession session;

    public IdentityProfilePanel(BasePage page, IdentityClientService service) {
        this(page, service, null);
    }

    public IdentityProfilePanel(BasePage page, IdentityClientService service, Runnable passwordChanged) {
        this(page, null, service, passwordChanged);
    }

    public IdentityProfilePanel(BasePage page, ClientSession session,
                                IdentityClientService service, Runnable passwordChanged) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.session = session; this.service = service; this.passwordChanged = passwordChanged;
        add(profileCard()); passwordSettings = passwordCard(); passwordSettings.setVisible(false);
        add(passwordSettings); load();
    }

    private SectionCard profileCard() {
        SectionCard card = new SectionCard("个人资料", "编辑姓名和联系方式；头像可从本地图片上传。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("显示名", displayName)); fields.add(UiFactory.labelledField("邮箱", email));
        fields.add(UiFactory.labelledField("手机号", phone));
        JPanel body = new JPanel(new BorderLayout(18, 10)); body.setOpaque(false);
        body.add(avatarArea(), BorderLayout.WEST); body.add(fields, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton refresh = new SecondaryButton("刷新"); refresh.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { load(); }
        });
        JButton save = new PrimaryButton("保存资料"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(state); actions.add(refresh); actions.add(save); body.add(actions, BorderLayout.SOUTH);
        card.setContent(body); return card;
    }

    private JPanel avatarArea() {
        JPanel area = new JPanel(new BorderLayout(0, 7)); area.setOpaque(false);
        area.add(avatarPreview, BorderLayout.CENTER);
        JButton upload = new SecondaryButton("上传头像");
        upload.addActionListener(e -> uploadAvatar());
        JButton remove = UiFactory.linkButton("移除");
        remove.addActionListener(e -> { avatarData = null; avatarPreview.showProfile(displayName.getText(), null); });
        JPanel actions = UiFactory.horizontal(2); actions.add(upload); actions.add(remove);
        area.add(actions, BorderLayout.SOUTH); return area;
    }

    private SectionCard passwordCard() {
        SectionCard card = new SectionCard("修改密码", "验证当前密码后修改；修改后重新登录。");
        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("当前密码", currentPassword)); fields.add(UiFactory.labelledField("新密码", newPassword));
        fields.add(UiFactory.labelledField("确认新密码", confirmPassword));
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false); body.add(fields, BorderLayout.CENTER);
        JButton change = new PrimaryButton("更新密码"); change.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { changePassword(); }
        });
        JButton close = UiFactory.linkButton("收起"); close.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { hidePasswordSettings(); }
        });
        JPanel actions = UiFactory.horizontal(8); actions.add(change); actions.add(close);
        body.add(actions, BorderLayout.SOUTH); card.setContent(body); return card;
    }

    public void showPasswordSettings() {
        passwordSettings.setVisible(true); revalidate(); repaint(); currentPassword.requestFocusInWindow();
    }

    public void hidePasswordSettings() {
        passwordSettings.setVisible(false); currentPassword.setText(""); newPassword.setText("");
        confirmPassword.setText(""); revalidate(); repaint();
    }

    boolean isPasswordSettingsVisible() { return passwordSettings.isVisible(); }

    private void load() {
        state.setText("正在加载个人资料…");
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.getOwnProfile(); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) {
                if (session != null) session.updateDisplayName(value.getDisplayName());
                displayName.setText(RealUi.input(value.getDisplayName())); email.setText(RealUi.input(value.getEmail()));
                phone.setText(RealUi.input(value.getPhone())); avatarData = value.getAvatarUrl();
                avatarPreview.showProfile(value.getDisplayName(), avatarData);
                state.setText("资料状态：" + RealUi.status(value.getStatus())); page.showSuccess("个人资料已加载。");
            }
            @Override public void onFailure(Throwable error) { state.setText("资料加载失败"); page.showError(AsyncTask.message(error)); }
        });
    }

    private void save() {
        try {
            final String name = RealUi.required(displayName.getText(), "显示名");
            final String mobile = RealUi.optional(phone.getText());
            if (mobile != null && !mobile.matches("[0-9]{6,32}")) {
                throw new IllegalArgumentException("手机号只能填写数字");
            }
            final ProfileUpdateRequest request = new ProfileUpdateRequest(name, RealUi.optional(email.getText()), mobile, avatarData);
            AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
                @Override public ProfileDto run() throws Exception { return service.updateProfile(request); }
            },
                    new AsyncTask.Callback<ProfileDto>() {
                        @Override public void onSuccess(ProfileDto value) {
                            if (session != null) session.updateDisplayName(value.getDisplayName());
                            avatarPreview.showProfile(value.getDisplayName(), value.getAvatarUrl());
                            state.setText("资料已保存"); page.showSuccess("个人资料已保存。");
                        }
                        @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                    });
        } catch (IllegalArgumentException ex) { page.showError(ex.getMessage()); }
    }

    private void uploadAvatar() {
        try {
            String encoded = AvatarUploadSupport.chooseAndEncode(this);
            if (encoded == null) return;
            avatarData = encoded; avatarPreview.showProfile(displayName.getText(), avatarData);
            state.setText("头像已选择，请保存资料");
        } catch (Exception ex) { page.showError(ex.getMessage()); }
    }

    private void changePassword() {
        final String old = new String(currentPassword.getPassword()); final String next = new String(newPassword.getPassword());
        if (old.trim().isEmpty() || next.trim().isEmpty()) { page.showWarning("请填写当前密码和新密码。"); return; }
        if (!next.equals(new String(confirmPassword.getPassword()))) { page.showWarning("两次输入的新密码不一致。"); return; }
        if (next.length() < 8 || next.length() > 72 || !next.matches(".*[A-Z].*")
                || !next.matches(".*[a-z].*") || !next.matches(".*[0-9].*")) {
            page.showWarning("新密码至少8位且须包含大小写字母和数字。"); return;
        }
        final PasswordChangeRequest request = new PasswordChangeRequest(old, next);
        AsyncTask.run(new AsyncTask.Work<ProfileDto>() {
            @Override public ProfileDto run() throws Exception { return service.changePassword(request); }
        }, new AsyncTask.Callback<ProfileDto>() {
            @Override public void onSuccess(ProfileDto value) { currentPassword.setText(""); newPassword.setText(""); confirmPassword.setText(""); page.showInfo("密码已修改，请重新登录。"); if (passwordChanged != null) passwordChanged.run(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static JTextField field() { return UiFactory.textField(14); }
    private static JPasswordField password() { JPasswordField value = new JPasswordField(14); UiFactory.styleField(value); value.setFont(DesignTokens.regular(14)); return value; }
}
