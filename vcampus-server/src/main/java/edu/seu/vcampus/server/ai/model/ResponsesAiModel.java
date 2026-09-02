package edu.seu.vcampus.server.ai.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/** 调用兼容 Responses API 的 HTTPS 接口并读取 SSE 文本增量。 */
public final class ResponsesAiModel implements AiModel {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final AiModelConfig config;

    public ResponsesAiModel(AiModelConfig config) {
        if (config == null) throw new IllegalArgumentException("model config is required");
        this.config = config;
    }

    public boolean isConfigured() { return config.isConfigured(); }

    public String getModelName() {
        return config.getModel() + (isConfigured() ? "（API 已配置）" : "（API 未配置）");
    }

    public void generate(String requestId, String prompt, AiTextSink sink) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("AI API Key 未配置");
        HttpURLConnection connection = open();
        try {
            byte[] body = body(prompt).getBytes(UTF8);
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream output = connection.getOutputStream();
            try { output.write(body); output.flush(); } finally { output.close(); }
            requireSuccess(connection);
            readStream(connection.getInputStream(), sink);
        } finally { connection.disconnect(); }
    }

    private HttpURLConnection open() throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(config.getEndpoint()).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(config.getConnectTimeout());
        connection.setReadTimeout(config.getReadTimeout());
        connection.setUseCaches(false); connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        return connection;
    }

    private String body(String prompt) {
        String instructions = "你是虚拟校园系统内的校园助手。只根据提供的校园知识作答；"
                + "涉及个人数据或业务操作时不得编造结果，回答简洁并标明依据。";
        return "{\"model\":" + JsonText.quote(config.getModel())
                + ",\"instructions\":" + JsonText.quote(instructions)
                + ",\"input\":" + JsonText.quote(prompt)
                + ",\"stream\":true,\"max_output_tokens\":1200}";
    }

    private void readStream(InputStream input, AiTextSink sink) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF8));
        int chunks = 0; boolean done = false; String line;
        while ((line = reader.readLine()) != null) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("AI request cancelled");
            }
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if ("[DONE]".equals(data)) { done = true; break; }
            String type = JsonText.string(data, "type");
            if ("response.output_text.delta".equals(type)) {
                String delta = JsonText.string(data, "delta");
                if (delta != null && !delta.isEmpty()) { sink.onText(delta); chunks++; }
            } else if ("response.completed".equals(type)) {
                done = true; break;
            } else if ("response.failed".equals(type) || "error".equals(type)) {
                String message = JsonText.string(data, "message");
                throw new IllegalStateException(message == null
                        ? "AI API 返回失败事件" : message);
            }
        }
        if (!done || chunks == 0) throw new IllegalStateException("AI API 没有返回完整文本");
    }

    private void requireSuccess(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalStateException(
                "AI API 返回 HTTP " + status + ": "
                        + readBounded(connection.getErrorStream(), 1000));
    }

    private String readBounded(InputStream input, int limit) throws Exception {
        if (input == null) return "无响应正文";
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF8));
        StringBuilder out = new StringBuilder(); int value;
        while (out.length() < limit && (value = reader.read()) >= 0) out.append((char) value);
        return out.toString();
    }
}
