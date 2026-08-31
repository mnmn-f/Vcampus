package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 图书馆模块的实时网络页面。 */
public final class RealLibraryPage extends BasePage {
    public RealLibraryPage(ClientSession session, ClientBusinessServices services) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.LIBRARY), "");
        Role role = session.getActiveRole(); setHeaderContext(role.getDisplayName());
        TaskTabs tabs = new TaskTabs();
        tabs.addTask(role == Role.LIBRARIAN ? "馆藏管理" : "图书检索",
                new LibraryBooksPanel(this, services.library(), role));
        if (role == Role.STUDENT) tabs.addTask("我的借阅", new LibraryBorrowingsPanel(this, services.library()));
        if (showsBorrowingLedger(role)) tabs.addTask("借阅台账", new LibraryBorrowingLedgerPanel(this, services.library()));
        tabs.addTask("自习空间", new LibraryRoomsPanel(this, services.library(), role));
        tabs.addTask("线上资源", new LibraryResourcesPanel(this, services.library(), role));
        tabs.addTask("图书馆公告", new CampusAnnouncementsPanel(this, services.campus(), role,
                "LIBRARY", "图书馆公告", Role.LIBRARIAN));
        addBlock(tabs);
    }

    static boolean showsBorrowingLedger(Role role) { return role == Role.LIBRARIAN; }
}
