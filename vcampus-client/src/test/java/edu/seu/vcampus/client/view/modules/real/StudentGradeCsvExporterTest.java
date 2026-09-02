package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 成绩导出包含 UTF-8 BOM 和服务端四项指标。 */
public final class StudentGradeCsvExporterTest {
    @Test
    public void writesBomAndMetrics() throws Exception {
        File file = File.createTempFile("vcampus-grade-", ".csv");
        try {
            StudentGradeMetricsDto metrics = new StudentGradeMetricsDto("2026-FALL", 1, 1,
                    new BigDecimal("3.00"), new BigDecimal("4.50"), null, null, null);
            StudentGradeExportDto export = new StudentGradeExportDto(
                    Collections.<StudentGradeDto>emptyList(), metrics);
            StudentGradeCsvExporter.write(file, export);
            FileInputStream input = new FileInputStream(file);
            try {
                assertEquals(0xEF, input.read());
                assertEquals(0xBB, input.read());
                assertEquals(0xBF, input.read());
                byte[] bytes = new byte[(int) file.length()];
                input.close(); input = new FileInputStream(file); input.read(bytes);
                String text = new String(bytes, "UTF-8");
                org.junit.Assert.assertTrue(text.contains("加权绩点/平均学分绩点"));
                org.junit.Assert.assertTrue(text.contains("4.50"));
            } finally {
                input.close();
            }
        } finally {
            if (file.exists()) file.delete();
        }
    }
}
