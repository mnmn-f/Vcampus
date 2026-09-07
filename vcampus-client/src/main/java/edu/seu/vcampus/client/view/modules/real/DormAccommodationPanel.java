package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.DormHomeSummaryDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.math.BigDecimal;

/**
 * 学生「我的住宿」首屏。
 *
 * <p>上下栏而不是左右栏：进这一页的人先问「我住哪、这个月要交多少、有没有事要办」，
 * 这三件事是一条自上而下的线；分成左右两列会逼着眼睛来回横跳，而右列往往只剩下
 * 半屏空白。</p>
 *
 * <p>顶部四个数字（同寝人数、本月水电、最近卫生分、待办）由服务端一次算好返回，
 * 不在界面上拼三次请求——分开取的话，页面上会出现「卫生分已经复查通过、提示条
 * 却还在喊整改」这种自相矛盾的画面。</p>
 */
public final class DormAccommodationPanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final DormExtClientService ext;

    private final JLabel heading = DormUi.pageTitle("住宿信息加载中…");
    private final JLabel subtitle = DormUi.sub("");
    private final JPanel headingRow = new JPanel();
    private final JPanel statsHolder = new JPanel(new BorderLayout());
    private final JPanel noticeHolder = new JPanel(new BorderLayout());

    private final JComboBox<RealUi.CodeOption> type = new JComboBox<RealUi.CodeOption>(
            RealUi.options("CHECK_IN", "TRANSFER", "CHECK_OUT"));
    // 「当前记录编号」不再让学生填：调宿和退宿要带的是本人当前那条住宿记录的编号，
    // 学生既不知道它是什么，填错了还会把申请挂到别人的记录上。页面加载时从
    // myAccommodation 取到多少就是多少，界面上不出现。
    private long recordId;
    private final JTextField reason = UiFactory.textField(18);
    private final AsyncPagedTable<AccommodationRequestDto> requests;
    private long accommodationId;

    public DormAccommodationPanel(BasePage page, DormClientService service, DormExtClientService ext) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.ext = ext;

        add(header());
        add(Box.createVerticalStrut(20));
        add(statsHolder);
        add(noticeHolder);
        requests = requests();
        // 分隔线画在这一层而不是「住宿申请」自己的标题上方：申请表单和申请记录是并列
        // 的两块，它们共同和上面的住宿信息分开。挂在左栏标题上的话，线只有左半截长，
        // 右边的「我的申请」看起来就像还属于上一段。
        JComponent rule = DormUi.rule();
        rule.setAlignmentX(LEFT_ALIGNMENT);
        add(rule);
        add(Box.createVerticalStrut(18));
        // 申请表单在左、申请记录在右：先填，再在右边看它排到哪儿了。上下堆会让表单
        // 独占一整屏宽，而它只需要两个短字段。
        add(DormUi.splitLeading(requestForm(), requests, 330));

        statsHolder.setOpaque(false);
        statsHolder.setAlignmentX(LEFT_ALIGNMENT);
        noticeHolder.setOpaque(false);
        noticeHolder.setAlignmentX(LEFT_ALIGNMENT);
        loadCurrent();
        loadSummary();
    }

    public void reload() {
        loadCurrent();
        loadSummary();
        requests.reload();
    }

    private JPanel header() {
        headingRow.setOpaque(false);
        headingRow.setLayout(new BoxLayout(headingRow, BoxLayout.X_AXIS));
        headingRow.add(heading);
        headingRow.add(Box.createHorizontalGlue());

        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        headingRow.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        block.add(headingRow);
        block.add(Box.createVerticalStrut(6));
        block.add(subtitle);
        block.setAlignmentX(LEFT_ALIGNMENT);
        return block;
    }

    // ---------- 首屏摘要 ----------

    private void loadSummary() {
        AsyncTask.run(new AsyncTask.Work<DormHomeSummaryDto>() {
            @Override public DormHomeSummaryDto run() throws Exception { return ext.homeSummary(); }
        }, new AsyncTask.Callback<DormHomeSummaryDto>() {
            @Override public void onSuccess(DormHomeSummaryDto value) { showSummary(value); }
            @Override public void onFailure(Throwable error) { showSummary(null); }
        });
    }

    private void showSummary(DormHomeSummaryDto summary) {
        statsHolder.removeAll();
        noticeHolder.removeAll();
        if (summary == null || !summary.isResident()) {
            statsHolder.revalidate();
            statsHolder.repaint();
            noticeHolder.revalidate();
            noticeHolder.repaint();
            return;
        }
        String beds = summary.getCapacity() > 0 ? "/ " + summary.getCapacity() + " 床位" : "人在住";
        String hygiene = summary.getHygieneScore() == null ? "—" : trim(summary.getHygieneScore());
        String hygieneUnit = summary.getHygieneScore() == null ? "暂无检查记录"
                : "/ 100 " + (summary.needsRectification() ? "待整改" : "合格");
        int todo = summary.todoCount();
        JPanel stats = DormUi.stats(
                new Color[]{null, summary.getUnpaidBills() > 0 ? DesignTokens.WARNING : null,
                        summary.needsRectification() ? DesignTokens.ERROR : null,
                        todo > 0 ? DesignTokens.WARNING : null},
                "同寝人数", String.valueOf(summary.getOccupiedBeds()), beds,
                "未缴水电", "￥" + trim(summary.getUnpaidAmount()),
                summary.getUnpaidBills() > 0 ? summary.getUnpaidBills() + " 笔待缴" : "已结清",
                "最近卫生分", hygiene, hygieneUnit,
                "待办", String.valueOf(todo), "条");
        stats.setAlignmentX(LEFT_ALIGNMENT);
        statsHolder.add(stats, BorderLayout.CENTER);
        statsHolder.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 22, 0));

        if (summary.needsRectification()) {
            String detail = summary.getHygieneIssue() == null || summary.getHygieneIssue().trim().isEmpty()
                    ? "宿管已判定本次卫生检查不合格，整改完成前该房间不计入评优。整改后会安排复查。"
                    : summary.getHygieneIssue().trim();
            JPanel notice = DormUi.notice(DormUi.Tone.ERROR, "卫生检查未通过，需要整改后复查", detail, null);
            notice.setAlignmentX(LEFT_ALIGNMENT);
            noticeHolder.add(notice, BorderLayout.CENTER);
            noticeHolder.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 22, 0));
        }
        statsHolder.revalidate();
        statsHolder.repaint();
        noticeHolder.revalidate();
        noticeHolder.repaint();
    }

    /** 金额和分数去掉无意义的小数尾巴：62.00 显示成 62，100.30 保持原样。 */
    private static String trim(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0 ? stripped.toBigInteger().toString() : stripped.toPlainString();
    }

    // ---------- 申请 ----------

    private JPanel requestForm() {
        type.setFont(DesignTokens.regular(15));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 11));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("申请类型", type));
        fields.add(UiFactory.labelledField("申请原因", reason));

        JPanel actions = UiFactory.horizontal(10);
        JButton submit = new PrimaryButton("提交申请");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton checkout = new DangerButton("确认退宿申请");
        checkout.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { checkout(); }
        });
        actions.add(submit);
        actions.add(checkout);

        // 表单是这一页唯一围起来的部分：填写区需要一个明确的边界，其余内容不需要。
        JPanel box = DormUi.panel();
        JPanel inner = new JPanel(new BorderLayout(0, 14));
        inner.setOpaque(false);
        inner.add(fields, BorderLayout.CENTER);
        inner.add(actions, BorderLayout.SOUTH);
        box.add(inner, BorderLayout.CENTER);

        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.add(DormUi.header("住宿申请",
                "入住、调宿和退宿统一提交；床位由宿管审批时统一调配，退宿需要二次确认。", null, false));
        box.setAlignmentX(LEFT_ALIGNMENT);
        section.add(box);
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private AsyncPagedTable<AccommodationRequestDto> requests() {
        return new AsyncPagedTable<AccommodationRequestDto>("我的申请", "仅展示你的申请记录。", "按申请类型或原因搜索",
                new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"},
                new String[]{"编号", "类型", "原因", "状态", "审批意见", "提交时间"},
                new AsyncPagedTable.Loader<AccommodationRequestDto>() {
                    @Override public PageSlice<AccommodationRequestDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.requests(new DormPageQuery(p, 20, keyword, requestStatus(filter), null, null), null));
                    }
                }, new AsyncPagedTable.RowMapper<AccommodationRequestDto>() {
                    @Override public Object[] values(AccommodationRequestDto row) { return new Object[]{row.getId(), RealUi.status(row.getRequestType()),
                            RealUi.text(row.getReason()), RealUi.status(row.getStatus()), RealUi.text(row.getReviewRemark()), RealUi.dateTime(row.getCreatedAt())}; }
                }, null);
    }

    private void loadCurrent() {
        heading.setText("住宿信息加载中…");
        AsyncTask.run(new AsyncTask.Work<AccommodationDto>() {
            @Override public AccommodationDto run() throws Exception { return service.myAccommodation(); }
        }, new AsyncTask.Callback<AccommodationDto>() {
            @Override public void onSuccess(AccommodationDto value) { showAccommodation(value); }
            @Override public void onFailure(Throwable error) {
                heading.setText("住宿信息暂时无法显示");
                subtitle.setText(AsyncTask.message(error));
                page.showError(AsyncTask.message(error));
            }
        });
    }

    private void showAccommodation(AccommodationDto value) {
        accommodationId = value == null ? 0L : value.getId();
        recordId = accommodationId;
        if (value == null) {
            heading.setText("当前暂无在住记录");
            subtitle.setText("分配床位后，这里会显示楼栋、房间和床位。");
            return;
        }
        heading.setText(RealUi.text(value.getBuildingName()) + " · " + RealUi.text(value.getRoomNo())
                + " · 床位 " + RealUi.text(value.getBedNo()));
        subtitle.setText("入住起始 " + RealUi.date(value.getStartDate()) + "　·　状态 " + RealUi.status(value.getStatus()));
        if (headingRow.getComponentCount() < 3) {
            headingRow.add(Box.createHorizontalStrut(12), 1);
            headingRow.add(DormUi.badge(RealUi.status(value.getStatus()), DormUi.Tone.OK), 2);
            headingRow.revalidate();
        }
    }

    /**
     * 提交申请。
     *
     * <p>不带目标床位：学生看不到哪张床空着，让他填一个编号只会填错，或者填了之后
     * 宿管还得改。床位在宿管审批那一步统一指定。</p>
     */
    private void submit() {
        String selected = RealUi.code(type.getSelectedItem());
        try {
            Long currentRecord = recordId > 0L ? Long.valueOf(recordId) : null;
            if (selected == null || "--".equals(selected)) throw new IllegalArgumentException("请选择申请类型");
            if (!"CHECK_IN".equals(selected) && currentRecord == null) throw new IllegalArgumentException("没有读到你的在住记录，无法提交该类型申请");
            final String finalSelected = selected; final Long finalCurrentRecord = currentRecord;
            AsyncTask.run(new AsyncTask.Work<AccommodationRequestDto>() {
                @Override public AccommodationRequestDto run() throws Exception { return service.submitRequest(new AccommodationRequest(finalSelected, finalCurrentRecord, null, RealUi.optional(reason.getText()))); }
            }, new AsyncTask.Callback<AccommodationRequestDto>() {
                @Override public void onSuccess(AccommodationRequestDto value) { page.showSuccess("住宿申请已提交，等宿管分配床位。"); reason.setText(""); requests.reload(); loadSummary(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private void checkout() {
        type.setSelectedItem(RealUi.option("CHECK_OUT"));
        if (accommodationId == 0L) { page.showWarning("当前没有可退宿的住宿记录。"); return; }
        if (!RealUi.confirm(this, "确认提交退宿申请？")) return;
        submit();
    }

    private static PageSlice<AccommodationRequestDto> slice(DormPage<AccommodationRequestDto> value) { return RealUi.page(value); }
    private static String requestStatus(String filter) { if ("待审批".equals(filter)) return "PENDING"; if ("已通过".equals(filter)) return "APPROVED"; if ("已驳回".equals(filter)) return "REJECTED"; if ("已取消".equals(filter)) return "CANCELLED"; return null; }
}
