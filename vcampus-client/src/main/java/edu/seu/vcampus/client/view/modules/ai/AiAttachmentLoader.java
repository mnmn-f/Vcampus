package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.common.ai.AiAttachment;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Locale;

/** 在客户端安全提取附件文字；服务端只接收有界图片或 UTF-8 文本。 */
final class AiAttachmentLoader {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int IMAGE_LIMIT = 2 * 1024 * 1024;
    private static final int TEXT_LIMIT = 256 * 1024;
    private static final int DOCUMENT_LIMIT = 10 * 1024 * 1024;
    private static final int MAX_EXTRACTED_CHARS = 60000;
    private static final int MAX_SHEET_CELLS = 12000;

    AiAttachment load(File file) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("请选择有效文件");
        String lower = file.getName().toLowerCase(Locale.ROOT);
        String imageType = imageType(lower);
        if (imageType != null) return image(file, imageType);
        if (textType(lower)) return textFile(file);
        if (documentType(lower)) return document(file, lower);
        throw new IOException("暂不支持该格式；可上传图片、PDF、DOCX、PPTX、XLS/XLSX、CSV、文本或代码文件");
    }

    private AiAttachment image(File file, String mediaType) throws IOException {
        requireSize(file, IMAGE_LIMIT, "图片不能超过 2 MB");
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (!hasImageSignature(bytes, mediaType)) {
            throw new IOException("图片内容与扩展名不匹配或文件已损坏");
        }
        return new AiAttachment(file.getName(), mediaType, bytes);
    }

    private AiAttachment textFile(File file) throws IOException {
        requireSize(file, TEXT_LIMIT, "文本文件不能超过 256 KB");
        byte[] bytes = Files.readAllBytes(file.toPath());
        String text = new String(bytes, UTF8);
        if (text.indexOf('\u0000') >= 0) throw new IOException("文本文件不是有效的 UTF-8 文本");
        return extracted(file, text);
    }

    private AiAttachment document(File file, String lower) throws IOException {
        requireSize(file, DOCUMENT_LIMIT, "文档不能超过 10 MB");
        String text;
        try {
            if (lower.endsWith(".pdf")) text = pdf(file);
            else if (lower.endsWith(".docx")) text = docx(file);
            else if (lower.endsWith(".pptx")) text = pptx(file);
            else text = workbook(file);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("文档无法读取，可能已损坏、加密或格式不受支持", ex);
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IOException(lower.endsWith(".pdf")
                    ? "PDF 没有可提取文字；扫描版 PDF 请转为图片后上传"
                    : "文档中没有可发送给 AI 的文字内容");
        }
        return extracted(file, text);
    }

    private String pdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) throw new IOException("暂不支持加密 PDF");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String docx(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
                XWPFDocument document = new XWPFDocument(input);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String pptx(File file) throws IOException {
        StringBuilder out = new StringBuilder();
        try (FileInputStream input = new FileInputStream(file);
                XMLSlideShow slides = new XMLSlideShow(input)) {
            for (int index = 0; index < slides.getSlides().size(); index++) {
                out.append("第").append(index + 1).append("页\n");
                for (XSLFShape shape : slides.getSlides().get(index).getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        String text = ((XSLFTextShape) shape).getText();
                        if (text != null && !text.trim().isEmpty()) out.append(text.trim()).append('\n');
                    }
                }
                if (out.length() >= MAX_EXTRACTED_CHARS) break;
            }
        }
        return out.toString();
    }

    private String workbook(File file) throws IOException {
        StringBuilder out = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        int cells = 0;
        try (Workbook workbook = WorkbookFactory.create(file)) {
            for (Sheet sheet : workbook) {
                out.append("工作表：").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    boolean wrote = false;
                    for (Cell cell : row) {
                        if (cells++ >= MAX_SHEET_CELLS) {
                            out.append("表格内容已达到提取上限\n");
                            return out.toString();
                        }
                        String value = formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) {
                            if (wrote) out.append('\t');
                            out.append(value); wrote = true;
                        }
                    }
                    if (wrote) out.append('\n');
                    if (out.length() >= MAX_EXTRACTED_CHARS) return out.toString();
                }
            }
        }
        return out.toString();
    }

    private AiAttachment extracted(File file, String value) throws IOException {
        String text = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (text.length() > MAX_EXTRACTED_CHARS) {
            text = text.substring(0, MAX_EXTRACTED_CHARS) + "\n文档内容已截断";
        }
        byte[] bytes = text.getBytes(UTF8);
        if (bytes.length > TEXT_LIMIT) throw new IOException("提取后的文档文字超过 256 KB");
        return new AiAttachment(file.getName(), "text/plain", bytes);
    }

    private void requireSize(File file, int limit, String message) throws IOException {
        if (file.length() <= 0 || file.length() > limit) throw new IOException(message);
    }

    private String imageType(String lower) {
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return null;
    }

    private boolean textType(String lower) {
        return lower.matches(".*\\.(txt|md|csv|log|java|py|js|ts|sql|yaml|yml|properties|html|css|json|xml)$");
    }

    private boolean documentType(String lower) {
        return lower.endsWith(".pdf") || lower.endsWith(".docx")
                || lower.endsWith(".pptx") || lower.endsWith(".xls")
                || lower.endsWith(".xlsx");
    }

    private boolean hasImageSignature(byte[] bytes, String type) {
        if (bytes.length < 12) return false;
        if ("image/png".equals(type)) {
            return bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                    && bytes[3] == 'G';
        }
        if ("image/jpeg".equals(type)) {
            return bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8;
        }
        if ("image/gif".equals(type)) {
            return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
        }
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
