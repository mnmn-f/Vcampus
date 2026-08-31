package edu.seu.vcampus.client.controller;

import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.PersonalCenterPage;
import edu.seu.vcampus.client.view.WorkbenchPanel;
import edu.seu.vcampus.client.view.modules.AiAssistantPage;
import edu.seu.vcampus.client.view.modules.ModulePages;
import edu.seu.vcampus.common.module.ModuleId;

/** 工作区控制器：统一执行导航授权检查并创建页面。 */
public final class WorkspaceController {
    private final ClientSession session;
    private final AiAssistantPage.Factory aiFactory;
    private final ClientBusinessServices businessServices;
    private final Runnable passwordChanged;

    public WorkspaceController(ClientSession session, AiAssistantPage.Factory aiFactory) {
        this(session, aiFactory, null);
    }

    public WorkspaceController(ClientSession session, AiAssistantPage.Factory aiFactory,
                               ClientBusinessServices businessServices) {
        this(session, aiFactory, businessServices, null);
    }

    public WorkspaceController(ClientSession session, AiAssistantPage.Factory aiFactory,
                               ClientBusinessServices businessServices, Runnable passwordChanged) {
        if (session == null) {
            throw new IllegalArgumentException("session 不能为空");
        }
        this.session = session;
        this.aiFactory = aiFactory == null ? new AiAssistantPage.Factory() {
            @Override public AiAssistantClientService create() {
                return new DisabledAiAssistantClientService();
            }
        } : aiFactory;
        this.businessServices = businessServices;
        this.passwordChanged = passwordChanged;
    }

    public boolean canOpen(ModuleId module) {
        return module != null && session.isAuthenticated()
                && module.isVisibleTo(session.getActiveRole());
    }

    public BasePage pageFor(ModuleId module, WorkbenchPanel.NavigationHandler navigation) {
        if (!canOpen(module)) {
            throw new IllegalArgumentException("当前职责无权访问该模块");
        }
        if (module == ModuleId.DASHBOARD) {
            return new WorkbenchPanel(session, navigation, businessServices, passwordChanged);
        }
        if (module == ModuleId.PROFILE) {
            return new PersonalCenterPage(session, businessServices, passwordChanged);
        }
        return ModulePages.forModule(module, session, aiFactory, businessServices);
    }
}
