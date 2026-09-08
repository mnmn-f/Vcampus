package edu.seu.vcampus.client.service.library;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 统一的 UTF-8 BOM CSV 编码器；单元测试可直接验证转义结果。 */
public final class CsvEncoder {
    private CsvEncoder() {
    }

    public static byte[] encode(List<String[]> rows) {
        StringBuilder text = new StringBuilder();
        text.append('\uFEFF');
        if (rows != null) {
            for (String[] row : rows) {
                if (row == null) continue;
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) text.append(',');
                    text.append(escape(row[i]));
                }
                text.append("\r\n");
            }
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String escape(String value) {
        String text = value == null ? "" : value;
        boolean quoted = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0;
        if (!quoted) return text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
