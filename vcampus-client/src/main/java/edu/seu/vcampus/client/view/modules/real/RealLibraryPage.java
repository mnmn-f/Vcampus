package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.service.library.DemoLibraryCampusClientService;
import edu.seu.vcampus.client.service.library.DemoLibraryClientService;
import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.RoleWorkspace;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

/** 图书馆模块页面；网络模式与 Demo 模式复用同一套界面和主题。 */
public final class RealLibraryPage extends BasePage {
    public RealLibraryPage(ClientSession session, ClientBusinessServices services) {
        this(session, services.library(), services.campus());
    }

    public static RealLibraryPage demo(ClientSession session) {
        return new RealLibraryPage(session, new DemoLibraryClientService(),
                new DemoLibraryCampusClientService(session.getActiveRole()));
    }

    private RealLibraryPage(ClientSession session, LibraryClientService library,
                            CampusClientService campus) {
        super(session, RoleWorkspace.navigationLabel(session.getActiveRole(), ModuleId.LIBRARY),
                "图书查询与借阅、自习室预约、公告和线上资源服务。");
        Role role = session.getActiveRole(); setHeaderContext(role.getDisplayName());
        final LibraryTaskTabs tabs = new LibraryTaskTabs(role == Role.LIBRARIAN);
        tabs.setDisplayName(session.getDisplayName());
        if (role == Role.LIBRARIAN) {
            tabs.addTask("公告管理", LineIcon.Kind.SYSTEM,
                    new CampusAnnouncementsPanel(this, campus, role,
                            "LIBRARY", "公告管理", Role.LIBRARIAN));
            tabs.addTask("图书管理", LineIcon.Kind.LIBRARY,
                    new LibraryBooksPanel(this, library, role));
            tabs.addTask("借阅管理", LineIcon.Kind.ACADEMIC,
                    new LibraryBorrowingLedgerPanel(this, library));
            tabs.addTask("自习室管理", LineIcon.Kind.PROFILE,
                    new LibraryRoomsPanel(this, library, role));
            tabs.addTask("线上资源管理", LineIcon.Kind.SYSTEM,
                    new LibraryOnlinePanel(this, library, session));
        } else if (role == Role.STUDENT) {
            final LibraryCatalogPanel catalog = new LibraryCatalogPanel(this, library, role);
            final LibraryRoomBookingPanel rooms = new LibraryRoomBookingPanel(this, library);
            final StudentLibraryOnlinePanel online = new StudentLibraryOnlinePanel(this, library, session);
            LibraryHomePanel home = new LibraryHomePanel(session, library, campus,
                    value -> { catalog.searchFor(value); tabs.selectTask(1); },
                    () -> { catalog.showBorrowings(); tabs.selectTask(1); },
                    () -> tabs.selectTask(2));
            tabs.addTask("首页", LineIcon.Kind.DASHBOARD, home);
            tabs.addTask("图书查阅", LineIcon.Kind.LIBRARY, catalog);
            tabs.addTask("自习室预约", LineIcon.Kind.ACADEMIC, rooms);
            tabs.addTask("线上资源", LineIcon.Kind.STUDENT_RECORD, online);
        } else {
            tabs.addTask("首页", LineIcon.Kind.DASHBOARD,
                    new CampusAnnouncementsPanel(this, campus, role,
                            "LIBRARY", "公告栏", Role.LIBRARIAN));
            {
                tabs.addTask("图书查阅", LineIcon.Kind.LIBRARY,
                        new LibraryBooksPanel(this, library, role));
            }
            tabs.addTask("自习室预约", LineIcon.Kind.ACADEMIC,
                    new LibraryRoomsPanel(this, library, role));
            tabs.addTask("线上资源", LineIcon.Kind.STUDENT_RECORD,
                    new LibraryOnlinePanel(this, library, session));
        }
        removeAll();
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
        add(feedback, java.awt.BorderLayout.NORTH);
        add(tabs, java.awt.BorderLayout.CENTER);
    }

    static boolean showsBorrowingLedger(Role role) { return role == Role.LIBRARIAN; }
}
