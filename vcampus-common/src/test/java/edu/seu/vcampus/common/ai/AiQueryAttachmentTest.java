package edu.seu.vcampus.common.ai;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 附件列表和字节内容不能被调用方在构造后篡改。 */
public final class AiQueryAttachmentTest {
    @Test public void keepsDefensiveAttachmentCopies() {
        byte[] bytes = new byte[] {1, 2, 3};
        AiAttachment attachment = new AiAttachment("photo.png", "image/png", bytes);
        AiQuery query = new AiQuery("r", "s", "看看图片", AiMode.CHAT,
                Arrays.asList(attachment));
        bytes[0] = 9;
        byte[] returned = query.getAttachments().get(0).getContent();
        assertEquals(1, returned[0]);
        returned[0] = 8;
        assertEquals(1, query.getAttachments().get(0).getContent()[0]);
        assertTrue(query.getAttachments().get(0).isImage());
    }
}
