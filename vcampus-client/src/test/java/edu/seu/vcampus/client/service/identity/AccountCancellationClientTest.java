package edu.seu.vcampus.client.service.identity;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 注销服务只发送稳定命令和当前会话令牌，不在请求中伪造 userId。 */
public final class AccountCancellationClientTest {
    @Test public void sendsOwnAndAdminCommandsWithCurrentToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "admin", "系统管理员", Role.SYSTEM_ADMIN, "token-7"));
        IdentityClientService service = new NetworkIdentityClientService(
                new NetworkClientService(gateway), session);
        assertEquals(7L, service.submitAccountCancellation(new AccountCancellationRequest("不再使用")).getUserId());
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT, gateway.command);
        assertEquals("token-7", gateway.token);
        service.listOwnAccountCancellations(new AccountCancellationQuery());
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST, gateway.command);
        service.withdrawAccountCancellation(11L);
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_WITHDRAW, gateway.command);
        assertEquals(Long.valueOf(11L), gateway.payload);
        service.searchAccountCancellationRequests(new AccountCancellationQuery("PENDING", 1, 20));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST, gateway.command);
        service.approveAccountCancellation(new AccountCancellationReviewRequest(11L, "核验通过"));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_APPROVE, gateway.command);
        service.rejectAccountCancellation(new AccountCancellationReviewRequest(11L, "资料不完整"));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_REJECT, gateway.command);
    }

    private static final class RecordingGateway implements ClientGateway {
        private String command;
        private String token;
        private Object payload;

        @Override public Message send(Message request) {
            command = request.getCommand(); token = request.getSessionToken(); payload = request.getPayload();
            if (IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST.equals(command)
                    || IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST.equals(command)) {
                return Message.success(request, new AccountCancellationPage(
                        Collections.singletonList(value()), 1, 20, 1L));
            }
            return Message.success(request, value());
        }

        private AccountCancellationDto value() {
            return new AccountCancellationDto(11L, 7L, "admin", "系统管理员", "不再使用",
                    "PENDING", null, null, null, LocalDateTime.now(), LocalDateTime.now());
        }

        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
