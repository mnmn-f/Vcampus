package edu.seu.vcampus.client.view.modules.real;

import java.util.Locale;

/** PDF 界面的状态、日期和字节数显示。 */
final class PdfUi {
    private PdfUi() { }
    static String state(String value) {
        if (value == null) return "--";
        switch (value) {
            case "PENDING": return "待审核";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已拒绝";
            case "INACTIVE": return "已停用";
            case "DOWNLOADING": return "未完成";
            case "COMPLETED": return "已完成";
            case "FAILED": return "失败";
            default: return value;
        }
    }
    static String filter(String value) {
        if ("待审核".equals(value)) return "PENDING";
        if ("已通过".equals(value)) return "APPROVED";
        if ("已拒绝".equals(value)) return "REJECTED";
        if ("已停用".equals(value)) return "INACTIVE";
        return null;
    }
    static String date(long millis) { return millis <= 0 ? "--" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(millis)); }
    static String size(long bytes) {
        return bytes < 1024 * 1024 ? String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
                : String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
    }
}
