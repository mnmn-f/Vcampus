package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 宿舍扩展测试共用的会话与异常断言。 */
final class DormExtTestSupport {
    interface Action { void run(); }

    private DormExtTestSupport() { }

    static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }

    static void assertCode(String code, Action action) {
        try {
            action.run();
            fail("expected result code: " + code);
        } catch (DormException ex) {
            assertEquals(code, ex.getResultCode());
        }
    }
}
