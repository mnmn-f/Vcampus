package edu.seu.vcampus.server;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 确认生产装配包含业务命令，且仍由统一路由器先完成会话鉴权。 */
public class ServerMainProductionRouterTest {
    @Test
    public void productionRouterRegistersCompletedBusinessModules() {
        CommandRouter router = ServerMain.createProductionRouter(
                new InMemoryUserRepository(), new PasswordHasher(4),
                new SessionManager(), new JdbcConnectionFactory());

        assertTrue(router.registeredCommandCount() >= 105);
        assertUnauthorized(router, StudentCommands.SELF_PROFILE);
        assertUnauthorized(router, AcademicCommands.COURSE_LIST);
        assertUnauthorized(router, LibraryCommands.BOOK_SEARCH);
        assertUnauthorized(router, DormCommands.MY_ACCOMMODATION);
        assertUnauthorized(router, DormExtCommands.STAY_MINE);
        assertUnauthorized(router, StoreCommands.PRODUCT_SEARCH);
        assertUnauthorized(router, CampusCommands.COMPETITION_LIST);
        assertUnauthorized(router, IdentityCommands.SYSTEM_MONITOR);
    }

    private static void assertUnauthorized(CommandRouter router, String command) {
        Message response = router.route(Message.request(command, null, null));
        assertEquals(ResultCodes.UNAUTHORIZED, response.getResultCode());
    }
}
