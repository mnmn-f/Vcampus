package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.ext.registry.DormExtCommandRegistry;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 宿舍扩展命令注册与服务状态入口。 */
public final class DormExtRoutingTest {
    private DormExtService service;
    private SessionContext manager;

    @Before public void setUp() {
        service = new DormExtService(new InMemoryDormExtRepository());
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }

    @Test public void registryRegistersEveryExtensionCommand() {
        Set<String> commands = declaredCommands();
        assertTrue(commands.size() >= 27);
        CommandRouter router = new CommandRouter(new SessionManager());
        DormExtCommandRegistry.registerAll(router, service);
        assertEquals(commands.size(), router.registeredCommandCount());
    }
    @Test public void statusReportsStoppedModule() {
        DormExtStatusDto status = service.status(manager);
        assertEquals(DormExtService.MODULE_VERSION, status.getModuleVersion());
        assertFalse(status.isSchedulerRunning()); assertNotNull(status.getServerTime());
    }
    @Test public void unknownCommandIsRejectedByRouter() {
        CommandRouter router = new CommandRouter(new SessionManager());
        DormExtCommandRegistry.registerAll(router, service);
        assertNotNull(router.route(Message.request("dorm.ext.not.exist", null, null)));
    }

    private static Set<String> declaredCommands() {
        Set<String> values = new HashSet<String>();
        for (Field f : DormExtCommands.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                try {
                    Object value = f.get(null);
                    if (value instanceof String && ((String) value).startsWith("dorm.ext.")) {
                        values.add((String) value);
                    }
                } catch (IllegalAccessException ex) { throw new AssertionError(ex); }
            }
        }
        return values;
    }
}
