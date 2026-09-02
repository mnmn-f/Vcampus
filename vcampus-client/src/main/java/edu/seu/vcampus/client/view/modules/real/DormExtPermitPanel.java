package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 学生端维修入内授权。
 *
 * <p>报修单本身仍由既有的「我的报修」页面提交，这里只管一件事：本人不在宿舍时
 * 是否允许维修人员进门。授权只能对自己提交的工单下达，服务端会二次校验提交人。</p>
 */
public final class DormExtPermitPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<RepairEntryPermitDto> permits;
    private final JTextField note = UiFactory.textField(24);
    private final JLabel contact = UiFactory.muted("选中一条工单后显示你的联系电话。");

    public DormExtPermitPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.permits = permitTable();
        add(permits);
        add(actions());
    }

    public void reload() { permits.reload(); }

    private AsyncPagedTable<RepairEntryPermitDto> permitTable() {
        return new AsyncPagedTable<RepairEntryPermitDto>("我的报修工单",
                "选中一条工单后在下方授权或撤销；未授权时维修人员需与你另行约时间。"
                        + "「联系电话」取自你的账号资料，改了资料这里会跟着变。",
                "搜索类别或房间",
                new String[]{"全部工单", "已授权", "未授权"},
                new String[]{"工单号", "楼栋", "房间", "类别", "工单状态", "提交时间",
                        "入内授权", "联系电话", "备注"},
                new AsyncPagedTable.Loader<RepairEntryPermitDto>() {
                    @Override
                    public PageSlice<RepairEntryPermitDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<RepairEntryPermitDto> value = service.myRepairPermits(
                                new DormPageQuery(p, 20, keyword, null, null, null));
                        return RealUi.page(filtered(value, filter));
                    }
                },
                new AsyncPagedTable.RowMapper<RepairEntryPermitDto>() {
                    @Override
                    public Object[] values(RepairEntryPermitDto row) {
                        return new Object[]{Long.valueOf(row.getRepairOrderId()),
                                RealUi.text(row.getBuildingCode()), RealUi.text(row.getRoomNo()),
                                RealUi.text(row.getCategory()), RealUi.status(row.getOrderStatus()),
                                RealUi.dateTime(row.getSubmittedAt()),
                                row.isAllowEnter() ? "已授权" : "未授权",
                                phoneText(row.getContactPhone()),
                                RealUi.text(row.getNote())};
                    }
                },
                new AsyncPagedTable.SelectionListener<RepairEntryPermitDto>() {
                    @Override public void onSelected(RepairEntryPermitDto row) { select(row); }
                });
    }

    private JPanel actions() {
        SectionCard card = new SectionCard("入内授权",
                "授权后维修人员可在你不在时进入宿舍作业。"
                        + "账号里没登记手机号的，必须在备注里留下能联系到你的方式。");
        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("备注"));
        line.add(note);
        JButton allow = new PrimaryButton("允许入内");
        allow.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit(true); }
        });
        JButton deny = new SecondaryButton("撤销授权");
        deny.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit(false); }
        });
        line.add(allow);
        line.add(deny);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(line, BorderLayout.CENTER);
        content.add(contact, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    /** 选中工单时把该生的联系电话回显出来，让学生知道维修人员拿到的是哪个号码。 */
    private void select(RepairEntryPermitDto row) {
        if (row == null) return;
        note.setText(RealUi.input(row.getNote()));
        contact.setText(hasPhone(row)
                ? "维修人员会看到你账号里的手机号 " + row.getContactPhone()
                : "你的账号里没有登记手机号，授权前请在备注里留下联系方式");
    }

    private void submit(final boolean allowEnter) {
        RepairEntryPermitDto selected = permits.selectedItem();
        if (selected == null) {
            page.showWarning("请先选择一条报修工单。");
            return;
        }
        // 服务端同样会拦一次；这里先拦是为了不让人白跑一趟网络，也把原因说在当场。
        if (allowEnter && !hasPhone(selected) && RealUi.optional(note.getText()) == null) {
            page.showWarning("你的账号里没有登记手机号。"
                    + "请先在「个人中心」补上，或在备注里留下维修期间能联系到你的方式。");
            return;
        }
        final RepairEntryPermitRequest request = new RepairEntryPermitRequest(
                selected.getRepairOrderId(), allowEnter, RealUi.optional(note.getText()));
        AsyncTask.run(new AsyncTask.Work<RepairEntryPermitDto>() {
            @Override public RepairEntryPermitDto run() throws Exception {
                return service.setRepairPermit(request);
            }
        }, new AsyncTask.Callback<RepairEntryPermitDto>() {
            @Override public void onSuccess(RepairEntryPermitDto value) {
                page.showSuccess(allowEnter ? "已允许维修人员入内。" : "已撤销入内授权。");
                note.setText("");
                permits.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static boolean hasPhone(RepairEntryPermitDto row) {
        return row.getContactPhone() != null && !row.getContactPhone().trim().isEmpty();
    }

    private static String phoneText(String phone) {
        return phone == null || phone.trim().isEmpty() ? "未登记" : phone;
    }

    /**
     * 「已授权／未授权」按客户端过滤。
     *
     * <p>服务端的分页查询没有这个条件，与其为了一个下拉去改查询接口，不如在当前页
     * 上筛——代价是筛完的这一页可能不满 20 行，但翻页和总数仍由服务端给出，语义清晰。</p>
     */
    private static DormPage<RepairEntryPermitDto> filtered(DormPage<RepairEntryPermitDto> value,
                                                           String filter) {
        Boolean want = null;
        if ("已授权".equals(filter)) want = Boolean.TRUE;
        if ("未授权".equals(filter)) want = Boolean.FALSE;
        if (want == null) return value;
        java.util.List<RepairEntryPermitDto> rows = new java.util.ArrayList<RepairEntryPermitDto>();
        for (RepairEntryPermitDto item : value.getItems()) {
            if (item.isAllowEnter() == want.booleanValue()) rows.add(item);
        }
        return new DormPage<RepairEntryPermitDto>(value.getPageNumber(), value.getPageSize(),
                value.getTotalElements(), rows);
    }
}
