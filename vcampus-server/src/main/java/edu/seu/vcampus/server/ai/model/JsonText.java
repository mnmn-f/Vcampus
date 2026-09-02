package edu.seu.vcampus.server.ai.model;

/** AI HTTP 适配器所需的最小 JSON 编解码，避免引入不兼容 Java 7 的 SDK。 */
final class JsonText {
    private JsonText() { }

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 0x20) out.append(String.format("\\u%04x", Integer.valueOf(c)));
            else out.append(c);
        }
        return out.append('"').toString();
    }

    static String string(String json, String key) {
        if (json == null) return null;
        int at = json.indexOf('"' + key + '"');
        if (at < 0) return null;
        at = json.indexOf(':', at);
        if (at < 0) return null;
        at++;
        while (at < json.length() && Character.isWhitespace(json.charAt(at))) at++;
        if (at >= json.length() || json.charAt(at) != '"') return null;
        StringBuilder out = new StringBuilder();
        for (int i = at + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (++i >= json.length()) break;
            char e = json.charAt(i);
            if (e == 'n') out.append('\n'); else if (e == 'r') out.append('\r');
            else if (e == 't') out.append('\t'); else if (e == 'b') out.append('\b');
            else if (e == 'f') out.append('\f');
            else if (e == 'u' && i + 4 < json.length()) {
                try { out.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); i += 4; }
                catch (NumberFormatException ex) { out.append('?'); }
            } else out.append(e);
        }
        return null;
    }

    static boolean bool(String json, String key) {
        if (json == null) return false;
        int at = json.indexOf('"' + key + '"');
        if (at < 0 || (at = json.indexOf(':', at)) < 0) return false;
        at++;
        while (at < json.length() && Character.isWhitespace(json.charAt(at))) at++;
        return json.startsWith("true", at);
    }
}
