package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;

import org.threeten.bp.LocalDateTime;

/** 来访登记在学生端与宿管端共用的文案、行映射与输入解析。 */
final class DormExtVisitorLabels {
    private DormExtVisitorLabels() { }

    /** 把界面上的中文筛选项翻回协议状态码。 */
    static String code(String filter) {
        if ("待审核".equals(filter)) return VisitorRegistrationDto.STATUS_PENDING;
        if ("已通过".equals(filter)) return VisitorRegistrationDto.STATUS_APPROVED;
        if ("已驳回".equals(filter)) return VisitorRegistrationDto.STATUS_REJECTED;
        if ("已撤销".equals(filter)) return VisitorRegistrationDto.STATUS_CANCELLED;
        return null;
    }

    static String label(String status) {
        if (VisitorRegistrationDto.STATUS_APPROVED.equals(status)) return "已通过";
        if (VisitorRegistrationDto.STATUS_REJECTED.equals(status)) return "已驳回";
        if (VisitorRegistrationDto.STATUS_CANCELLED.equals(status)) return "已撤销";
        return "待审核";
    }

    /** 宿管视图多两列：提交学生和所在房间。 */
    static Object[] row(VisitorRegistrationDto row, boolean includeStudent) {
        if (includeStudent) {
            return new Object[]{row.getId(), row.getStudentUserId(),
                    RealUi.text(row.getBuildingCode()) + " " + RealUi.text(row.getRoomNo()),
                    RealUi.text(row.getVisitorName()), RealUi.text(row.getVisitorIdCardMasked()),
                    RealUi.text(row.getVisitReason()), RealUi.dateTime(row.getStartAt()),
                    RealUi.dateTime(row.getEndAt()), label(row.getAuditStatus())};
        }
        return new Object[]{row.getId(), RealUi.text(row.getVisitorName()),
                RealUi.text(row.getVisitorIdCardMasked()), RealUi.text(row.getVisitReason()),
                RealUi.dateTime(row.getStartAt()), RealUi.dateTime(row.getEndAt()),
                label(row.getAuditStatus()), RealUi.text(row.getAuditRemark())};
    }

    /**
     * 解析日期时间输入。
     *
     * <p>界面提示的是 {@code yyyy-MM-dd HH:mm}（空格分隔，符合日常书写习惯），
     * 而 ThreeTen 的 parse 要求 ISO 的 'T'，因此这里做一次转换；两种写法都接受。</p>
     */
    static LocalDateTime dateTime(String text, String label) {
        String value = RealUi.required(text, label).trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm。");
        }
    }
}
