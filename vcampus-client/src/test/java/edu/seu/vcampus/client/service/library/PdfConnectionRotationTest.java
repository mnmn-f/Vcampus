package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;

/** 客户端真实网关在 PDF 分块间重建连接，累计传输可超过对象流预算。 */
public final class PdfConnectionRotationTest {
    @Test public void gatewayRotatesForLargeDownloadsAndRetainsSession() throws Exception {
        int count = 70;
        try (ServerSocket listener = new ServerSocket(0)) {
            listener.setSoTimeout(10000);
            CompletableFuture<Void> server = CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < count; i++) try (Socket socket = listener.accept()) {
                        socket.setSoTimeout(10000); ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream()); output.flush();
                        SafeObjectInputStream input = new SafeObjectInputStream(socket.getInputStream()); Message request = (Message) input.readObject();
                        assertEquals("token-test", request.getSessionToken()); output.writeObject(Message.success(request, new byte[PdfCommands.CHUNK_BYTES])); output.flush();
                    }
                } catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
            });
            NetworkClientService network = new NetworkClientService(new SocketClientGateway("127.0.0.1", listener.getLocalPort())); network.setSessionToken("token-test");
            NetworkPdfClientService service = new NetworkPdfClientService(network, null); long received = 0;
            for (int i = 0; i < count; i++) received += service.downloadChunk(new PdfDownloadChunk(1, received, PdfCommands.CHUNK_BYTES)).length;
            assertEquals((long) count * PdfCommands.CHUNK_BYTES, received); server.get(); network.close();
        }
    }

    @Test public void sessionTokenIsReadForEachChunkAfterRelogin() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkClientService network = new NetworkClientService(gateway);
        ClientSession session = new ClientSession();
        session.open(new LoginResult(1L, "a", "甲", Role.STUDENT, "token-a"));
        NetworkPdfClientService service = new NetworkPdfClientService(network, session);
        service.downloadChunk(new PdfDownloadChunk(1, 0, 1));
        session.open(new LoginResult(1L, "a", "甲", Role.STUDENT, "token-b"));
        service.downloadChunk(new PdfDownloadChunk(1, 1, 1));
        org.junit.Assert.assertEquals(java.util.Arrays.asList("token-a", "token-b"), gateway.tokens);
    }

    private static final class RecordingGateway implements ClientGateway {
        final List<String> tokens = new ArrayList<String>();
        @Override public Message send(Message request) {
            tokens.add(request.getSessionToken()); return Message.success(request, new byte[]{1});
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
