package edu.seu.vcampus.client.service.library;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/** CSV 导出保证 Excel 可识别的 UTF-8 BOM 和 RFC 风格字段转义。 */
public final class CsvEncoderTest {
    @Test public void writesBomAndEscapesCommaQuoteAndNewline() {
        byte[] bytes = CsvEncoder.encode(Arrays.asList(
                new String[]{"学生", "备注"}, new String[]{"张三,学生", "他说\"已还\"\n请确认"}));
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                Arrays.copyOf(bytes, 3));
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("\uFEFF学生,备注\r\n"));
        assertTrue(text.contains("\"张三,学生\""));
        assertTrue(text.contains("\"他说\"\"已还\"\"\n请确认\""));
    }

    @Test public void emptyInputStillContainsOnlyBom() {
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                CsvEncoder.encode(Collections.<String[]>emptyList()));
    }
}
