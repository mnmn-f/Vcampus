package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 住宿申请的受理：三等分，从左到右是「有哪些申请」「放到哪间房」「这间房怎么排」。
 *
 * <p>住宿申请和请假、来访不是一类事。后两者点个头就完了，住宿申请点头之后还得真的
 * 把人放到某张床上——而「哪张床还空着」只有看图才知道。所以它从合并的待办表里单独
 * 拎出来：选中申请 → 中栏挑一间房 → 右栏图上点一张空床 → 通过，一步到位。</p>
 *
 * <p>三栏而不是「主内容 + 侧栏」两栏：房间目录和床位平面是两件独立的事，塞进同一条
 * 窄侧栏里就得上下叠，平面图会被挤到屏幕外面。三件事平级，所以三等分。没选中申请时
 * 右边两栏是空的——这时候本来就没有哪间房、哪张床可谈。</p>
 *
 * <p>床位由这里指定，而不是学生提交时自己填：学生看不到哪张房还有空床，让他填编号
 * 只会填错。所以右栏多了一张房间目录——统一调配的前提是宿管当场能翻到房。</p>
 *
 * <p>只有「通过并分配」一个通过按钮。批准和落实拆成两步，会留下一条「已通过但没住
 * 进去」的申请：学生看到批了却仍然没有住处，而系统里没有任何地方提醒宿管还欠一次
 * 分配。两件事合成一次请求，服务端在同一个事务里做完。</p>
 */
