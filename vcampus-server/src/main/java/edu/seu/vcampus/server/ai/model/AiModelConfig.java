package edu.seu.vcampus.server.ai.model;

import java.net.URI;

/** DeepSeek Responses API 的服务端配置；密钥只从环境变量读取。 */
public final class AiModelConfig {
    static final String DEFAULT_ENDPOINT = "https://api.deepseek.com/responses";
    static final String DEFAULT_MODEL = "deepseek-v4-flash";
    static final String DEFAULT_VISION_MODEL = "deepseek-v4-flash-vision-exp";
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final String visionModel;
    private final int connectTimeout;
    private final int readTimeout;

    AiModelConfig(String endpoint, String apiKey, String model,
            int connectTimeout, int readTimeout) {
        this(endpoint, apiKey, model, DEFAULT_VISION_MODEL, connectTimeout, readTimeout);
    }

    AiModelConfig(String endpoint, String apiKey, String model, String visionModel,
            int connectTimeout, int readTimeout) {
        this.endpoint = apiEndpoint(endpoint);
        this.apiKey = trim(apiKey);
        this.model = requireText(model, "model");
        this.visionModel = requireText(visionModel, "vision model");
        this.connectTimeout = positive(connectTimeout, "connect timeout");
        this.readTimeout = positive(readTimeout, "read timeout");
    }

    public static AiModelConfig load() {
        return new AiModelConfig(
                config("vcampus.ai.endpoint", "VCAMPUS_AI_ENDPOINT", DEFAULT_ENDPOINT),
                firstPresent(System.getenv("VCAMPUS_AI_API_KEY"), System.getenv("DEEPSEEK_API_KEY")),
                config("vcampus.ai.model", "VCAMPUS_AI_MODEL", DEFAULT_MODEL),
                config("vcampus.ai.vision-model", "VCAMPUS_AI_VISION_MODEL", DEFAULT_VISION_MODEL),
                propertyInt("vcampus.ai.connect-timeout", 10000),
                propertyInt("vcampus.ai.read-timeout", 120000));
    }

    public String getEndpoint() { return endpoint; }
    String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getVisionModel() { return visionModel; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public boolean isConfigured() { return !blank(apiKey); }

    private static String apiEndpoint(String value) {
        try {
            String text = requireText(value, "Responses API endpoint");
            URI uri = new URI(text);
            String host = uri.getHost();
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host) || "::1".equals(host)
                    || "[::1]".equals(host);
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            String path = uri.getPath();
            if ((!secure && !(loopback && "http".equalsIgnoreCase(uri.getScheme())))
                    || blank(host) || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || blank(path)) {
                throw new IllegalArgumentException(
                        "Responses API 地址必须使用 HTTPS（本机测试可使用 loopback HTTP）");
            }
            return text;
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalArgumentException("Responses API 地址格式不正确", ex);
        }
    }

    private static String config(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (blank(value)) value = System.getenv(environment);
        return blank(value) ? fallback : value.trim();
    }

    private static int propertyInt(String key, int fallback) {
        try { return positive(Integer.parseInt(System.getProperty(key, "" + fallback)), key); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static int positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static String requireText(String value, String name) {
        if (blank(value)) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }
    private static String firstPresent(String first, String second) {
        return blank(first) ? second : first;
    }
    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
