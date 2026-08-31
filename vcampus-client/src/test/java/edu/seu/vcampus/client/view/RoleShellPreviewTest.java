package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** 生成九类校园人员的完整应用壳层预览。 */
public final class RoleShellPreviewTest {
    @Test public void renderEveryRoleShellAtDeliverySize() throws Exception {
        for (Role role : Role.values()) render(role);
    }

    private static void render(final Role role) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            ClientSession session = new ClientSession();
            session.open(new LoginResult(1000L + role.ordinal(), account(role), displayName(role),
                    role, "preview-token"));
            AppShell shell = new AppShell(new PreviewAuth(), session, new AppShell.Listener() {
                @Override public void onLogout() { }
            }, null, null);
            shell.setSize(1280, 820); shell.validate(); layout(shell);
            BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); shell.printAll(graphics); graphics.dispose();
            try {
                File dir = new File("target/ui-previews/roles"); dir.mkdirs();
                ImageIO.write(image, "png", new File(dir, "shell-" + account(role) + "-1280x820.png"));
            } catch (Exception ex) { throw new PreviewFailure(ex); }
            }
        });
    }

    private static String account(Role role) { return role.name().toLowerCase(java.util.Locale.ROOT); }

    private static String displayName(Role role) {
        switch (role) {
            case STUDENT: return "林同学";
            case TEACHER: return "周老师";
            case REGISTRAR: return "学籍管理员";
            case ACADEMIC_ADMIN: return "教务老师";
            case LIBRARIAN: return "图书管理员";
            case STORE_MANAGER: return "商店管理员";
            case DORM_MANAGER: return "宿管老师";
            case AI_KNOWLEDGE_ADMIN: return "知识管理员";
            default: return "系统管理员";
        }
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }

    private static final class PreviewAuth implements AuthClientService {
        @Override public LoginResult login(String account, String password) { return null; }
        @Override public void logout() { }
        @Override public Role switchRole(Role role) { return role; }
        @Override public boolean isLoggedIn() { return true; }
    }

    private static final class PreviewFailure extends RuntimeException {
        PreviewFailure(Exception cause) { super(cause); }
    }
}
