package edu.seu.vcampus.server.ai.model;

import edu.seu.vcampus.common.ai.AiAttachment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** 调用兼容 Responses API 的 HTTPS 接口并读取 SSE 文本增量。 */
public final class ResponsesAiModel implements AiModel {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_OUTPUT_CHARS = 32000;
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
        generate(requestId, prompt, Collections.<AiAttachment>emptyList(), sink);
    }

    public void generate(String requestId, String prompt, List<AiAttachment> attachments,
                         AiTextSink sink) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("AI API Key 未配置");
        HttpURLConnection connection = open();
        try {
            byte[] body = body(prompt, attachments).getBytes(UTF8);
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

    private String body(String prompt, List<AiAttachment> attachments) {
        String instructions = "你是虚拟校园系统内的 AI 助手。严格遵循输入中声明的模式和任务；"
                + "涉及个人数据或业务操作时不得编造结果，也不得声称执行未由系统工具完成的操作。"
                + "回答必须是适合桌面聊天框显示的纯文本，不使用 Markdown 标题、星号、方框、"
                + "反引号或方括号数字引用；需要分步时使用“第一步、第二步”这样的中文表达。";
        boolean hasImage = false;
        StringBuilder content = new StringBuilder("[");
        content.append("{\"type\":\"input_text\",\"text\":")
                .append(JsonText.quote(prompt)).append('}');
        if (attachments != null) for (AiAttachment attachment : attachments) {
            if (attachment == null) continue;
            if (attachment.isImage()) {
                hasImage = true;
                String data = "data:" + attachment.getMediaType() + ";base64,"
                        + Base64.getEncoder().encodeToString(attachment.getContent());
                content.append(",{\"type\":\"input_text\",\"text\":")
                        .append(JsonText.quote("图片附件：" + attachment.getFileName()
                                + "。请观察实际画面，不要根据文件名猜测。"))
                        .append('}')
                        .append(",{\"type\":\"input_image\",\"image_url\":")
                        .append(JsonText.quote(data)).append('}');
            } else if (attachment.isText()) {
                String text = new String(attachment.getContent(), UTF8);
                if (text.length() > 60000) text = text.substring(0, 60000) + "\n文件内容已截断";
                content.append(",{\"type\":\"input_text\",\"text\":")
                        .append(JsonText.quote("附件 " + attachment.getFileName() + ":\n" + text))
                        .append('}');
            }
        }
        content.append(']');
        return "{\"model\":" + JsonText.quote(hasImage ? config.getVisionModel() : config.getModel())
                + ",\"instructions\":" + JsonText.quote(instructions)
                + ",\"input\":[{\"role\":\"user\",\"content\":" + content + "}]"
                + ",\"stream\":true,\"max_output_tokens\":1200}";
    }

    private void readStream(InputStream input, AiTextSink sink) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF8));
        int chunks = 0; int outputChars = 0; boolean done = false; String line;
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
                if (delta != null && !delta.isEmpty()) {
                    outputChars += delta.length();
                    if (outputChars > MAX_OUTPUT_CHARS) {
                        throw new IllegalStateException("AI API 输出超过安全上限");
                    }
                    sink.onText(delta); chunks++;
                }
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
