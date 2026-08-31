package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.modules.real.RealAcademicPage;
import edu.seu.vcampus.client.view.modules.real.RealDormPage;
import edu.seu.vcampus.client.view.modules.real.RealIdentityAdminPage;
import edu.seu.vcampus.client.view.modules.real.RealLibraryPage;
import edu.seu.vcampus.client.view.modules.real.RealStorePage;
import edu.seu.vcampus.client.view.modules.real.RealStudentRecordPage;
import edu.seu.vcampus.common.module.ModuleId;

/** 模块页面工厂：网络模式使用真实页面，未接入服务时使用统一占位页。 */
public final class ModulePages {
    private ModulePages() {
    }

    public static BasePage forModule(ModuleId id, ClientSession session,
                                     AiAssistantPage.Factory aiFactory) {
        return forModule(id, session, aiFactory, null);
    }

    public static BasePage forModule(ModuleId id, ClientSession session,
                                     AiAssistantPage.Factory aiFactory,
                                     ClientBusinessServices businessServices) {
        if (businessServices == null || id == null) {
            return new ServiceRequiredPage(session, id);
        }
        switch (id) {
            case STUDENT_RECORD:
                return new RealStudentRecordPage(session, businessServices);
            case ACADEMIC:
                return new RealAcademicPage(session, businessServices);
            case LIBRARY:
                return new RealLibraryPage(session, businessServices);
            case STORE:
                return new RealStorePage(session, businessServices);
            case DORMITORY:
                return new RealDormPage(session, businessServices);
            case USER_ADMIN:
                return new RealIdentityAdminPage(session, businessServices.identity(), false);
            case SYSTEM:
                return new RealIdentityAdminPage(session, businessServices.identity(), true);
            case AI_ASSISTANT:
                return AiAssistantPage.create(session, aiFactory);
            default:
                return new ServiceRequiredPage(session, id);
        }
    }
}
