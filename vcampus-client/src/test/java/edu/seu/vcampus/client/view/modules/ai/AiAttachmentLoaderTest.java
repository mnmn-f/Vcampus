package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.common.ai.AiAttachment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.FileOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class AiAttachmentLoaderTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void loadsTextAndRejectsUnsupportedBinary() throws Exception {
        File text = temporary.newFile("question.md");
        Files.write(text.toPath(), "解释这段内容".getBytes(StandardCharsets.UTF_8));
        AiAttachment loaded = new AiAttachmentLoader().load(text);
        assertEquals("question.md", loaded.getFileName());
        assertTrue(loaded.isText());

        File binary = temporary.newFile("archive.zip");
        Files.write(binary.toPath(), new byte[] {1, 2, 3});
        try {
            new AiAttachmentLoader().load(binary);
            fail("unsupported binary should be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("暂不支持"));
        }
    }

    @Test public void extractsDocxAndSpreadsheetAsBoundedText() throws Exception {
        File docx = temporary.newFile("notice.docx");
        try (XWPFDocument document = new XWPFDocument();
                FileOutputStream output = new FileOutputStream(docx)) {
            document.createParagraph().createRun().setText("宿舍安全规定");
            document.write(output);
        }
        AiAttachment doc = new AiAttachmentLoader().load(docx);
        assertTrue(new String(doc.getContent(), StandardCharsets.UTF_8).contains("宿舍安全规定"));

        File xlsx = temporary.newFile("schedule.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                FileOutputStream output = new FileOutputStream(xlsx)) {
            workbook.createSheet("课表").createRow(0).createCell(0).setCellValue("高等数学");
            workbook.write(output);
        }
        AiAttachment sheet = new AiAttachmentLoader().load(xlsx);
        String content = new String(sheet.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("工作表：课表"));
        assertTrue(content.contains("高等数学"));
    }

    @Test public void extractsPdfText() throws Exception {
        File pdf = temporary.newFile("rules.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(); document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.showText("Campus library rules");
                content.endText();
            }
            document.save(pdf);
        }
        AiAttachment loaded = new AiAttachmentLoader().load(pdf);
        assertTrue(new String(loaded.getContent(), StandardCharsets.UTF_8)
                .contains("Campus library rules"));
    }
}
