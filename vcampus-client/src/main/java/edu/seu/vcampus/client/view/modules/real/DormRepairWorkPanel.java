package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkRequest;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 维修员工作台的一页。
 *
 * <p>四个标签（待接工单 / 我的工单 / 入内授权 / 处理记录）是同一张表配不同的数据源、
 * 列和按钮，所以做成一个可配置的面板而不是四个各写一遍的类——四份几乎相同的代码，
 * 改一次列宽要改四处，迟早会长歪。</p>
 *
 * <p>排版沿用宿舍模块的那套：不铺白卡片，标题下面一条分隔线，表格直接坐在页面
 * 底色上。维修员端和学生端、宿管端是同一个系统的三个入口，不该长成三个样子。</p>
 */
public final class DormRepairWorkPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /**
     * 这一页显示哪一批工单。
     *
     * <p>{@code QUEUE}（待接工单）保留成一个取值，但维修员端已经不再挂这一页：派单权
     * 归宿管，他知道谁手头压了几单、哪一单更急。留着是因为服务端的 {@code repairQueue}
     * 查询还在，宿管排查「有没有单子没派出去」时可能还用得上。</p>
     */
    public enum View { QUEUE, ASSIGNED, HISTORY }

    private final BasePage page;
    private final DormExtClientService service;
    private final View view;
    private final AsyncPagedTable<RepairWorkOrderDto> table;
    private final JLabel detail = DormUi.sub("在上表选中一张工单，这里显示详情。");
    private final JPanel detailBox = DormUi.panel();

    public DormRepairWorkPanel(BasePage page, DormExtClientService service, View view) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.view = view;
        table = buildTable();
        addActions();
        add(table);
        add(Box.createVerticalStrut(16));
        add(detailPanel());
    }

    public void reload() {
        table.reload();
    }

    // ---------- 表格 ----------

    private AsyncPagedTable<RepairWorkOrderDto> buildTable() {
        return new AsyncPagedTable<RepairWorkOrderDto>(title(), subtitle(), "按房间号、类别或描述搜索",
                filters(), columns(),
                new AsyncPagedTable.Loader<RepairWorkOrderDto>() {
                    @Override public PageSlice<RepairWorkOrderDto> load(int p, String keyword, String filter) throws Exception {
                        DormPageQuery query = new DormPageQuery(p, 20, keyword, statusOf(filter), null, null);
                        return RealUi.page(fetch(query));
                    }
                },
                new AsyncPagedTable.RowMapper<RepairWorkOrderDto>() {
                    @Override public Object[] values(RepairWorkOrderDto row) { return row(row); }
                },
                new AsyncPagedTable.SelectionListener<RepairWorkOrderDto>() {
                    @Override public void onSelected(RepairWorkOrderDto row) { showDetail(row); }
                });
    }

    private DormPage<RepairWorkOrderDto> fetch(DormPageQuery query) throws Exception {
        if (view == View.QUEUE) return service.repairQueue(query);
        if (view == View.ASSIGNED) return service.repairAssigned(query);
        return service.repairHistory(query);
    }

    private String title() {
        if (view == View.QUEUE) return "待接工单";
        if (view == View.ASSIGNED) return "我的工单";
        return "处理记录";
    }

    private String subtitle() {
        if (view == View.QUEUE) return "还没有人接的报修单，急件排在前面。接单后才显示房间联系电话。";
        if (view == View.ASSIGNED) return "已经接到手上的工单。到场后点「开始处理」，修完点「完工」。";
        return "已报完工待宿管确认、已结束或已取消的工单，含学生评价。";
    }

    private String[] filters() {
        if (view == View.ASSIGNED) return new String[]{"全部状态", "已派单", "处理中"};
        if (view == View.HISTORY) return new String[]{"全部状态", "待宿管审核", "已完成", "已取消"};
        return null;
    }

    private String[] columns() {
        if (view == View.HISTORY) {
            return new String[]{"工单", "位置", "类别", "状态", "完工时间", "评价"};
        }
        if (view == View.ASSIGNED) {
            return new String[]{"工单", "位置", "类别", "紧急度", "状态", "能否入内", "联系电话", "接单时间"};
        }
        return new String[]{"工单", "位置", "类别", "紧急度", "状态", "能否入内", "提交时间"};
    }

    private Object[] row(RepairWorkOrderDto value) {
        if (view == View.ASSIGNED) {
            return new Object[]{Long.valueOf(value.getOrderId()), value.location(), RealUi.status(value.getCategory()),
                    RealUi.status(value.getPriority()), RealUi.status(value.getStatus()), entryText(value),
                    value.getContactPhone() == null ? "—" : value.getContactPhone(),
                    RealUi.dateTime(value.getAcceptedAt())};
        }
        if (view == View.HISTORY) {
            return new Object[]{Long.valueOf(value.getOrderId()), value.location(), RealUi.status(value.getCategory()),
                    RealUi.status(value.getStatus()), RealUi.dateTime(value.getCompletedAt()),
                    value.getEvaluationScore() == null ? "未评价" : value.getEvaluationScore() + " 分"};
        }
        return new Object[]{Long.valueOf(value.getOrderId()), value.location(), RealUi.status(value.getCategory()),
                RealUi.status(value.getPriority()), RealUi.status(value.getStatus()), entryText(value),
                RealUi.dateTime(value.getSubmittedAt())};
    }

    private static String entryText(RepairWorkOrderDto value) {
        return value.canEnterUnattended() ? "已授权" : "需联系学生";
    }

    private static String statusOf(String filter) {
        if ("已派单".equals(filter)) return RepairWorkOrderDto.ACCEPTED;
        if ("处理中".equals(filter)) return RepairWorkOrderDto.IN_PROGRESS;
        if ("待宿管审核".equals(filter)) return RepairWorkOrderDto.PENDING_REVIEW;
        if ("已完成".equals(filter)) return RepairWorkOrderDto.COMPLETED;
        if ("已取消".equals(filter)) return RepairWorkOrderDto.CANCELLED;
        return null;
    }

    // ---------- 操作 ----------

    private void addActions() {
        if (view == View.QUEUE) {
            JButton accept = new PrimaryButton("接单");
            accept.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { act(Action.ACCEPT); }
            });
            table.addAction(accept);
            return;
        }
        if (view == View.ASSIGNED) {
            JButton start = new SecondaryButton("开始处理");
            start.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { act(Action.START); }
            });
            JButton finish = new PrimaryButton("报完工");
            finish.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { act(Action.FINISH); }
            });
            table.addAction(start);
            table.addAction(finish);
        }
    }

    private enum Action { ACCEPT, START, FINISH }

    private void act(final Action action) {
        final RepairWorkOrderDto selected = table.selectedItem();
        if (selected == null) { page.showWarning("请先在表中选中一张工单。"); return; }
        if (action == Action.FINISH && !RealUi.confirm(this, "确认报完工？宿管现场确认后这单才算结束。")) return;
        final RepairWorkRequest request = new RepairWorkRequest(selected.getOrderId(), null);
        AsyncTask.run(new AsyncTask.Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run() throws Exception {
                if (action == Action.ACCEPT) return service.acceptRepair(request);
                if (action == Action.START) return service.startRepair(request);
                return service.finishRepair(request);
            }
        }, new AsyncTask.Callback<RepairWorkOrderDto>() {
            @Override public void onSuccess(RepairWorkOrderDto value) {
                page.showSuccess(message(action) + "：" + value.location() + " 工单 " + value.getOrderId());
                showDetail(value);
                table.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String message(Action action) {
        if (action == Action.ACCEPT) return "已接单";
        if (action == Action.START) return "已开始处理";
        return "已报完工，等宿管确认";
    }

    // ---------- 详情 ----------

    /**
     * 工单详情铺在表格正下方，占满整幅宽度。
     *
     * <p>之前挂在右下角，是因为它被当成表格的附属物塞在了行动按钮那一排旁边。
     * 但详情是接下来要读的正文——报修描述可能是一整段话，挤在一个角落里既读不完
     * 也找不到。</p>
     */
    private JPanel detailPanel() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.add(DormUi.header("工单详情", "在上表选中一张工单，这里显示完整描述与入内授权。", null, true));
        detail.setBorder(BorderFactory.createEmptyBorder());
        // 详情就是正文，用正文字号。之前它在 HTML 里又写了一遍 font-size，Swing 的
        // HTML 渲染会在标签字体的基础上再放大一次，结果这一块比页面上任何标题都大。
        detail.setFont(DesignTokens.regular(14));
        detail.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        detailBox.add(detail, BorderLayout.CENTER);
        detailBox.setAlignmentX(LEFT_ALIGNMENT);
        detailBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        section.add(detailBox);
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private void showDetail(RepairWorkOrderDto value) {
        if (value == null) {
            detail.setText("在上表选中一张工单，这里显示完整描述与入内授权。");
            detail.setForeground(DesignTokens.TEXT_SECONDARY);
            return;
        }
        int width = Math.max(560, detailBox.getWidth() - 60);
        // 不再写 font-size：字号交给 detail 这个 JLabel 的字体，HTML 里只管排版。
        StringBuilder text = new StringBuilder("<html><body style='width:" + width + "px'>");
        text.append("<b>").append(escape(value.location())).append("　·　")
                .append(escape(RealUi.status(value.getCategory()))).append("　·　")
                .append(escape(RealUi.status(value.getStatus()))).append("</b>");
        text.append("<br><br>")
                .append(escape(value.getDescription() == null || value.getDescription().trim().isEmpty()
                        ? "（报修人未填写描述）" : value.getDescription().trim()));
        text.append("<br><br>提交时间 ").append(escape(RealUi.dateTime(value.getSubmittedAt())));
        if (value.getAcceptedAt() != null) text.append("　　接单 ").append(escape(RealUi.dateTime(value.getAcceptedAt())));
        if (value.getCompletedAt() != null) text.append("　　完工 ").append(escape(RealUi.dateTime(value.getCompletedAt())));
        text.append("<br><br>入内授权：").append(value.canEnterUnattended()
                ? "<span style='color:#1A8F5A'><b>学生已授权</b></span>，可在其不在宿舍时进入"
                : "<span style='color:#C92A2A'><b>未授权</b></span>，请先联系学生约时间");
        if (value.getEntryNote() != null && !value.getEntryNote().trim().isEmpty()) {
            text.append("　备注：").append(escape(value.getEntryNote().trim()));
        }
        if (value.getContactPhone() != null && !value.getContactPhone().trim().isEmpty()) {
            text.append("<br>联系电话：<b>").append(escape(value.getContactPhone().trim())).append("</b>");
        } else if (view == View.QUEUE) {
            text.append("<br>联系电话：接单后显示");
        }
        text.append("</body></html>");
        detail.setText(text.toString());
        detail.setForeground(DesignTokens.TEXT_PRIMARY);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
