package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.service.library.PdfClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** 在原有线上链接管理旁接入 PDF 资源工作区。 */
public final class LibraryOnlinePanel extends JPanel {
    public LibraryOnlinePanel(BasePage page, LibraryClientService service, ClientSession session) {
        super(new BorderLayout()); setOpaque(false);
        JTabbedPane tabs = new edu.seu.vcampus.client.ui.FitContentTabs();
        PdfClientService pdf = service.pdf(session);
        if (pdf != null) tabs.addTab("PDF 文件资源", new PdfResourcesPanel(page, pdf, session.getActiveRole()));
        tabs.addTab("线上资源链接", new LibraryResourcesPanel(page, service, session.getActiveRole()));
        add(tabs, BorderLayout.CENTER);
    }
}
