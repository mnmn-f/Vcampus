package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
/** 宿舍公告列表；学生只读，宿管可维护类型、范围与置顶。 */
public final class DormExtNoticePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] TYPE_CODES = {NoticeExtraDto.TYPE_GENERAL,
            NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.TYPE_HYGIENE,
            NoticeExtraDto.TYPE_SAFETY, NoticeExtraDto.TYPE_URGENT};
    private static final String[] SCOPE_CODES = {NoticeExtraDto.SCOPE_ALL,
            NoticeExtraDto.SCOPE_BUILDING, NoticeExtraDto.SCOPE_ROOM};
    private final BasePage page;
    private final DormExtClientService service;
    private final boolean manage;
    private final AsyncPagedTable<NoticeExtraDto> notices;
    private final JComboBox<String> noticeType = new JComboBox<String>(typeLabels());
    private final JComboBox<String> scopeType = new JComboBox<String>(scopeLabels());
    private final JComboBox<String> pinned =
            new JComboBox<String>(new String[]{"不置顶", "置顶"});
    private final JTextField buildingId = UiFactory.textField(8);
    private final JTextField roomId = UiFactory.textField(8);
    private final JTextArea preview = UiFactory.textArea(6, 40);
    public DormExtNoticePanel(BasePage page, DormExtClientService service, boolean manage) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.manage = manage;
        noticeType.setFont(DesignTokens.regular(13));
        scopeType.setFont(DesignTokens.regular(13));
        pinned.setFont(DesignTokens.regular(13));
        preview.setEditable(false);
        this.notices = noticeTable();
        add(notices);
        add(manage ? settings() : reader());
    }
    public void reload() { notices.reload(); }
    private AsyncPagedTable<NoticeExtraDto> noticeTable() {
        final String subtitle = manage
                ? "含草稿；置顶公告排在最前，其次按发布时间倒序。"
                : "只显示投放到你所在楼栋或房间的已发布公告，置顶的排在最前。";
        return new AsyncPagedTable<NoticeExtraDto>(manage ? "宿舍公告设置" : "宿舍通知",
                subtitle, "搜索标题或正文",
                manage ? new String[]{"全部状态", "草稿", "已发布", "已过期", "已撤回"}
                        : new String[]{"全部类型", "普通通知", "维修通知", "卫生通知", "安全提醒", "紧急通知"},
                new String[]{"编号", "置顶", "类型", "范围", "标题", "状态", "发布时间", "截止时间"},
                (p, keyword, filter) -> {
                    DormPageQuery query = new DormPageQuery(p, 20, keyword, filterCode(filter), null, null);
                    DormPage<NoticeExtraDto> value = manage ? service.notices(query) : service.myNotices(query);
                    return RealUi.page(value);
                },
                row -> new Object[]{Long.valueOf(row.getAnnouncementId()), row.isPinned() ? "置顶" : "",
                        NoticeExtraDto.typeName(row.getNoticeType()), row.scopeText(), RealUi.text(row.getTitle()),
                        RealUi.status(row.getStatus()), RealUi.dateTime(row.getPublishAt()), RealUi.dateTime(row.getExpireAt())},
                this::select);
    }
    private JPanel settings() {
        SectionCard card = new SectionCard("类型、范围与置顶",
                "只改这三项，不会动公告的标题、正文和发布状态；范围选楼栋或房间时需填对应编号。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("公告类型", noticeType));
        fields.add(UiFactory.labelledField("可见范围", scopeType));
        fields.add(UiFactory.labelledField("楼栋编号（范围=本楼栋时填）", buildingId));
        fields.add(UiFactory.labelledField("房间编号（范围=本房间时填）", roomId));
        fields.add(UiFactory.labelledField("是否置顶", pinned));
        JPanel line = UiFactory.horizontal(8);
        JButton reset = new SecondaryButton("重置为全体");
        reset.addActionListener(e -> resetScope());
        JButton save = new PrimaryButton("保存设置");
        save.addActionListener(e -> save());
        line.add(reset);
        line.add(save);
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
    private JPanel reader() {
        SectionCard card = new SectionCard("公告正文", "在上方列表里选中一条查看全文。");
        card.setContent(new JScrollPane(preview));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }
    private void select(NoticeExtraDto row) {
        if (row == null) return;
        if (!manage) {
            preview.setText(RealUi.text(row.getContent()));
            preview.setCaretPosition(0);
            return;
        }
        noticeType.setSelectedItem(NoticeExtraDto.typeName(row.getNoticeType()));
        scopeType.setSelectedItem(NoticeExtraDto.scopeName(row.getScopeType()));
        pinned.setSelectedItem(row.isPinned() ? "置顶" : "不置顶");
        buildingId.setText(RealUi.input(row.getScopeBuildingId()));
        roomId.setText(RealUi.input(row.getScopeRoomId()));
    }
    private void resetScope() {
        scopeType.setSelectedItem(NoticeExtraDto.scopeName(NoticeExtraDto.SCOPE_ALL));
        buildingId.setText("");
        roomId.setText("");
    }
    private void save() {
        NoticeExtraDto selected = notices.selectedItem();
        if (selected == null) {
            page.showWarning("请先选择一条公告。");
            return;
        }
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
        if (NoticeExtraDto.SCOPE_ALL.equals(scope)) {
            building = null;
            room = null;
        } else if (NoticeExtraDto.SCOPE_BUILDING.equals(scope)) {
            room = null;
        } else {
            building = null;
        }
        final NoticeExtraRequest request = new NoticeExtraRequest(selected.getAnnouncementId(),
                typeCode(String.valueOf(noticeType.getSelectedItem())), scope, building, room,
                "置顶".equals(pinned.getSelectedItem()));
        AsyncTask.run(new AsyncTask.Work<NoticeExtraDto>() {
            @Override public NoticeExtraDto run() throws Exception {
                return service.saveNoticeExtra(request);
            }
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
        for (int i = 0; i < codes.length; i++) labels[i] = type ? NoticeExtraDto.typeName(codes[i]) : NoticeExtraDto.scopeName(codes[i]);
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
