package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRepairStatus;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormHomeSummaryDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生端报修服务：左边一列工单查进度，右边一栏看选中工单的详情。
 *
 * <p>之前这一页是三个面板竖着摞：报修表单、入内授权、报修评价，各带一张表，同一批
 * 工单被列了三遍，而「这单到哪一步了」反倒没地方看。现在合成一页——左边是工单列表，
 * 右边跟着选中的那一单走，进度、入内授权、评价都在同一处，因为它们说的是同一件事。</p>
 *
 * <p>提交新报修改成弹窗：一年提交不了几次，却要在页面上常驻六个输入框，把真正天天
 * 要看的进度挤到下面去。</p>
 */
public final class DormStudentRepairPage extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIDE_WIDTH = 400;

    private final BasePage page;
    private final DormClientService service;
    private final DormExtClientService ext;
    private final AsyncPagedTable<RepairOrderDto> orders;

    /** 入内授权与工单是两条查询，按工单号在客户端合起来显示。 */
    private final Map<Long, RepairEntryPermitDto> permits = new HashMap<Long, RepairEntryPermitDto>();

    private final JPanel detail = new JPanel();
    private final JComboBox<Integer> score = new JComboBox<Integer>(new Integer[]{5, 4, 3, 2, 1});
    private final JTextField evaluationNote = UiFactory.textField(16);
    private RepairOrderDto selected;
    private long myRoomId;
    private String myRoomLabel = "";

    /** 新建报修的表单：展开在页面正下方，不另开窗口。 */
    private final JPanel compose = new JPanel();
    private final JLabel composeRoom = UiFactory.body("");
    private final JComboBox<RealUi.CodeOption> composeCategory = new JComboBox<RealUi.CodeOption>(
            RealUi.options("WATER", "LIGHTING", "ELECTRIC", "NETWORK", "FURNITURE", "APPLIANCE",
                    "AIR_CONDITIONING", "DOOR_WINDOW", "EQUIPMENT"));
    private final JComboBox<RealUi.CodeOption> composePriority =
            new JComboBox<RealUi.CodeOption>(RealUi.options("NORMAL", "HIGH", "LOW"));
    /**
     * 报修时就把「我不在能不能进门」定下来。
     *
     * <p>之前它是选中工单后在右栏单独填的一块，可这件事对每张工单的答案本来就不一样
     * ——修个灯泡可以随便进，修完还要验收的就不一定——放在工单外面等于让人为每一单
     * 再回来点一次。做成两个选项而不是一个备注框：学生要表达的只是「行」或「不行」，
     * 不该逼他现编一句话。</p>
     */
    private final JComboBox<String> composeEntry = new JComboBox<String>(new String[]{
            "不授权，等我在宿舍时上门", "授权，我不在也可以进门作业"});
    private final JTextArea composeDescription = UiFactory.textArea(4, 40);

    public DormStudentRepairPage(BasePage page, DormClientService service, DormExtClientService ext) {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());
        this.page = page;
        this.service = service;
        this.ext = ext;
        orders = table();
        detail.setOpaque(false);
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        showDetail(null);
        compose.setOpaque(false);
        compose.setLayout(new BoxLayout(compose, BoxLayout.Y_AXIS));
        compose.setVisible(false);
        buildCompose();
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(DormUi.split(left(), detail, SIDE_WIDTH));
        body.add(compose);
        add(body, BorderLayout.CENTER);
        loadPermits();
        loadRoom();
    }

    public void reload() {
        orders.reload();
        loadPermits();
    }

    // ---------- 左栏：工单列表 ----------

    private JPanel left() {
        JButton create = new PrimaryButton("新建报修");
        create.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCompose(!compose.isVisible()); }
        });
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("我的报修工单", "选中一条查看进度、入内授权与评价。",
                DormUi.actions(create), false));
        column.add(orders);
        return column;
    }

    private AsyncPagedTable<RepairOrderDto> table() {
        return new AsyncPagedTable<RepairOrderDto>("", "", "按类别或描述搜索",
                new String[]{"全部状态", "待派单", "已派单", "处理中", "待宿管审核", "已完成", "已取消"},
                new String[]{"工单", "类别", "描述", "优先级", "状态", "提交时间"},
                new AsyncPagedTable.Loader<RepairOrderDto>() {
                    @Override public PageSlice<RepairOrderDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(service.repairs(new DormPageQuery(p, 20, keyword, repairStatus(filter), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<RepairOrderDto>() {
                    @Override public Object[] values(RepairOrderDto row) {
                        return new Object[]{Long.valueOf(row.getId()), RealUi.status(row.getCategory()),
                                RealUi.text(row.getDescription()), RealUi.status(row.getPriority()),
                                RealUi.status(row.getStatus()), RealUi.dateTime(row.getSubmittedAt())};
                    }
                }, new AsyncPagedTable.SelectionListener<RepairOrderDto>() {
                    @Override public void onSelected(RepairOrderDto row) { showDetail(row); }
                });
    }

    // ---------- 右栏：选中工单的详情 ----------

    private void showDetail(RepairOrderDto value) {
        selected = value;
        detail.removeAll();
        if (value == null) {
            detail.add(DormUi.header("工单详情", "在左侧选中一条工单。", null, false));
            JLabel empty = DormUi.sub("还没有选中工单。新建报修后，这里会显示它走到了哪一步。");
            empty.setAlignmentX(LEFT_ALIGNMENT);
            detail.add(empty);
            DormUi.alignLeft(detail);
            detail.revalidate();
            detail.repaint();
            return;
        }
        detail.add(DormUi.header("工单 " + value.getId() + " · " + RealUi.status(value.getCategory()),
                RealUi.status(value.getStatus()) + "　·　提交于 " + RealUi.dateTime(value.getSubmittedAt()),
                null, false));
        detail.add(timeline(value));
        detail.add(Box.createVerticalStrut(18));
        detail.add(permitLine(value));
        if ("COMPLETED".equalsIgnoreCase(value.getStatus())) {
            detail.add(Box.createVerticalStrut(20));
            detail.add(evaluationBlock(value));
        }
        // 详情栏是选中工单后现搭的，没经过 DormUi.flatten，横向对齐还是默认的居中值；
        // BoxLayout 按各自的对齐值排，整栏就会看起来往右歪。
        DormUi.alignLeft(detail);
        detail.revalidate();
        detail.repaint();
    }

    /**
     * 进度时间线。
     *
     * <p>四步的完成与否直接从状态和三个时间戳推出来，不额外存一份进度记录：状态本身
     * 就是进度，另存一份迟早会和状态对不上。</p>
     */
    private JPanel timeline(RepairOrderDto value) {
        String status = value.getStatus() == null ? "" : value.getStatus().toUpperCase(java.util.Locale.ROOT);
        boolean cancelled = "CANCELLED".equals(status);
        boolean done = "COMPLETED".equals(status);
        boolean reviewing = "PENDING_REVIEW".equals(status);
        boolean working = "IN_PROGRESS".equals(status) || reviewing || done;
        boolean accepted = value.getAcceptedAt() != null || "ACCEPTED".equals(status) || working;

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(step("已提交", RealUi.dateTime(value.getSubmittedAt()), true, false));
        column.add(connector(accepted));
        column.add(step("已派单", accepted ? RealUi.dateTime(value.getAcceptedAt()) : "等待宿管派单", accepted, false));
        column.add(connector(working));
        column.add(step("处理中", working ? "维修员已到场处理" : "尚未开始", working, false));
        column.add(connector(reviewing || done));
        column.add(step("待宿管审核",
                done ? "宿管已审核通过" : reviewing ? "维修员报了完工，等宿管现场确认" : "维修员尚未报完工",
                reviewing || done, false));
        column.add(connector(done));
        column.add(step(cancelled ? "已取消" : "已完成",
                done ? RealUi.dateTime(value.getCompletedAt()) : cancelled ? "工单已取消" : "尚未结束",
                done || cancelled, cancelled));
        column.setAlignmentX(LEFT_ALIGNMENT);
        return column;
    }

    private JPanel step(String title, String note, boolean reached, boolean cancelled) {
        JPanel row = new JPanel(new BorderLayout(11, 0));
        row.setOpaque(false);
        Color accent = cancelled ? DesignTokens.ERROR : reached ? DesignTokens.PRIMARY : DesignTokens.BORDER;
        JLabel dot = new JLabel(reached ? "✓" : "", SwingConstants.CENTER);
        dot.setOpaque(true);
        dot.setBackground(reached ? accent : Color.WHITE);
        dot.setForeground(Color.WHITE);
        dot.setFont(DesignTokens.medium(11));
        dot.setBorder(BorderFactory.createLineBorder(accent));
        dot.setPreferredSize(new Dimension(20, 20));
        JPanel dotHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dotHolder.setOpaque(false);
        dotHolder.add(dot);
        row.add(dotHolder, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(title);
        name.setFont(DesignTokens.medium(14));
        name.setForeground(reached ? DesignTokens.TEXT_PRIMARY : DesignTokens.TEXT_PLACEHOLDER);
        text.add(name);
        JLabel when = DormUi.sub(note);
        text.add(when);
        row.add(text, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JComponent connector(boolean reached) {
        JPanel line = new JPanel();
        line.setOpaque(true);
        line.setBackground(reached ? DesignTokens.PRIMARY : DesignTokens.BORDER);
        line.setPreferredSize(new Dimension(2, 14));
        line.setMaximumSize(new Dimension(2, 14));
        line.setMinimumSize(new Dimension(2, 14));
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));
        holder.add(line);
        holder.setAlignmentX(LEFT_ALIGNMENT);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        return holder;
    }

    // ---------- 入内授权 ----------

    /**
     * 只读的一行授权状态。
     *
     * <p>授权在提交报修时就选好了，这里不再给一个能改的表单——同一件事在两处都能改，
     * 迟早会出现「学生以为撤了、维修员看到的还是已授权」。要换主意就撤单重报，或者
     * 直接联系宿管。</p>
     */
    private JPanel permitLine(RepairOrderDto value) {
        RepairEntryPermitDto permit = permits.get(Long.valueOf(value.getId()));
        boolean allowed = permit != null && permit.isAllowEnter();
        String phone = permit == null ? null : permit.getContactPhone();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.notice(allowed ? DormUi.Tone.OK : DormUi.Tone.INFO,
                allowed ? "已授权不在场入内" : "未授权不在场入内",
                allowed
                        ? (phone == null || phone.trim().isEmpty()
                                ? "维修员可以在你不在时进门作业。"
                                : "维修员可以在你不在时进门作业，需要时会打 " + phone + " 联系你。")
                        : "维修员会先联系你约时间，到场时你需要在宿舍。",
                null));
        column.setAlignmentX(LEFT_ALIGNMENT);
        return column;
    }

    private void loadPermits() {
        AsyncTask.run(new AsyncTask.Work<DormPage<RepairEntryPermitDto>>() {
            @Override public DormPage<RepairEntryPermitDto> run() throws Exception {
                return ext.myRepairPermits(new DormPageQuery(1, 100, null, null, null, null));
            }
        }, new AsyncTask.Callback<DormPage<RepairEntryPermitDto>>() {
            @Override public void onSuccess(DormPage<RepairEntryPermitDto> value) {
                permits.clear();
                if (value != null && value.getItems() != null) {
                    for (RepairEntryPermitDto item : value.getItems()) {
                        permits.put(Long.valueOf(item.getRepairOrderId()), item);
                    }
                }
                if (selected != null) showDetail(selected);
            }
            @Override public void onFailure(Throwable error) { /* 授权读不到不影响看进度，静默降级 */ }
        });
    }

    // ---------- 评价 ----------

    private JPanel evaluationBlock(RepairOrderDto value) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        if (value.getEvaluationScore() != null) {
            column.add(DormUi.header("我的评价", "每个工单只能评价一次。", null, true));
            JLabel done = UiFactory.body(value.getEvaluationScore() + " 分　"
                    + (value.getEvaluationNote() == null ? "" : value.getEvaluationNote()));
            done.setAlignmentX(LEFT_ALIGNMENT);
            column.add(done);
            column.setAlignmentX(LEFT_ALIGNMENT);
            return column;
        }
        column.add(DormUi.header("评价这次维修", "评分 1–5 分，提交后不能修改。", null, true));
        score.setFont(DesignTokens.regular(15));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(labelled("评分", score, 90));
        row.add(labelled("评价内容", evaluationNote, 232));
        row.setAlignmentX(LEFT_ALIGNMENT);
        column.add(row);
        column.add(Box.createVerticalStrut(12));
        JButton submit = new PrimaryButton("提交评价");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { evaluate(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(submit);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        column.add(buttons);
        column.setAlignmentX(LEFT_ALIGNMENT);
        return column;
    }

    private void evaluate() {
        if (selected == null) return;
        final RepairEvaluationRequest request = new RepairEvaluationRequest(selected.getId(),
                ((Integer) score.getSelectedItem()).intValue(), RealUi.optional(evaluationNote.getText()));
        AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() {
            @Override public RepairOrderDto run() throws Exception { return service.evaluateRepair(request); }
        }, new AsyncTask.Callback<RepairOrderDto>() {
            @Override public void onSuccess(RepairOrderDto value) {
                page.showSuccess("评价已提交。");
                evaluationNote.setText("");
                showDetail(value);
                orders.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    // ---------- 新建报修弹窗 ----------

    private void loadRoom() {
        AsyncTask.run(new AsyncTask.Work<DormHomeSummaryDto>() {
            @Override public DormHomeSummaryDto run() throws Exception { return ext.homeSummary(); }
        }, new AsyncTask.Callback<DormHomeSummaryDto>() {
            @Override public void onSuccess(DormHomeSummaryDto value) {
                if (value == null || !value.isResident()) {
                    composeRoom.setText("暂无住宿记录，暂不能提交报修");
                    return;
                }
                myRoomId = value.getRoomId();
                myRoomLabel = RealUi.text(value.getBuildingName()) + " " + RealUi.text(value.getRoomNo());
                composeRoom.setText("报修房间：" + myRoomLabel);
            }
            @Override public void onFailure(Throwable error) {
                composeRoom.setText("暂无住宿记录，暂不能提交报修");
            }
        });
    }

    /**
     * 新建报修的表单展开在页面正下方，点「新建报修」展开、点「收起」关掉。
     *
     * <p>不常驻是因为一年提交不了几次，却要占掉六个输入框的高度；不用弹窗是因为弹窗
     * 会把上面的工单列表整个盖住——而「我是不是已经报过这一条了」正是该在提交前看一眼
     * 的东西。</p>
     */
    private void buildCompose() {
        composePriority.setFont(DesignTokens.regular(15));
        composeRoom.setText("正在读取你的住宿记录…");

        composeEntry.setFont(DesignTokens.regular(15));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row.setOpaque(false);
        row.add(labelled("问题类别", composeCategory, 180));
        row.add(labelled("优先级", composePriority, 130));
        JPanel entryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        entryRow.setOpaque(false);
        entryRow.add(labelled("你不在时能否进门", composeEntry, 300));

        JPanel note = new JPanel(new BorderLayout(0, 5));
        note.setOpaque(false);
        note.add(DormUi.caption("问题描述"), BorderLayout.NORTH);
        note.add(new javax.swing.JScrollPane(composeDescription), BorderLayout.CENTER);
        note.setPreferredSize(new Dimension(720, 130));
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel buttons = DormFormUi.primarySecondaryActions("提交报修",
                new java.awt.event.ActionListener() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) { create(); }
                }, "收起", new java.awt.event.ActionListener() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCompose(false); }
                });

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(composeRoom);
        rows.add(Box.createVerticalStrut(12));
        rows.add(row);
        rows.add(Box.createVerticalStrut(12));
        rows.add(entryRow);
        rows.add(Box.createVerticalStrut(12));
        rows.add(note);
        rows.add(Box.createVerticalStrut(14));
        rows.add(buttons);
        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);

        compose.add(Box.createVerticalStrut(26));
        compose.add(DormUi.header("新建报修", "填写问题类别、优先级和描述。", null, true));
        compose.add(box);
    }

    private void toggleCompose(boolean show) {
        compose.setVisible(show);
        if (!show) {
            composeCategory.setSelectedItem(RealUi.option("WATER"));
            composeDescription.setText("");
        }
        revalidate();
        repaint();
    }

    private void create() {
        try {
            if (myRoomId <= 0L) throw new IllegalArgumentException("暂无住宿记录，暂不能提交报修");
            final RepairCreateRequest request = new RepairCreateRequest(myRoomId,
                    RealUi.code(composeCategory.getSelectedItem()),
                    RealUi.required(composeDescription.getText(), "问题描述"),
                    RealUi.code(composePriority.getSelectedItem()));
            final boolean allowEnter = composeEntry.getSelectedIndex() == 1;
            AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() {
                @Override public RepairOrderDto run() throws Exception { return service.createRepair(request); }
            }, new AsyncTask.Callback<RepairOrderDto>() {
                @Override public void onSuccess(RepairOrderDto value) {
                    page.showSuccess("报修已提交，工单号 " + value.getId() + "。");
                    toggleCompose(false);
                    orders.reload();
                    if (allowEnter) grantEntry(value.getId());
                    else loadPermits();
                }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
        }
    }

    /**
     * 建单之后补一次入内授权。
     *
     * <p>分两步是因为授权挂在工单号上，而工单号要等服务端建完才有。授权失败不影响
     * 报修本身——单子已经在了，只是维修员得先联系学生，所以这里只提示不报错。</p>
     */
    private void grantEntry(final long orderId) {
        AsyncTask.run(new AsyncTask.Work<RepairEntryPermitDto>() {
            @Override public RepairEntryPermitDto run() throws Exception {
                return ext.setRepairPermit(new RepairEntryPermitRequest(orderId, true, null));
            }
        }, new AsyncTask.Callback<RepairEntryPermitDto>() {
            @Override public void onSuccess(RepairEntryPermitDto value) { loadPermits(); }
            @Override public void onFailure(Throwable error) {
                page.showWarning("工单已提交，但入内授权没设上：" + AsyncTask.message(error));
                loadPermits();
            }
        });
    }

    // ---------- 小工具 ----------

    private static JPanel labelled(String label, Component control, int width) {
        JPanel holder = new JPanel(new BorderLayout(0, 5));
        holder.setOpaque(false);
        holder.add(DormUi.caption(label), BorderLayout.NORTH);
        holder.add(control, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(width, 60));
        return holder;
    }

    private static String repairStatus(String filter) {
        if ("待派单".equals(filter)) return DormRepairStatus.SUBMITTED.name();
        if ("已派单".equals(filter)) return "ACCEPTED";
        if ("处理中".equals(filter)) return DormRepairStatus.IN_PROGRESS.name();
        if ("待宿管审核".equals(filter)) return "PENDING_REVIEW";
        if ("已完成".equals(filter)) return DormRepairStatus.COMPLETED.name();
        if ("已取消".equals(filter)) return DormRepairStatus.CANCELLED.name();
        return null;
    }
}
