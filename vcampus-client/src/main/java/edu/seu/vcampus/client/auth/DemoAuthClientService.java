package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用于界面预览和前端联调的本地认证实现。
 *
 * <p>它只模拟认证，不伪造业务数据。接入服务器时替换为
 * {@link NetworkAuthClientService} 即可。</p>
 */
public final class DemoAuthClientService implements AuthClientService {
    private final Map<String, DemoAccount> accounts;
    private LoginResult current;

    public DemoAuthClientService() {
        this.accounts = createAccounts();
    }

    @Override
    public LoginResult login(String account, String password) throws ClientServiceException {
        String normalizedAccount = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        if (normalizedAccount.length() == 0 || password == null || password.length() == 0) {
            throw new ClientServiceException(ResultCodes.INVALID_INPUT, "请输入账号和密码");
        }
        DemoAccount demo = accounts.get(normalizedAccount);
        if (demo == null || !demo.password.equals(password)) {
            throw new ClientServiceException(ResultCodes.INVALID_CREDENTIALS,
                    "账号或密码不正确，请检查后重试");
        }
        current = new LoginResult(demo.userId, normalizedAccount, demo.displayName,
                demo.roles, demo.roles.iterator().next(),
                "demo-" + UUID.randomUUID().toString());
        return current;
    }

    @Override
    public void logout() {
        current = null;
    }

    @Override
    public Role switchRole(Role role) throws ClientServiceException {
        if (current == null || role == null || !current.getRoles().contains(role)) {
            throw new ClientServiceException(ResultCodes.FORBIDDEN, "当前账号未被授予该职责");
        }
        current = new LoginResult(current.getUserId(), current.getAccount(),
                current.getDisplayName(), current.getRoles(), role,
                current.getSessionToken());
        return role;
    }

    @Override
    public boolean isLoggedIn() {
        return current != null;
    }

    private Map<String, DemoAccount> createAccounts() {
        Map<String, DemoAccount> result = new LinkedHashMap<String, DemoAccount>();
        add(result, 1L, "demo_student", "student123", "演示学生", Role.STUDENT);
        add(result, 2L, "demo_teacher", "teacher123", "演示教师兼教务员",
                EnumSet.of(Role.TEACHER, Role.ACADEMIC_ADMIN));
        add(result, 3L, "demo_registrar", "registrar123", "演示学籍管理员", Role.REGISTRAR);
        add(result, 4L, "demo_academic", "academic123", "演示教务老师", Role.ACADEMIC_ADMIN);
        add(result, 5L, "demo_librarian", "library123", "演示图书管理员", Role.LIBRARIAN);
        add(result, 6L, "demo_store", "store123", "演示商店管理员", Role.STORE_MANAGER);
        add(result, 7L, "demo_dorm", "dorm123", "演示宿管员", Role.DORM_MANAGER);
        add(result, 8L, "demo_ai", "ai123", "演示AI知识管理员", Role.AI_KNOWLEDGE_ADMIN);
        add(result, 9L, "demo_system", "system123", "演示系统管理员", Role.SYSTEM_ADMIN);
        return result;
    }

    private void add(Map<String, DemoAccount> result, long id, String account,
                     String password, String displayName, Role role) {
        add(result, id, account, password, displayName, EnumSet.of(role));
    }

    private void add(Map<String, DemoAccount> result, long id, String account,
                     String password, String displayName, Set<Role> roles) {
        result.put(account, new DemoAccount(id, password, displayName, roles));
    }

    private static final class DemoAccount {
        private final long userId;
        private final String password;
        private final String displayName;
        private final Set<Role> roles;

        private DemoAccount(long userId, String password, String displayName,
                            Set<Role> roles) {
            this.userId = userId;
            this.password = password;
            this.displayName = displayName;
            this.roles = Collections.unmodifiableSet(EnumSet.copyOf(roles));
        }
    }
}
