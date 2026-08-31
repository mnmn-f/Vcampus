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

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 注销申请客户端只负责令牌、命令和 Common DTO 映射。 */
public final class AccountCancellationClientServiceTest {
    @Test public void cancellationMethodsUseStableCommandsAndCurrentToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        ClientSession session = new ClientSession();
        session.open(new LoginResult(9L, "student", "学生", Role.STUDENT, "token-9"));
        NetworkIdentityClientService service = new NetworkIdentityClientService(
                new NetworkClientService(gateway), session);
        AccountCancellationDto dto = new AccountCancellationDto(7L, 9L, "student", "学生",
                "原因", "PENDING", null, null, null, null, null);
        gateway.payload = dto;
        assertEquals(dto, service.submitAccountCancellation(new AccountCancellationRequest("原因")));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT, gateway.lastRequest.getCommand());
        assertEquals("token-9", gateway.lastRequest.getSessionToken());
        assertEquals(dto, service.withdrawAccountCancellation(7L));
        assertEquals(Long.valueOf(7L), gateway.lastRequest.getPayload());
        gateway.payload = new AccountCancellationPage(Collections.singletonList(dto), 1, 20, 1L);
        assertEquals(1L, service.listOwnAccountCancellations(new AccountCancellationQuery()).getTotal());
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST, gateway.lastRequest.getCommand());
        gateway.payload = dto;
        service.approveAccountCancellation(new AccountCancellationReviewRequest(7L, "确认"));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_APPROVE, gateway.lastRequest.getCommand());
        service.rejectAccountCancellation(new AccountCancellationReviewRequest(7L, "驳回"));
        assertEquals(IdentityCommands.ACCOUNT_CANCELLATION_REJECT, gateway.lastRequest.getCommand());
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message lastRequest;
        private Object payload;

        @Override public Message send(Message request) {
            lastRequest = request;
            return Message.success(request, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
