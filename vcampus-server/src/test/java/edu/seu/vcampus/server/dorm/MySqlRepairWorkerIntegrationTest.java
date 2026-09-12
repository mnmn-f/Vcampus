package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** 可选真实 MySQL 验证：V7 维修员账号能登录并读取自己的工单。 */
public final class MySqlRepairWorkerIntegrationTest {
    @Test
    public void repairWorkerLoginAndAssignedOrdersUseTheProductionRoute() {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        String password = System.getProperty("vcampus.repair.password");
        Assume.assumeTrue(password != null && !password.trim().isEmpty());
        JdbcConnectionFactory factory = new JdbcConnectionFactory();
        SessionManager sessions = new SessionManager();
        CommandRouter router = ServerMain.createProductionRouter(new MySqlUserRepository(factory),
                new PasswordHasher(4), sessions, factory);
        Message login = router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("demo_repair", password)));
        assertTrue(login.isSuccess());
        LoginResult result = (LoginResult) login.getPayload();
        Message assigned = router.route(Message.request(DormExtCommands.REPAIR_ASSIGNED,
                result.getSessionToken(), new DormPageQuery(1, 20, null, null, null, null)));
        assertTrue(assigned.isSuccess());
        DormPage<?> page = (DormPage<?>) assigned.getPayload();
        assertTrue(page.getTotal() > 0L);
        assertTrue(page.getItems().get(0) instanceof RepairWorkOrderDto);
    }
}
