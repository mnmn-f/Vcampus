package edu.seu.vcampus.server.ai.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class AiPlainTextFilterTest {
    @Test public void removesMarkdownNoiseAcrossStreamingChunks() {
        StringBuilder answer = new StringBuilder();
        AiPlainTextFilter filter = new AiPlainTextFilter(answer::append);
        filter.onText("**结论**\n□ 可预约 [");
        filter.onText("1]\n\\- 时间：`09:00`");
        filter.finish();
        assertEquals("结论\n 可预约 \n- 时间：09:00", answer.toString());
    }

    @Test public void keepsNonNumericBrackets() {
        StringBuilder answer = new StringBuilder();
        AiPlainTextFilter filter = new AiPlainTextFilter(answer::append);
        filter.onText("课程[必修]与未闭合[2");
        filter.finish();
        assertEquals("课程[必修]与未闭合[2", answer.toString());
    }
}
