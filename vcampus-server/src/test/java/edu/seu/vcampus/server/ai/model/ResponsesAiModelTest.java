package edu.seu.vcampus.server.ai.model;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 用最小本地 HTTP 服务器验证 Responses API 鉴权、请求体和 SSE 增量。 */
public final class ResponsesAiModelTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Test public void streamsResponsesApiWithBearerAuthorization() throws Exception {
        String events = "event: response.output_text.delta\n"
                + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"你好\"}\n\n"
                + "event: response.output_text.delta\n"
                + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"校园\"}\n\n"
                + "event: response.completed\n"
                + "data: {\"type\":\"response.completed\"}\n\n";
        FakeResponsesApi fake = new FakeResponsesApi(events);
        try {
            ResponsesAiModel model = model(fake.port(), "test-secret");
            assertTrue(model.isConfigured());
            final StringBuilder answer = new StringBuilder();
            model.generate("request-1", "介绍校园", new AiTextSink() {
                public void onText(String text) { answer.append(text); }
            });
            fake.await();
            assertEquals("你好校园", answer.toString());
            String request = fake.request();
            assertTrue(request.contains("POST /v1/responses"));
            assertTrue(request.contains("Authorization: Bearer test-secret"));
            assertTrue(request.contains("Accept: text/event-stream"));
            assertTrue(request.contains("\"model\":\"test-model\""));
            assertTrue(request.contains("\"stream\":true"));
        } finally { fake.close(); }
    }

    @Test public void missingKeyKeepsKnowledgeFallbackAvailable() throws Exception {
        ResponsesAiModel model = new ResponsesAiModel(new AiModelConfig(
                "https://api.example.com/v1/responses", "", "test-model", 1000, 1000));
        assertFalse(model.isConfigured());
        assertTrue(model.getModelName().contains("未配置"));
    }

    @Test public void rejectsOversizedStreamingOutput() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 33000; i++) text.append('x');
        String events = "data: {\"type\":\"response.output_text.delta\",\"delta\":\""
                + text + "\"}\n\n";
        FakeResponsesApi fake = new FakeResponsesApi(events);
        try {
            try {
                model(fake.port(), "test-secret").generate("request-large", "test",
                        value -> { });
                fail("oversized model output must be rejected");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("安全上限"));
            }
            fake.await();
        } finally { fake.close(); }
    }

    private ResponsesAiModel model(int port, String key) {
        return new ResponsesAiModel(new AiModelConfig(
                "http://127.0.0.1:" + port + "/v1/responses",
                key, "test-model", 2000, 2000));
    }

    private static final class FakeResponsesApi implements Runnable {
        private final ServerSocket server;
        private final String response;
        private final Thread thread;
        private volatile String request;
        private volatile Exception failure;

        private FakeResponsesApi(String response) throws Exception {
            this.response = response;
            server = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
            thread = new Thread(this, "fake-responses-api");
            thread.setDaemon(true); thread.start();
        }

        public void run() {
            try { serve(server.accept()); }
            catch (Exception ex) { if (!server.isClosed()) failure = ex; }
        }

        private void serve(Socket socket) throws Exception {
            try {
                InputStream input = socket.getInputStream();
                ByteArrayOutputStream head = new ByteArrayOutputStream();
                int state = 0; int value;
                int[] separator = new int[] {13, 10, 13, 10};
                while (state < separator.length && (value = input.read()) >= 0) {
                    head.write(value);
                    state = value == separator[state] ? state + 1 : (value == 13 ? 1 : 0);
                }
                String headers = new String(head.toByteArray(), Charset.forName("ISO-8859-1"));
                int length = contentLength(headers); byte[] body = new byte[length]; int read = 0;
                while (read < length) {
                    int count = input.read(body, read, length - read);
                    if (count < 0) break;
                    read += count;
                }
                request = headers + new String(body, 0, read, UTF8);
                byte[] responseBody = response.getBytes(UTF8);
                OutputStream output = socket.getOutputStream();
                String responseHeaders = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/event-stream\r\nContent-Length: "
                        + responseBody.length + "\r\nConnection: close\r\n\r\n";
                output.write(responseHeaders.getBytes(Charset.forName("ISO-8859-1")));
                output.write(responseBody); output.flush();
            } finally { socket.close(); }
        }

        private int contentLength(String headers) {
            String lower = headers.toLowerCase(); int at = lower.indexOf("content-length:");
            if (at < 0) return 0;
            int start = at + "content-length:".length();
            int end = lower.indexOf("\r\n", start);
            return Integer.parseInt(lower.substring(start, end).trim());
        }

        private int port() { return server.getLocalPort(); }
        private String request() { return request; }
        private void await() throws Exception {
            thread.join(3000L);
            if (thread.isAlive()) throw new AssertionError("fake API did not finish");
            if (failure != null) throw failure;
        }
        private void close() throws Exception { server.close(); thread.join(1000L); }
    }
}
