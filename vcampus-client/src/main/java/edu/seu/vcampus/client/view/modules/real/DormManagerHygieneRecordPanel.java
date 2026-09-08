package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * 历史卫生检查记录。
 *
 * <p>和上面的「检查任务 + 分项打分」分工是时间上的：那边是还没做的活——本周该查哪些
 * 房间，选中一间当场按五项打分，总分、等级、要不要整改、什么时候复查全由服务端算；
 * 这边是已经做完的账，按房间和状态回看。</p>
 *
 * <p>这里只读。原来右边还挂着一个「修订记录」表单，可以直接改总分和整改状态——那等于
 * 在五项分制旁边开了一个能随手覆盖它的后门：改完的总分和分项分对不上，谁也说不清哪个
 * 才算数。整改状态该由复查来推进：复查任务查完、分数够了，服务端自己会把它标成已整改。</p>
 */
public final class DormManagerHygieneRecordPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DormClientService service;
    private final AsyncPagedTable<HygieneInspectionDto> records;

    public DormManagerHygieneRecordPanel(BasePage page, DormClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.service = service;
        records = table();
        add(records);
    }

    public void reload() { records.reload(); }

    private AsyncPagedTable<HygieneInspectionDto> table() {
        return new AsyncPagedTable<HygieneInspectionDto>("检查记录",
                "所有房间的历史检查与整改状态，只读；要改状态请排一次复查。",
                "搜索房间或检查结果",
                new String[]{"全部状态", "正常", "待整改", "已整改"},
                new String[]{"编号", "房间", "评分", "结果", "问题", "状态", "检查时间"},
                new AsyncPagedTable.Loader<HygieneInspectionDto>() {
                    @Override public PageSlice<HygieneInspectionDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.hygiene(new DormPageQuery(p, 20, k, statusCode(f), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<HygieneInspectionDto>() {
                    @Override public Object[] values(HygieneInspectionDto row) {
                        return new Object[]{Long.valueOf(row.getId()), Long.valueOf(row.getRoomId()),
                                RealUi.text(row.getScore()), RealUi.status(row.getResult()),
                                RealUi.text(row.getIssueDescription()), RealUi.status(row.getStatus()),
                                RealUi.dateTime(row.getInspectedAt())};
                    }
                }, null);
    }

    private static String statusCode(String filter) {
        if ("正常".equals(filter)) return "NORMAL";
        if ("待整改".equals(filter)) return "RECTIFICATION_REQUIRED";
        if ("已整改".equals(filter)) return "RECTIFIED";
        return null;
    }
}
