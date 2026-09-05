package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * 宿舍公告：左边一列标题，右边看正文或改投放设置。
 *
 * <p>学生端右侧默认是空的——一进来就把某条公告的正文摊开，等于替人决定先读哪条。
 * 点一条展开，再点同一条收起。宿管端右侧固定是投放设置，因为宿管进这一页就是来
 * 改类型、范围和置顶的。</p>
 *
 * <p>「写公告」用弹窗：公告正文要占好几行，常驻在页面上会把列表挤到只剩几行高，
 * 而写公告一周也不见得有一次。</p>
 */
public final class DormExtNoticePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    // 宿管端右栏是投放设置表单，宽度固定就够。
    private static final int SIDE_WIDTH = 580;
    /**
     * 学生端左侧目录的宽度。
     *
     * <p>学生进这一页只做一件事：读公告。所以左边收成一条固定宽的目录——类型、标题、
     * 日期三列，够认出是哪一条就行——正文占掉剩下的全部宽度。之前是反过来的：一张
     * 铺满左半屏的表配一个 580px 的正文框，结果最该读的那段字被挤在角落里。</p>
     */
    private static final int LIST_WIDTH = 330;
    private static final String[] TYPE_CODES = {NoticeExtraDto.TYPE_GENERAL,
            NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.TYPE_HYGIENE,
            NoticeExtraDto.TYPE_SAFETY, NoticeExtraDto.TYPE_URGENT};
    private static final String[] SCOPE_CODES = {NoticeExtraDto.SCOPE_ALL,
            NoticeExtraDto.SCOPE_BUILDING, NoticeExtraDto.SCOPE_ROOM};

    private final BasePage page;
    private final DormExtClientService service;
    private final DormClientService dorm;
    private final boolean manage;
    private final AsyncPagedTable<NoticeExtraDto> notices;

    private final JComboBox<String> noticeType = new JComboBox<String>(typeLabels());
    private final JComboBox<String> scopeType = new JComboBox<String>(scopeLabels());
    private final JComboBox<String> pinned = new JComboBox<String>(new String[]{"不置顶", "置顶"});
    private final JTextField buildingId = UiFactory.textField(6);
    private final JTextField roomId = UiFactory.textField(6);

    private final JPanel side = new JPanel();
    /** 写公告的表单：就在当前页面正下方展开，不另开窗口。 */
    private final JPanel compose = new JPanel();
    private final JTextField composeTitle = UiFactory.textField(30);
    private final JTextArea composeContent = UiFactory.textArea(6, 46);
    private final JComboBox<String> composeStatus =
            new JComboBox<String>(new String[]{"立即发布", "存为草稿"});
    private final JComboBox<String> composeType = new JComboBox<String>(typeLabels());
    /** 学生端右侧当前展开的是哪一条；再点同一条就收起。 */
    private long openedId;

    public DormExtNoticePanel(BasePage page, DormExtClientService service,
                              DormClientService dorm, boolean manage) {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());
        this.page = page;
        this.service = service;
        this.dorm = dorm;
        this.manage = manage;
        noticeType.setFont(DesignTokens.regular(15));
        scopeType.setFont(DesignTokens.regular(15));
        pinned.setFont(DesignTokens.regular(15));
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        notices = noticeTable();
        if (manage) renderSettings(null); else renderReader(null);
        compose.setOpaque(false);
        compose.setLayout(new BoxLayout(compose, BoxLayout.Y_AXIS));
        compose.setVisible(false);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(manage ? DormUi.split(left(), side, SIDE_WIDTH)
                : DormUi.splitLeading(left(), side, LIST_WIDTH));
        if (manage) {
            buildCompose();
            body.add(compose);
        }
        add(body, BorderLayout.CENTER);
        installToggle();
    }

    public void reload() { notices.reload(); }

    // ---------- 左栏 ----------

    private JPanel left() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        JComponentActions actions = new JComponentActions();
        column.add(DormUi.header(manage ? "宿舍公告" : "宿舍通知",
                manage ? "含草稿；置顶排最前，其次按发布时间倒序。"
                        // 目录只有 330px 宽，一句长说明会被截断，所以拆成两行短句放不下的
                        // 部分留给右侧正文自己说明。
                        : "投放到你的公告，置顶在最前。",
                actions.panel, false));
        column.add(notices);
        return column;
    }

    /** 「写公告」按钮只在宿管端出现，包在一个小类里是为了让上面的组装读起来是一行。 */
    private final class JComponentActions {
        private final JPanel panel;
        JComponentActions() {
            if (!manage) { panel = null; return; }
            JButton create = new PrimaryButton("写公告");
            create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCompose(!compose.isVisible()); }
            });
            panel = DormUi.actions(create);
        }
    }

    private AsyncPagedTable<NoticeExtraDto> noticeTable() {
        // 左栏窄，列就得少：标题是主角，类型和范围各留一列，其余信息放右边详情里。
        String[] columns = manage
                ? new String[]{"置顶", "类型", "标题", "范围", "状态"}
                : new String[]{"类型", "标题", "时间"};
        AsyncPagedTable<NoticeExtraDto> table = new AsyncPagedTable<NoticeExtraDto>("", "", "搜索标题或正文",
                manage ? new String[]{"全部状态", "草稿", "已发布", "已过期", "已撤回"}
                        : new String[]{"全部类型", "普通通知", "维修通知", "卫生通知", "安全提醒", "紧急通知"},
                columns,
                new AsyncPagedTable.Loader<NoticeExtraDto>() {
                    @Override public PageSlice<NoticeExtraDto> load(int p, String keyword, String filter) throws Exception {
                        DormPageQuery query = new DormPageQuery(p, 20, keyword, filterCode(filter), null, null);
                        return RealUi.page(manage ? service.notices(query) : service.myNotices(query));
                    }
                },
                new AsyncPagedTable.RowMapper<NoticeExtraDto>() {
                    @Override public Object[] values(NoticeExtraDto row) {
                        if (manage) {
                            return new Object[]{row.isPinned() ? "置顶" : "",
                                    NoticeExtraDto.typeName(row.getNoticeType()), RealUi.text(row.getTitle()),
                                    row.scopeText(), RealUi.status(row.getStatus())};
                        }
                        // 置顶不再单独占一列：330px 的目录里，一列只为了偶尔显示两个字太奢侈，
                        // 标题前加个记号一样看得出来。
                        return new Object[]{NoticeExtraDto.typeName(row.getNoticeType()),
                                (row.isPinned() ? "★ " : "") + RealUi.text(row.getTitle()),
                                // 只显示日期：列表里精确到分钟没有意义，却要多占一半宽度。
                                RealUi.date(row.getPublishAt() == null ? null : row.getPublishAt().toLocalDate())};
                    }
                },
                new AsyncPagedTable.SelectionListener<NoticeExtraDto>() {
                    @Override public void onSelected(NoticeExtraDto row) { select(row); }
                });
        return table;
    }

    /**
     * 再点一次已展开的公告就收起。
     *
     * <p>选中同一行不会触发选择变化事件，所以收起要自己在鼠标事件里判断——这是
     * 「点开／点收」这个交互唯一能挂的地方。</p>
     */
    private void installToggle() {
        if (manage) return;
        final javax.swing.JTable table = notices.getTable();
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                NoticeExtraDto current = notices.selectedItem();
                if (current != null && current.getAnnouncementId() == openedId
                        && table.getSelectedRow() == row) {
                    table.clearSelection();
                    renderReader(null);
                }
            }
        });
    }

    // ---------- 右栏 ----------

    private void select(NoticeExtraDto row) {
        if (manage) { renderSettings(row); return; }
        renderReader(row);
    }

    /** 学生端右栏：没选中时什么都不显示，只留一句提示。 */
    private void renderReader(NoticeExtraDto row) {
        side.removeAll();
        openedId = row == null ? 0L : row.getAnnouncementId();
        if (row == null) {
            JLabel hint = DormUi.sub("在左侧点一条公告查看正文。");
            hint.setAlignmentX(LEFT_ALIGNMENT);
            side.add(Box.createVerticalStrut(6));
            side.add(hint);
            side.revalidate();
            side.repaint();
            return;
        }
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        title.setOpaque(false);
        JLabel heading = new JLabel(RealUi.text(row.getTitle()));
        heading.setFont(DesignTokens.medium(19));
        heading.setForeground(DesignTokens.TEXT_PRIMARY);
        title.add(heading);
        if (row.isPinned()) title.add(DormUi.badge("置顶", DormUi.Tone.WARN));
        title.setAlignmentX(LEFT_ALIGNMENT);
        side.add(title);
        side.add(Box.createVerticalStrut(7));
        JLabel meta = DormUi.sub(NoticeExtraDto.typeName(row.getNoticeType()) + "　·　投放至 "
                + row.scopeText() + "　·　" + RealUi.dateTime(row.getPublishAt()) + " 发布"
                + (row.getExpireAt() == null ? "" : "　·　" + RealUi.dateTime(row.getExpireAt()) + " 过期"));
        meta.setAlignmentX(LEFT_ALIGNMENT);
        side.add(meta);
        side.add(Box.createVerticalStrut(14));
        JComponent line = DormUi.rule();
        line.setAlignmentX(LEFT_ALIGNMENT);
        side.add(line);
        side.add(Box.createVerticalStrut(16));

        JTextArea body = UiFactory.textArea(10, 28);
        body.setText(RealUi.text(row.getContent()));
        body.setEditable(false);
        body.setFont(DesignTokens.regular(15));
        body.setBackground(java.awt.Color.WHITE);
        body.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        // 正文占满剩下的宽度，高度给足一屏：公告本来就是拿来读的，不是拿来预览的。
        scroll.setPreferredSize(new Dimension(720, 520));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
        side.add(scroll);
        DormUi.alignLeft(side);
        side.revalidate();
        side.repaint();
    }

    /** 宿管端右栏：固定的投放设置表单，选中公告后回填。 */
    private void renderSettings(NoticeExtraDto row) {
        side.removeAll();
        side.add(DormUi.header("类型、范围与置顶",
                "只改这三项，不会动标题、正文和发布状态；范围选楼栋或房间时要填对应编号。", null, false));
        if (row != null) {
            noticeType.setSelectedItem(NoticeExtraDto.typeName(row.getNoticeType()));
            scopeType.setSelectedItem(NoticeExtraDto.scopeName(row.getScopeType()));
            pinned.setSelectedItem(row.isPinned() ? "置顶" : "不置顶");
            buildingId.setText(RealUi.input(row.getScopeBuildingId()));
            roomId.setText(RealUi.input(row.getScopeRoomId()));
        }
        JLabel target = DormUi.sub(row == null ? "先在左侧选中一条公告。"
                : "当前公告：" + RealUi.text(row.getTitle()));
        target.setAlignmentX(LEFT_ALIGNMENT);
        side.add(target);
        side.add(Box.createVerticalStrut(12));

        JPanel first = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        first.setOpaque(false);
        first.add(field("公告类型", noticeType, 176));
        first.add(field("可见范围", scopeType, 176));
        JPanel second = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        second.setOpaque(false);
        second.add(field("楼栋编号", buildingId, 110));
        second.add(field("房间编号", roomId, 110));
        second.add(field("是否置顶", pinned, 120));

        JButton save = new PrimaryButton("保存设置");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        });
        JButton reset = new SecondaryButton("重置为全体");
        reset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { resetScope(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(save);
        buttons.add(reset);

        JPanel box = DormUi.panel();
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(first);
        rows.add(Box.createVerticalStrut(10));
        rows.add(second);
        rows.add(Box.createVerticalStrut(14));
        rows.add(buttons);
        box.add(rows, BorderLayout.CENTER);
        box.setAlignmentX(LEFT_ALIGNMENT);
        side.add(box);
        DormUi.alignLeft(side);
        side.revalidate();
        side.repaint();
    }

    private static JPanel field(String label, Component control, int width) {
        JPanel holder = new JPanel(new BorderLayout(0, 5));
        holder.setOpaque(false);
        holder.add(DormUi.caption(label), BorderLayout.NORTH);
        holder.add(control, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(width, 60));
        return holder;
    }

    // ---------- 写公告 ----------

    /**
     * 写公告的表单直接长在页面里，点「写公告」展开、再点收起。
     *
     * <p>不用弹窗：弹窗把人从列表里拽出来，写的时候看不到已经发过哪些公告，很容易
     * 发重。展开在正下方，上面的列表还在，写完一眼就能看到它出现在里面。</p>
     */
    private void buildCompose() {
        composeStatus.setFont(DesignTokens.regular(15));
        composeType.setFont(DesignTokens.regular(15));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row.setOpaque(false);
        row.add(field("公告类型", composeType, 176));
        row.add(field("发布方式", composeStatus, 150));

        JPanel titleRow = new JPanel(new BorderLayout(0, 5));
        titleRow.setOpaque(false);
        titleRow.add(DormUi.caption("标题"), BorderLayout.NORTH);
        titleRow.add(composeTitle, BorderLayout.CENTER);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JPanel bodyRow = new JPanel(new BorderLayout(0, 5));
        bodyRow.setOpaque(false);
        bodyRow.add(DormUi.caption("正文"), BorderLayout.NORTH);
        bodyRow.add(new JScrollPane(composeContent), BorderLayout.CENTER);
        bodyRow.setPreferredSize(new Dimension(760, 170));
        bodyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        JButton submit = new PrimaryButton("保存公告");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submitCompose(); }
        });
        JButton cancel = new SecondaryButton("收起");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCompose(false); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(submit);
        buttons.add(cancel);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(row);
        rows.add(Box.createVerticalStrut(12));
        rows.add(titleRow);
        rows.add(Box.createVerticalStrut(12));
        rows.add(bodyRow);
        rows.add(Box.createVerticalStrut(14));
        rows.add(buttons);
        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);

        compose.add(Box.createVerticalStrut(26));
        compose.add(DormUi.header("写公告",
                "投放范围和置顶在公告建好后，于右侧「类型、范围与置顶」里设置。", null, true));
        compose.add(box);
    }

    private void toggleCompose(boolean show) {
        compose.setVisible(show);
        if (!show) {
            composeTitle.setText("");
            composeContent.setText("");
        }
        revalidate();
        repaint();
    }

    private void submitCompose() {
        final String title;
        final String content;
        try {
            title = RealUi.required(composeTitle.getText(), "标题");
            content = RealUi.required(composeContent.getText(), "正文");
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        final String status = "存为草稿".equals(composeStatus.getSelectedItem()) ? "DRAFT" : "PUBLISHED";
        final String type = typeCode(String.valueOf(composeType.getSelectedItem()));
        AsyncTask.run(new AsyncTask.Work<DormAnnouncementDto>() {
            @Override public DormAnnouncementDto run() throws Exception {
                return dorm.saveAnnouncement(new AnnouncementSaveRequest(title, content, status));
            }
        }, new AsyncTask.Callback<DormAnnouncementDto>() {
            @Override public void onSuccess(DormAnnouncementDto value) {
                toggleCompose(false);
                applyType(value.getId(), type, title);
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    /** 公告建好后补一次类型；范围默认全体，宿管可以在右侧再改。 */
    private void applyType(final long announcementId, final String type, final String title) {
        AsyncTask.run(new AsyncTask.Work<NoticeExtraDto>() {
            @Override public NoticeExtraDto run() throws Exception {
                return service.saveNoticeExtra(new NoticeExtraRequest(announcementId, type,
                        NoticeExtraDto.SCOPE_ALL, null, null, false));
            }
        }, new AsyncTask.Callback<NoticeExtraDto>() {
            @Override public void onSuccess(NoticeExtraDto value) {
                page.showSuccess("公告「" + title + "」已保存，投放范围为全体。");
                notices.reload();
            }
            @Override public void onFailure(Throwable error) {
                page.showWarning("公告已保存，但类型没设上：" + AsyncTask.message(error));
                notices.reload();
            }
        });
    }

    // ---------- 保存投放设置 ----------

    private void resetScope() {
        scopeType.setSelectedItem(NoticeExtraDto.scopeName(NoticeExtraDto.SCOPE_ALL));
        buildingId.setText("");
        roomId.setText("");
    }

    private void save() {
        NoticeExtraDto selected = notices.selectedItem();
        if (selected == null) { page.showWarning("请先选择一条公告。"); return; }
        String scope = scopeCode(String.valueOf(scopeType.getSelectedItem()));
        Long building = RealUi.number(buildingId.getText());
        Long room = RealUi.number(roomId.getText());
        if (NoticeExtraDto.SCOPE_BUILDING.equals(scope) && building == null) {
            page.showWarning("按楼栋投放时请填写楼栋编号。");
            return;
        }
        if (NoticeExtraDto.SCOPE_ROOM.equals(scope) && room == null) {
            page.showWarning("按房间投放时请填写房间编号。");
            return;
        }
        if (NoticeExtraDto.SCOPE_ALL.equals(scope)) { building = null; room = null; }
        else if (NoticeExtraDto.SCOPE_BUILDING.equals(scope)) { room = null; }
        else { building = null; }
        final NoticeExtraRequest request = new NoticeExtraRequest(selected.getAnnouncementId(),
                typeCode(String.valueOf(noticeType.getSelectedItem())), scope, building, room,
                "置顶".equals(pinned.getSelectedItem()));
        AsyncTask.run(new AsyncTask.Work<NoticeExtraDto>() {
            @Override public NoticeExtraDto run() throws Exception { return service.saveNoticeExtra(request); }
        }, new AsyncTask.Callback<NoticeExtraDto>() {
            @Override public void onSuccess(NoticeExtraDto value) {
                page.showSuccess("公告「" + RealUi.text(value.getTitle()) + "」设置已保存。");
                notices.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    /** 宿管按状态筛选，学生按公告类型筛选。 */
    private String filterCode(String filter) {
        if (manage) {
            if ("草稿".equals(filter)) return "DRAFT";
            if ("已发布".equals(filter)) return "PUBLISHED";
            if ("已过期".equals(filter)) return "EXPIRED";
            if ("已撤回".equals(filter)) return "REVOKED";
            return null;
        }
        return typeCode(filter);
    }

    private static String[] typeLabels() { return names(TYPE_CODES, true); }
    private static String[] scopeLabels() { return names(SCOPE_CODES, false); }

    private static String[] names(String[] codes, boolean type) {
        String[] labels = new String[codes.length];
        for (int i = 0; i < codes.length; i++) {
            labels[i] = type ? NoticeExtraDto.typeName(codes[i]) : NoticeExtraDto.scopeName(codes[i]);
        }
        return labels;
    }

    private static String typeCode(String label) {
        for (String code : TYPE_CODES) {
            if (NoticeExtraDto.typeName(code).equals(label)) return code;
        }
        return null;
    }

    private static String scopeCode(String label) {
        for (String code : SCOPE_CODES) {
            if (NoticeExtraDto.scopeName(code).equals(label)) return code;
        }
        return NoticeExtraDto.SCOPE_ALL;
    }
}
