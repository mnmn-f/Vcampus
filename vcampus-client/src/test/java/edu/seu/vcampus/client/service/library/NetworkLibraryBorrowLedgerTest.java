package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.BorrowAdminSearchRequest;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 管理员借阅台账客户端发送独立查询 DTO 和当前会话令牌。 */
public final class NetworkLibraryBorrowLedgerTest {
    @Test public void sendsAdminLedgerCommandAndQuery() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(9L, "librarian", "图书管理员", Role.LIBRARIAN, "token-9"));
        NetworkLibraryClientService service = new NetworkLibraryClientService(
                new NetworkClientService(gateway), session);
        BorrowAdminSearchRequest query = new BorrowAdminSearchRequest("BORROWED", 7L,
                "Java", 2, 50);
        gateway.payload = new PageResult<BorrowRecordView>(Collections.singletonList(row()), 2, 50, 51L);

        assertEquals(51L, service.adminBorrowings(query).getTotal());
        assertEquals(LibraryCommands.BORROW_ADMIN_LIST, gateway.request.getCommand());
        assertEquals("token-9", gateway.request.getSessionToken());
        assertEquals(query, gateway.request.getPayload());
        assertTrue(gateway.request.getPayload() instanceof BorrowAdminSearchRequest);
    }

    private static BorrowRecordView row() {
        LocalDateTime now = LocalDateTime.now();
        return new BorrowRecordView(1L, 2L, "Java", 7L, "学生", now, now.plusDays(20),
                null, "BORROWED", 0, null);
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