public final class DormManagerHousingRequestPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BasePage page;
    private final DormClientService service;
    private final AsyncPagedTable<AccommodationRequestDto> requests;
    /** 房间目录：宿管在这里翻到要放人的那间房，选中后画出它的床位平面。 */
    private final AsyncPagedTable<DormRoomDto> roomPicker;
    private final DormRoomPlan plan = new DormRoomPlan();
    /** 中栏：受理动作。 */
    private final JPanel side = new JPanel();
    /** 右栏：选中房间的床位平面。 */
    private final JPanel planSide = new JPanel();
    private final javax.swing.JLabel planHint = DormUi.sub("先在中间的房间目录里选一间房。");
    private final JTextField bedId = UiFactory.textField(8);
    private final JTextField remark = UiFactory.textField(16);
    private AccommodationRequestDto selected;

    public DormManagerHousingRequestPanel(BasePage page, DormClientService service) {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());
        this.page = page;
        this.service = service;
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        planSide.setOpaque(false);
        planSide.setLayout(new BoxLayout(planSide, BoxLayout.Y_AXIS));
        plan.setListener(new DormRoomPlan.Listener() {
            @Override public void onBedPicked(DormBedDto value) {
                bedId.setText(String.valueOf(value.getId()));
                planHint.setText("已选中 " + RealUiPlanText.bedLabel(value));
            }
        });
        roomPicker = roomTable();
        // 房间目录只在选中非退宿申请时才挂到右栏上，页面搭好时它还没进组件树，
        // RealDormPage 那次 DormUi.flatten 走不到它——所以在这儿自己走一遍，
        // 否则它会顶着一张白卡片出现在一堆扁平内容中间。
        DormUi.flatten(roomPicker);
        requests = table();
        renderSide();
        add(DormUi.thirds(left(), side, planSide), BorderLayout.CENTER);
    }

    public void reload() { requests.reload(); roomPicker.reload(); }

    private JPanel left() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("住宿申请",
                "入住、调宿、退宿。选中一条，中间看理由和现住，右边挑房间、点床位，然后通过。",
                null, false));
        column.add(requests);
        return column;
    }

    /**
     * 左栏的申请表只留四列：学生、类型、状态、提交时间。
     *
     * <p>现住宿舍和申请理由都不放进表里：选中一条之后中栏会整段展示，表里再塞一遍
     * 只会把左栏撑得要左右滚。表格的任务是让宿管认出"哪一条"，不是读内容。</p>
     */
    private AsyncPagedTable<AccommodationRequestDto> table() {
        return requestTable();
    }

    private AsyncPagedTable<AccommodationRequestDto> requestTable() {
        return new AsyncPagedTable<AccommodationRequestDto>("", "", "搜索学生或申请原因",
                new String[]{"待审批", "全部状态", "已通过", "已驳回", "已取消"},
                new String[]{"学生", "类型", "状态", "提交时间"},
                new AsyncPagedTable.Loader<AccommodationRequestDto>() {
                    @Override public PageSlice<AccommodationRequestDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.requests(
                                new DormPageQuery(p, 20, k, statusCode(f), null, null), null));
                    }
                }, new AsyncPagedTable.RowMapper<AccommodationRequestDto>() {
                    @Override public Object[] values(AccommodationRequestDto row) {
                        return new Object[]{row.studentLabel(), RealUi.status(row.getRequestType()),
                                RealUi.status(row.getStatus()), RealUi.dateTime(row.getCreatedAt())};
                    }
                }, new AsyncPagedTable.SelectionListener<AccommodationRequestDto>() {
                    @Override public void onSelected(AccommodationRequestDto row) { select(row); }
                });
    }

    /** 房间目录每页几条：三条一页，目录矮一点，下面的分配表单不用往下翻。 */
    private static final int ROOM_PAGE_ROWS = 3;

    /** 中栏的房间目录：只留挑房时要看的四列，宽度有限，别的信息在「住宿与空间」里看。 */
    private AsyncPagedTable<DormRoomDto> roomTable() {
        AsyncPagedTable<DormRoomDto> table = new AsyncPagedTable<DormRoomDto>("", "", "搜索房间号或楼栋",
                new String[]{"有空床", "全部房间"},
                new String[]{"楼栋", "房间", "容量", "已住"},
                new AsyncPagedTable.Loader<DormRoomDto>() {
                    @Override public PageSlice<DormRoomDto> load(int p, String k, String f) throws Exception {
                        // 「有空床」是在客户端过滤的，三条一页要是直接按页向服务端要，
                        // 一页里的三间房碰巧都住满了就会显示成空页。所以一次把可用房间
                        // （服务端上限 100 条）拿回来，过滤完再在客户端切成三条一页。
                        DormPage<DormRoomDto> value = service.rooms(
                                new DormPageQuery(1, 100, k, "AVAILABLE", null, null));
                        List<DormRoomDto> rows = free(value, !"全部房间".equals(f)).getItems();
                        return PageSlice.filter(rows, null, p, ROOM_PAGE_ROWS,
                                new java.util.function.Function<DormRoomDto, String>() {
                                    @Override public String apply(DormRoomDto room) { return ""; }
                                });
                    }
                }, new AsyncPagedTable.RowMapper<DormRoomDto>() {
                    @Override public Object[] values(DormRoomDto row) {
                        return new Object[]{RealUi.text(row.getBuildingName()), RealUi.text(row.getRoomNo()),
                                Integer.valueOf(row.getCapacity()), Integer.valueOf(row.getOccupiedBeds())};
                    }
                }, new AsyncPagedTable.SelectionListener<DormRoomDto>() {
                    @Override public void onSelected(DormRoomDto row) { loadPlan(row); }
                });
        table.setPageRows(ROOM_PAGE_ROWS);
        return table;
    }

    /** 默认只列还有空床的房间：分配时翻到一间满员的房，除了浪费一次点击没有别的用处。 */
    private static DormPage<DormRoomDto> free(DormPage<DormRoomDto> value, boolean onlyFree) {
        if (value == null) return new DormPage<DormRoomDto>(1, 20, 0, new ArrayList<DormRoomDto>());
        if (!onlyFree) return value;
        List<DormRoomDto> rows = new ArrayList<DormRoomDto>();
        for (DormRoomDto item : value.getItems()) {
            if (item.getOccupiedBeds() < item.getCapacity()) rows.add(item);
        }
        return new DormPage<DormRoomDto>(value.getPageNumber(), value.getPageSize(),
                value.getTotalElements(), rows);
    }

    // ---------- 右栏 ----------

    private void select(AccommodationRequestDto row) {
        selected = row;
        bedId.setText("");
        // 换一条申请就把上一条留下的图清掉，否则退宿那张只读图会一直挂在右边。
        plan.showRoom(row == null ? "未选中申请" : "先在中间的房间目录里选一间房", null);
        planHint.setText("先在中间的房间目录里选一间房。");
        renderSide();
    }

    private void renderSide() {
        side.removeAll();
        planSide.removeAll();
        if (selected == null) {
            side.add(DormUi.header("受理申请", "在左侧选中一条住宿申请。", null, false));
            side.add(DormUi.sub("选中后这里显示房间目录和受理动作，右边显示床位平面。"));
            finishRender();
            return;
        }
        boolean checkout = "CHECK_OUT".equalsIgnoreCase(selected.getRequestType());
        String where = selected.getCurrentLocation() == null || selected.getCurrentLocation().trim().isEmpty()
                ? "尚无住宿记录" : "现住 " + selected.getCurrentLocation().trim();
        side.add(DormUi.header("受理申请 · " + RealUi.status(selected.getRequestType())
                        + " · " + selected.studentLabel(),
                RealUi.status(selected.getStatus()) + "　·　" + where + "　·　提交于 "
                        + RealUi.dateTime(selected.getCreatedAt()),
                null, false));
        // 申请理由整段展示：表里已经不放它了，而理由是宿管判断批不批的主要依据，
        // 所以单独放进一个半透明白底的框里、字号比普通说明大一号，一眼能找到。
        if (selected.getReason() != null && !selected.getReason().trim().isEmpty()) {
            side.add(DormUi.caption("申请理由"));
            side.add(Box.createVerticalStrut(6));
            side.add(DormUi.passage(selected.getReason().trim()));
            side.add(Box.createVerticalStrut(14));
        }
        if (selected.getReviewRemark() != null && !selected.getReviewRemark().trim().isEmpty()) {
            side.add(DormUi.caption("审批意见"));
            side.add(Box.createVerticalStrut(4));
            side.add(DormUi.paragraph(selected.getReviewRemark().trim()));
            side.add(Box.createVerticalStrut(14));
        }
        plan.setInteractive(!checkout);
        if (!checkout) {
            side.add(roomPicker);
            side.add(Box.createVerticalStrut(16));
            planSide.add(DormUi.header("房间平面",
                    "实心为已占用，虚线为空闲。点一张空床，编号会填进中间的表单。", null, false));
            planSide.add(plan);
            planSide.add(Box.createVerticalStrut(6));
            planSide.add(planHint);
        } else {
            side.add(DormUi.sub("退宿不涉及选床，通过后学生现住的床位会自动释放。"));
            side.add(Box.createVerticalStrut(14));
            // 退宿也把平面图摆出来，但只看不点：宿管批之前想知道的是「这个人睡在哪张床、
            // 同屋还有谁」，图比「D2 102-1床」这串字直观。床位从在住关系里反查。
            planSide.add(DormUi.header("房间平面",
                    "学生现住的房间；金色描边是将被释放的床位，仅供查看。", null, false));
            planSide.add(plan);
            planSide.add(Box.createVerticalStrut(6));
            planSide.add(planHint);
            loadCurrentBedPlan(selected);
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        if (!checkout) row.add(field("目标床位编号（点图自动填）", bedId, 190));
        row.add(field("审批备注", remark, checkout ? 380 : 190));

        boolean pending = "PENDING".equalsIgnoreCase(selected.getStatus());
        JButton approve = new PrimaryButton(checkout ? "通过退宿" : "通过并分配");
        approve.setEnabled(pending);
        approve.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { approve(); }
        });
        JButton reject = new DangerButton("驳回");
        reject.setEnabled(pending);
        reject.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reject(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(approve);
        buttons.add(reject);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(row);
        rows.add(Box.createVerticalStrut(14));
        rows.add(buttons);
        if (!pending) {
            rows.add(Box.createVerticalStrut(10));
            rows.add(DormUi.sub("这条申请已经处理过，按钮不可用。"));
        }
        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);
        side.add(box);
        finishRender();
    }

    /** 两栏都是选中之后现搭的，收尾动作一样：左对齐再重排。 */
    private void finishRender() {
        DormUi.alignLeft(side);
        DormUi.alignLeft(planSide);
        side.revalidate();
        side.repaint();
        planSide.revalidate();
        planSide.repaint();
    }

    private static JPanel field(String label, Component control, int width) {
        JPanel holder = new JPanel(new BorderLayout(0, 5));
        holder.setOpaque(false);
        holder.add(DormUi.caption(label), BorderLayout.NORTH);
        holder.add(control, BorderLayout.CENTER);
        holder.setPreferredSize(new Dimension(width, 60));
        return holder;
    }

    /** 画出这间房的床位。房间号在不同楼栋会重号，所以搜到之后还要按 roomId 过滤。 */
    private void loadPlan(final DormRoomDto room) {
        if (room == null) {
            plan.showRoom("未选中房间", null);
            planHint.setText("先在中间的房间目录里选一间房。");
            return;
        }
        final String caption = RealUi.text(room.getBuildingName()) + " " + RealUi.text(room.getRoomNo());
        planHint.setText("容量 " + room.getCapacity() + " 张　已住 " + room.getOccupiedBeds()
                + " 人　空 " + Math.max(0, room.getCapacity() - room.getOccupiedBeds()) + " 张；点一张空床选中它。");
        AsyncTask.run(new AsyncTask.Work<List<DormBedDto>>() {
            @Override public List<DormBedDto> run() throws Exception { return bedsOf(room.getId()); }
        }, new AsyncTask.Callback<List<DormBedDto>>() {
            @Override public void onSuccess(List<DormBedDto> value) { plan.showRoom(caption, value); }
            @Override public void onFailure(Throwable error) { plan.showRoom(caption + "（床位读取失败）", null); }
        });
    }

    /**
     * 这间房的全部床位。
     *
     * <p>之前是拿房间号当关键字去搜再按 roomId 过滤，但床位查询根本不认关键字——
     * 服务端只按 roomId 和状态筛——于是每次拿到的都是全库前 50 张床，排在后面的房间
     * 一张都分不到，平面图就空着。直接按 roomId 查才是对的。</p>
     */
    private List<DormBedDto> bedsOf(long roomId) throws Exception {
        DormPage<DormBedDto> value = service.beds(
                new DormPageQuery(1, 50, null, null, null, Long.valueOf(roomId)));
        List<DormBedDto> mine = new ArrayList<DormBedDto>();
        if (value != null && value.getItems() != null) {
            for (DormBedDto item : value.getItems()) {
                if (item.getRoomId() == roomId) mine.add(item);
            }
        }
        return mine;
    }

    /**
     * 退宿受理：反查学生现住的床，把那间房画出来并标出这张床。
     *
     * <p>申请记录里只有一串「D2 102-1床」的文字，没有床位编号，所以从已占用的床位里
     * 按住户找：宿管有 DORM_MANAGE，床位列表会带住户编号。演示库的在住床位只有几十张，
     * 按 100 一页翻几页就够了；找不到就只留一句提示，不影响审批。</p>
     */
    private void loadCurrentBedPlan(final AccommodationRequestDto request) {
        final long requestId = request.getId();
        final long studentId = request.getStudentUserId();
        planHint.setText("正在查找学生现住的床位…");
        AsyncTask.run(new AsyncTask.Work<Object[]>() {
            @Override public Object[] run() throws Exception {
                DormBedDto current = null;
                for (int p = 1; p <= 20 && current == null; p++) {
                    DormPage<DormBedDto> value = service.beds(
                            new DormPageQuery(p, 100, null, "OCCUPIED", null, null));
                    if (value == null || value.getItems() == null || value.getItems().isEmpty()) break;
                    for (DormBedDto item : value.getItems()) {
                        if (item.getOccupantUserId() != null && item.getOccupantUserId().longValue() == studentId) {
                            current = item;
                            break;
                        }
                    }
                    if (value.getItems().size() < 100) break;
                }
                if (current == null) return null;
                return new Object[]{current, bedsOf(current.getRoomId())};
            }
        }, new AsyncTask.Callback<Object[]>() {
            @Override public void onSuccess(Object[] value) {
                // 查的过程中宿管可能已经点了别的申请，那就别把旧结果画上去。
                if (selected == null || selected.getId() != requestId) return;
                if (value == null) {
                    plan.showRoom("未找到该学生的在住床位", null);
                    planHint.setText("没有查到这名学生的在住床位，可能已经退宿。");
                    return;
                }
                DormBedDto current = (DormBedDto) value[0];
                @SuppressWarnings("unchecked")
                List<DormBedDto> beds = (List<DormBedDto>) value[1];
                String where = request.getCurrentLocation() == null ? "" : request.getCurrentLocation().trim();
                int cut = where.lastIndexOf('-');
                String caption = cut > 0 ? where.substring(0, cut)
                        : RealUi.text(current.getBuildingCode()) + " " + RealUi.text(current.getRoomNo());
                plan.showRoom(caption, beds, Long.valueOf(current.getId()));
                int occupied = 0;
                for (DormBedDto bed : beds) if ("OCCUPIED".equals(bed.getStatus())) occupied++;
                planHint.setText("学生现住 " + RealUi.text(current.getBedNo()) + " 号床，同屋共 " + occupied
                        + " 人在住；通过后该床位释放。");
            }
            @Override public void onFailure(Throwable error) {
                if (selected == null || selected.getId() != requestId) return;
                plan.showRoom("床位读取失败", null);
                planHint.setText(AsyncTask.message(error));
            }
        });
    }

    // ---------- 动作 ----------

    private void approve() {
        if (selected == null) return;
        final long requestId = selected.getId();
        final boolean checkout = "CHECK_OUT".equalsIgnoreCase(selected.getRequestType());
        final Long bed = checkout ? null : RealUi.number(bedId.getText());
        if (!checkout && bed == null) {
            page.showWarning("先给这条申请挑一张床：在中间的房间目录里选一间房，再在右边的平面图上点一张空床。");
            return;
        }
        final String note = RealUi.optional(remark.getText());
        AsyncTask.run(new AsyncTask.Work<AccommodationRequestDto>() {
            @Override public AccommodationRequestDto run() throws Exception {
                // 通过和落实在服务端的同一个事务里做完：分开发两次请求的话，第二次失败
                // 就会留下一条「已通过但没住进去」的申请，而界面上看不出它还缺一步。
                return service.approveRequest(new DormApprovalRequest(requestId, true, note, bed));
            }
        }, new AsyncTask.Callback<AccommodationRequestDto>() {
            @Override public void onSuccess(AccommodationRequestDto value) {
                page.showSuccess(checkout ? "退宿申请已通过，床位已释放。" : "申请已通过，住宿关系已更新。");
                remark.setText("");
                bedId.setText("");
                requests.reload();
                roomPicker.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void reject() {
        if (selected == null) return;
        if (!RealUi.confirm(this, "确认驳回这条住宿申请？")) return;
        final long requestId = selected.getId();
        final String note = RealUi.optional(remark.getText());
        AsyncTask.run(new AsyncTask.Work<AccommodationRequestDto>() {
            @Override public AccommodationRequestDto run() throws Exception {
                return service.approveRequest(new DormApprovalRequest(requestId, false, note));
            }
        }, new AsyncTask.Callback<AccommodationRequestDto>() {
            @Override public void onSuccess(AccommodationRequestDto value) {
                page.showSuccess("申请已驳回。");
                remark.setText("");
                requests.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String statusCode(String filter) {
        if ("全部状态".equals(filter)) return null;
        if ("已通过".equals(filter)) return "APPROVED";
        if ("已驳回".equals(filter)) return "REJECTED";
        if ("已取消".equals(filter)) return "CANCELLED";
        return "PENDING";
    }
}
