package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import edu.seu.vcampus.common.protocol.io.SafeObjectInputStream;
import org.junit.Test;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;
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
}
