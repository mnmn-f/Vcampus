package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

/** 将服务端限定范围内的成绩与四项指标导出为 UTF-8 BOM CSV。 */
final class StudentGradeCsvExporter {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private StudentGradeCsvExporter() {
    }

    static void write(File file, StudentGradeExportDto export) throws IOException {
        if (file == null || export == null) throw new IllegalArgumentException("export is required");
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF8);
        try {
            writer.write('\uFEFF');
            writer.write("学期,课程编号,课程名称,学分,成绩,绩点,是否计入指标\r\n");
            writeRows(writer, export.getItems());
            writeMetrics(writer, export.getMetrics());
        } finally {
            writer.close();
        }
    }

    private static void writeRows(Writer writer, List<StudentGradeDto> rows) throws IOException {
        for (StudentGradeDto row : rows) {
            row(writer, row.getSemesterCode(), row.getCourseCode(), row.getCourseName(),
                    row.getCredits(), row.getScore(), row.getGradePoint(),
                    row.isGpaIncluded() ? "是" : "否");
        }
    }

    private static void writeMetrics(Writer writer, StudentGradeMetricsDto metrics)
            throws IOException {
        writer.write("\r\n"); row(writer, "指标", "数值");
        if (metrics == null) return;
        metric(writer, "加权绩点/平均学分绩点", metrics.getWeightedGpa());
        metric(writer, "平均绩点", metrics.getAverageGpa());
        metric(writer, "加权均分", metrics.getWeightedAverageScore());
        metric(writer, "平均均分", metrics.getAverageScore());
    }

    private static void metric(Writer writer, String label, Object value) throws IOException {
        row(writer, label, value);
    }

    private static void row(Writer writer, Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) writer.write(',');
            String text = values[i] == null ? "" : String.valueOf(values[i]);
            writer.write('"'); writer.write(text.replace("\"", "\"\"")); writer.write('"');
        }
        writer.write("\r\n");
    }
}
