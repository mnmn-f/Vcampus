package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.modules.real.IdentityCancellationPanel;
import edu.seu.vcampus.client.view.modules.real.IdentityProfilePanel;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;

/** 与业务首页分离的个人资料和账号设置页面。 */
public final class PersonalCenterPage extends BasePage {
    public PersonalCenterPage(ClientSession session, ClientBusinessServices services,
                              Runnable passwordChanged) {
        super(session, "个人中心", "");
        setHeaderContext(session.getActiveRole().getDisplayName());
        if (services == null) addBlock(summary(session));
        else {
            final IdentityProfilePanel profile = new IdentityProfilePanel(this, session,
                    services.identity(), passwordChanged);
            addBlock(profile);
            final IdentityCancellationPanel cancellation = session.getActiveRole() == Role.SYSTEM_ADMIN
                    ? null : new IdentityCancellationPanel(this, services.identity(), false);
            if (cancellation != null) cancellation.setVisible(false);
            addBlock(accountLinks(profile, cancellation));
            if (session.getActiveRole() != Role.SYSTEM_ADMIN) {
                addBlock(cancellation);
            }
        }
    }

    private JPanel accountLinks(final IdentityProfilePanel profile,
                                final IdentityCancellationPanel cancellation) {
        JPanel links = UiFactory.horizontal(12);
        JButton password = UiFactory.linkButton("修改密码 ›");
        password.addActionListener(e -> {
            if (cancellation != null) cancellation.setVisible(false);
            profile.showPasswordSettings();
        });
        links.add(password);
        if (cancellation != null) {
            JButton cancel = UiFactory.linkButton("注销账号 ›");
            cancel.addActionListener(e -> {
                profile.hidePasswordSettings(); cancellation.setVisible(true);
                cancellation.revalidate(); cancellation.repaint();
            });
            links.add(cancel);
        }
        return links;
    }

    private SectionCard summary(ClientSession current) {
        SectionCard card = new SectionCard("账号信息", "");
        JPanel fields = new JPanel(new GridLayout(0, 2, 18, 12));
        fields.setOpaque(false);
        fields.add(label("账号")); fields.add(value(current.getLoginResult().getAccount()));
        fields.add(label("姓名")); fields.add(value(current.getDisplayName()));
        fields.add(label("身份")); fields.add(value(current.getActiveRole().getDisplayName()));
        fields.add(label("账号状态")); fields.add(value("正常"));
        card.setContent(fields);
        return card;
    }

    private JLabel label(String text) { return UiFactory.muted(text); }
    private JLabel value(String text) { return UiFactory.body(text); }
}
