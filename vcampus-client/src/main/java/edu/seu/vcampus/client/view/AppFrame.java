package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;

import javax.swing.JFrame;
import java.awt.Dimension;

/** 登录成功后的主窗口。 */
public final class AppFrame extends JFrame {
    public interface Listener {
        void onLogout();
    }

    public AppFrame(AuthClientService authService, ClientSession session,
                    Listener listener, AiAssistantClientService aiService) {
        this(authService, session, listener, aiService, null);
    }

    public AppFrame(AuthClientService authService, ClientSession session,
                    final Listener listener, final AiAssistantClientService aiService,
                    ClientBusinessServices businessServices) {
        super("东南大学虚拟校园");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        AppShell shell = new AppShell(authService, session, new AppShell.Listener() {
            @Override
            public void onLogout() {
                if (listener != null) {
                    listener.onLogout();
                }
            }
        }, new edu.seu.vcampus.client.view.modules.AiAssistantPage.Factory() {
            @Override public AiAssistantClientService create() { return aiService; }
        }, businessServices);
        setContentPane(shell);
    }
}
