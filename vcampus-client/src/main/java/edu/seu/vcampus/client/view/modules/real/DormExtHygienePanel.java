package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.threeten.bp.LocalDate;

/**
 * 宿管员卫生检查：五项分项打分与检查任务清单。
 *
 * <p>界面只收五项分数，总分、等级和是否整改都由服务端算完回传，避免两边算法
 * 走偏；不合格时服务端会自动排一条复查任务，列表里能直接看到。</p>
 */
public final class DormExtHygienePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<HygieneTaskDto> tasks;

    private final JTextField room = UiFactory.textField(8);
    private final JTextField[] scores = new JTextField[HygieneItemScoreDto.ITEM_CODES.length];
    private final JTextField issue = UiFactory.textField(20);

    public DormExtHygienePanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        for (int i = 0; i < scores.length; i++) scores[i] = UiFactory.textField(6);
        this.tasks = taskTable();
        tasks.addAction(generateButton());
        add(scoreForm());
        add(tasks);
        resetScores();
    }

    public void reload() { tasks.reload(); }

    private JPanel scoreForm() {
        SectionCard card = new SectionCard("卫生检查评分",
                "五项各 0~20 分；总分与等级由服务端计算，低于 "
                + "70 分自动下发整改并排复查任务。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("房间编号", room));
        for (int i = 0; i < HygieneItemScoreDto.ITEM_CODES.length; i++) {
            fields.add(UiFactory.labelledField(
                    HygieneItemScoreDto.itemName(HygieneItemScoreDto.ITEM_CODES[i]) + "（0-20）",
                    scores[i]));
        }
        fields.add(UiFactory.labelledField("问题描述", issue));

        JPanel line = UiFactory.horizontal(8);
        JButton reset = new SecondaryButton("重置为满分");
        reset.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { resetScores(); }
        });
        JButton submit = new PrimaryButton("提交检查");
        submit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit(); }
        });
        line.add(reset);
        line.add(submit);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private AsyncPagedTable<HygieneTaskDto> taskTable() {
        return new AsyncPagedTable<HygieneTaskDto>("检查任务",
                "顺序即待办顺序：待检查在最前，复查优先于周检查，同组按计划日期从早到晚。",
                "搜索房间或楼栋",
                new String[]{"全部状态", "待检查", "已完成", "已跳过"},
                new String[]{"编号", "楼栋", "房间", "类型", "计划日期", "状态", "关联检查"},
                new AsyncPagedTable.Loader<HygieneTaskDto>() {
                    @Override
                    public PageSlice<HygieneTaskDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<HygieneTaskDto> value = service.hygieneTasks(
                                new DormPageQuery(p, 20, keyword, taskStatus(filter), null, null));
                        return RealUi.page(value);
                    }
                },
                new AsyncPagedTable.RowMapper<HygieneTaskDto>() {
                    @Override public Object[] values(HygieneTaskDto row) {
                        return new Object[]{row.getId(), RealUi.text(row.getBuildingCode()),
                                RealUi.text(row.getRoomNo()),
                                row.isRecheck() ? "复查" : "周检查",
                                RealUi.date(row.getPlanDate()), taskLabel(row.getStatus()),
                                RealUi.text(row.getInspectionId())};
                    }
                }, null);
    }

    /**
     * 补生成本周检查任务。
     *
     * <p>正常情况下每周一 09:00 由 DormScheduler 自动生成，这个按钮只是补漏用的手动
     * 入口（比如那天服务端没开），不值得单独占一整块，挂在任务表的工具栏上就够了。
     * 计划日期固定取今天：要补的就是当期，填别的日期只会造出对不上的任务。</p>
     */
    private JButton generateButton() {
        JButton generate = new SecondaryButton("补生成本周任务");
        generate.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { generate(); }
        });
        return generate;
    }

    private void resetScores() {
        room.setText("");
        issue.setText("");
        for (JTextField field : scores) field.setText("20");
    }

    private void submit() {
        final HygieneScoreSubmitRequest request;
        try {
            Long roomId = RealUi.number(RealUi.required(room.getText(), "房间编号"));
            if (roomId == null || roomId.longValue() <= 0L) {
                throw new IllegalArgumentException("房间编号必须是正整数。");
            }
            List<HygieneItemScoreDto> items = new ArrayList<HygieneItemScoreDto>();
            for (int i = 0; i < HygieneItemScoreDto.ITEM_CODES.length; i++) {
                String code = HygieneItemScoreDto.ITEM_CODES[i];
                items.add(new HygieneItemScoreDto(code,
                        score(scores[i].getText(), HygieneItemScoreDto.itemName(code)), null));
            }
            request = new HygieneScoreSubmitRequest(roomId.longValue(), items,
                    RealUi.optional(issue.getText()));
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        AsyncTask.run(new AsyncTask.Work<HygieneDetailDto>() {
            @Override public HygieneDetailDto run() throws Exception {
                return service.submitHygiene(request);
            }
        }, new AsyncTask.Callback<HygieneDetailDto>() {
            @Override public void onSuccess(HygieneDetailDto value) {
                page.showSuccess("检查已提交：总分 " + value.getTotalScore() + "，等级 "
                        + HygieneDetailDto.levelName(value.getScoreLevel())
                        + (value.isNeedRectify()
                                ? "，需整改，复查安排在 " + value.getRecheckDate() : "，无需整改"));
                tasks.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void generate() {
        final LocalDate date = LocalDate.now();
        AsyncTask.run(new AsyncTask.Work<HygieneTaskGenerateResultDto>() {
            @Override public HygieneTaskGenerateResultDto run() throws Exception {
                return service.generateHygieneTasks(new HygieneTaskGenerateRequest(null, date));
            }
        }, new AsyncTask.Callback<HygieneTaskGenerateResultDto>() {
            @Override public void onSuccess(HygieneTaskGenerateResultDto value) {
                page.showSuccess("计划日 " + value.getPlanDate() + "：扫描房间 "
                        + value.getRoomsScanned() + " 间，新建任务 " + value.getCreated()
                        + " 条，已存在 " + value.getExisting() + " 条。");
                tasks.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static BigDecimal score(String text, String label) {
        String value = RealUi.required(text, label);
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.signum() < 0
                    || parsed.compareTo(BigDecimal.valueOf(HygieneItemScoreDto.MAX_ITEM_SCORE)) > 0) {
                throw new IllegalArgumentException(label + "得分必须在 0 到 "
                        + HygieneItemScoreDto.MAX_ITEM_SCORE + " 之间。");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + "得分必须是数字。");
        }
    }

    private static String taskStatus(String filter) {
        if ("待检查".equals(filter)) return HygieneTaskDto.STATUS_PENDING;
        if ("已完成".equals(filter)) return HygieneTaskDto.STATUS_DONE;
        if ("已跳过".equals(filter)) return HygieneTaskDto.STATUS_SKIPPED;
        return null;
    }

    private static String taskLabel(String status) {
        if (HygieneTaskDto.STATUS_DONE.equals(status)) return "已完成";
        if (HygieneTaskDto.STATUS_SKIPPED.equals(status)) return "已跳过";
        return "待检查";
    }
}
