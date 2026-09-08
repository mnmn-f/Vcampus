package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.auth.ClientServiceException;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.controller.WorkspaceController;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.modules.AiAssistantPage;
import edu.seu.vcampus.client.view.pet.PetActivityListener;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;

/** 工作区组合器，协调侧栏、顶栏和当前页面，业务页面不依赖窗口。 */
public final class AppShell extends JPanel {
    private final AuthClientService authService;
    private final ClientSession session;
    private final Listener listener;
    private final AiAssistantPage.Factory aiFactory;
    private final SidebarPanel sidebar;
    private final TopBarPanel topbar;
    private final WorkspaceController controller;
    private final ClientBusinessServices businessServices;
    private final PetActivityListener petActivity;
    private final JPanel content = new JPanel(new BorderLayout());
    private ModuleId activeModule = ModuleId.DASHBOARD;
    private boolean roleSwitching;
    private boolean loggingOut;

    public interface Listener {
        void onLogout();
    }

    public AppShell(AuthClientService authService, final ClientSession session, Listener listener,
                    AiAssistantPage.Factory aiFactory) {
        this(authService, session, listener, aiFactory, null);
    }

    public AppShell(AuthClientService authService, final ClientSession session, Listener listener,
                    AiAssistantPage.Factory aiFactory,
                    ClientBusinessServices businessServices) {
        this(authService, session, listener, aiFactory, businessServices, null);
    }

    public AppShell(AuthClientService authService, final ClientSession session, Listener listener,
                    AiAssistantPage.Factory aiFactory,
                    ClientBusinessServices businessServices,
                    PetActivityListener petActivity) {
        super(new BorderLayout());
        this.authService = authService;
        this.session = session;
        this.listener = listener;
        this.businessServices = businessServices;
        this.petActivity = petActivity == null ? PetActivityListener.NONE : petActivity;
        this.aiFactory = aiFactory == null ? new AiAssistantPage.Factory() {
            @Override public AiAssistantClientService create() {
                return new DisabledAiAssistantClientService();
            }
        } : aiFactory;
        this.controller = new WorkspaceController(session, this.aiFactory, businessServices, new Runnable() {
            @Override public void run() { logout(); }
        }, this.petActivity);
        setBackground(DesignTokens.PAGE_BACKGROUND);
        sidebar = new SidebarPanel(new SidebarPanel.ClientSessionView() {
            @Override public Role activeRole() { return session.getActiveRole(); }
        }, new SidebarPanel.Listener() {
            @Override public void onModuleSelected(ModuleId moduleId) { open(moduleId); }
        });
        topbar = new TopBarPanel(authService, new TopBarPanel.SessionView() {
            @Override public java.util.Set<Role> roles() { return session.getRoles(); }
            @Override public Role activeRole() { return session.getActiveRole(); }
            @Override public String displayName() { return session.getDisplayName(); }
            @Override public String account() { return session.getAccount(); }
            @Override public long userId() { return session.getUserId(); }
        }, new TopBarPanel.Listener() {
            @Override public void onRoleChanged(Role role) { changeRole(role); }
            @Override public void onLogout() { logout(); }
        });
        add(sidebar, BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(DesignTokens.PAGE_BACKGROUND);
        main.add(topbar, BorderLayout.NORTH);
        content.setOpaque(false);
        main.add(content, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        refreshForRole();
    }

    public void refreshForRole() {
        activeModule = ModuleId.DASHBOARD;
        sidebar.refresh(activeModule);
        topbar.syncRole();
        open(activeModule);
        petActivity.onAvailabilityChanged(controller.canOpen(ModuleId.AI_ASSISTANT));
    }

    public ModuleId getActiveModule() {
        return activeModule;
    }

    public void open(ModuleId module) {
        if (!controller.canOpen(module)) {
            return;
        }
        activeModule = module;
        topbar.setActiveModule(module);
        content.removeAll();
        content.add(controller.pageFor(module, new WorkbenchPanel.NavigationHandler() {
            @Override public void open(ModuleId moduleId) { AppShell.this.open(moduleId); }
        }), BorderLayout.CENTER);
        sidebar.setActive(module);
        content.revalidate();
        content.repaint();
    }

    private void changeRole(Role role) {
        if (role == null || roleSwitching || role == session.getActiveRole()) return;
        final Role requestedRole = role;
        roleSwitching = true;
        new SwingWorker<Role, Void>() {
            private ClientServiceException failure;

            @Override protected Role doInBackground() {
                try { return authService.switchRole(requestedRole); }
                catch (ClientServiceException ex) { failure = ex; return null; }
            }

            @Override protected void done() {
                try {
                    if (failure != null) {
                        topbar.syncRole();
                        showCurrentPageError(failure.getMessage());
                        return;
                    }
                    session.switchRole(get());
                    if (businessServices != null) businessServices.synchronizeSession();
                    refreshForRole();
                } catch (Exception ex) {
                    topbar.syncRole();
                    showCurrentPageError("职责切换失败，请稍后重试。");
                } finally {
                    roleSwitching = false;
                }
            }
        }.execute();
    }

    private void showCurrentPageError(String message) {
        if (content.getComponentCount() == 0) return;
        java.awt.Component current = content.getComponent(0);
        if (current instanceof BasePage) ((BasePage) current).showError(message);
    }

    private void logout() {
        if (loggingOut) return;
        loggingOut = true;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                authService.logout();
                return null;
            }

            @Override protected void done() {
                session.close();
                if (businessServices != null) businessServices.clearSession();
                loggingOut = false;
                if (listener != null) listener.onLogout();
            }
        }.execute();
    }
}
