package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.controller.LoginController;
import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.common.dto.auth.LoginResult;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.awt.CardLayout;

/** 登录窗口只负责布局和会话切换，表单逻辑由 LoginFormPanel 负责。 */
public final class LoginFrame extends JFrame {
    public interface Listener {
        void onLoginSuccess(LoginResult result);
    }

    private final ClientSession session;

    public LoginFrame(AuthClientService authService, ClientSession session, Listener listener) {
        this(authService, session, listener, null);
    }

    public LoginFrame(AuthClientService authService, ClientSession session, Listener listener,
                      IdentityClientService identityService) {
        super("VCampus · 东南大学虚拟校园");
        if (authService == null || session == null) {
            throw new IllegalArgumentException("认证服务和会话不能为空");
        }
        this.session = session;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(980, 640));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setContentPane(buildContent(authService, listener, identityService));
    }

    private JPanel buildContent(final AuthClientService authService, final Listener listener,
                                final IdentityClientService identityService) {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(DesignTokens.PAGE_BACKGROUND);
        root.add(new LoginBrandPanel());
        final CardLayout layout = new CardLayout();
        final JPanel forms = new JPanel(layout); forms.setOpaque(false);
        LoginFormPanel form = new LoginFormPanel(new LoginController(authService), new LoginFormPanel.Listener() {
            @Override public void onLoginSuccess(LoginResult result) {
                session.open(result);
                if (listener != null) listener.onLoginSuccess(result);
            }
        }, identityService == null ? null : new Runnable() {
            @Override public void run() { layout.show(forms, "register"); }
        });
        forms.add(form, "login");
        if (identityService != null) {
            forms.add(new RegistrationPanel(identityService,
                    new Runnable() {
                        @Override public void run() { layout.show(forms, "login"); }
                    }), "register");
        }
        root.add(forms);
        getRootPane().setDefaultButton(form.getLoginButton());
        return root;
    }
}
