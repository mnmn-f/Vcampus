package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 线上资源客户端只发送资源编号和当前会话令牌。 */
public final class NetworkLibraryResourceAccessTest {
    @Test public void accessSendsOnlyResourceIdAndToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        NetworkLibraryClientService service = new NetworkLibraryClientService(
                new NetworkClientService(gateway), session);
        gateway.payload = resource();

        assertEquals("资源", service.accessResource(12L).getTitle());
        assertEquals(LibraryCommands.RESOURCE_ACCESS, gateway.request.getCommand());
        assertEquals("token-7", gateway.request.getSessionToken());
        assertTrue(gateway.request.getPayload() instanceof OnlineResourceAccessRequest);
        OnlineResourceAccessRequest request = (OnlineResourceAccessRequest) gateway.request.getPayload();
        assertEquals(12L, request.getResourceId());
        assertFalse(hasMethod(OnlineResourceAccessRequest.class, "getUserId"));
        assertFalse(hasMethod(OnlineResourceAccessRequest.class, "getClientIp"));
    }

    @Test public void accessLogSearchMapsStrongPageAndQuery() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(20L, "librarian", "图书管理员", Role.LIBRARIAN, "token-20"));
        NetworkLibraryClientService service = new NetworkLibraryClientService(
                new NetworkClientService(gateway), session);
        OnlineResourceAccessLogQuery query = new OnlineResourceAccessLogQuery(
                Long.valueOf(12L), Long.valueOf(7L), LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59), 2, 10);
        gateway.payload = new OnlineResourceAccessLogPage(Collections.singletonList(
                new OnlineResourceAccessLogDto(1L, 12L, 7L, "资源", "student", "学生",
                        LocalDateTime.of(2026, 8, 29, 10, 0))), 2, 10, 11L);

        assertEquals(11L, service.searchResourceAccessLogs(query).getTotal());
        assertEquals(LibraryCommands.RESOURCE_ACCESS_LOGS, gateway.request.getCommand());
        assertEquals(query, gateway.request.getPayload());
        assertEquals("token-20", gateway.request.getSessionToken());
    }

    private static OnlineResourceView resource() {
        return new OnlineResourceView(12L, "资源", "DOCUMENTATION",
                "https://example.com/resource", null, 20L, "ACTIVE", null);
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            if (name.equals(method.getName())) return true;
        }
        return false;
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        private java.io.Serializable payload;
        @Override public Message send(Message value) {
            request = value; return Message.success(value, payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
