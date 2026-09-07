package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.pet.PetStateModel;
import edu.seu.vcampus.client.view.pet.SquirrelPetController;

import javax.swing.JFrame;
import java.awt.Dimension;

/** 登录成功后的主窗口。 */
public final class AppFrame extends JFrame {
    private SquirrelPetController petController;

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
        PetStateModel petState = new PetStateModel();
        AppShell shell = new AppShell(authService, session, new AppShell.Listener() {
            @Override
            public void onLogout() {
                if (listener != null) {
                    listener.onLogout();
                }
            }
        }, new edu.seu.vcampus.client.view.modules.AiAssistantPage.Factory() {
            @Override public AiAssistantClientService create() { return aiService; }
        }, businessServices, petState);
        petController = new SquirrelPetController(this, shell, petState, aiService);
        setContentPane(petController.getRoot());
    }

    @Override
    public void dispose() {
        if (petController != null) {
            petController.dispose();
            petController = null;
        }
        super.dispose();
    }
}
