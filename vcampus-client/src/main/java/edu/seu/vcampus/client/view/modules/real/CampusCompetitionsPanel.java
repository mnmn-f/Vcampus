package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

/** 比赛列表、学生报名/取消及教务老师维护报名名单。 */
public final class CampusCompetitionsPanel extends JPanel {
    private final BasePage page; private final CampusClientService service; private final Role role;
    private final AsyncPagedTable<CompetitionDto> table; private final CompetitionEditorPanel editor;
    private final JLabel detail = UiFactory.muted("选择比赛查看详情。");
    private final JTextArea roster = UiFactory.textArea(4, 60);

    public CampusCompetitionsPanel(BasePage page, CampusClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        editor = role == Role.ACADEMIC_ADMIN ? new CompetitionEditorPanel(new CompetitionEditorPanel.Listener() {
            @Override public void onSave(CompetitionSaveRequest request) { save(request); }
        }) : null;
        table = createTable(); add(table); if (editor != null) add(editor); add(detailPanel());
    }

    private AsyncPagedTable<CompetitionDto> createTable() {
        AsyncPagedTable<CompetitionDto> value = new AsyncPagedTable<CompetitionDto>("校园比赛",
                "学生报名或取消报名，教务老师负责发布活动和查看名单。", "搜索比赛名称",
                new String[]{"全部状态", "已发布", "已截止", "已取消"},
                new String[]{"比赛名称", "时间", "报名截止", "人数", "状态"},
                new AsyncPagedTable.Loader<CompetitionDto>() {
                    @Override public PageSlice<CompetitionDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(service.competitions(new CampusCompetitionQuery(
                                new CampusPageQuery(p, 20, keyword, status(filter)))));
                    }
                }, new AsyncPagedTable.RowMapper<CompetitionDto>() {
                    @Override public Object[] values(CompetitionDto row) { return new Object[]{row.getTitle(), RealUi.dateTime(row.getStartAt()) + " - " + RealUi.dateTime(row.getEndAt()),
                            RealUi.dateTime(row.getRegistrationDeadline()), row.getRegisteredCount() + "/" + RealUi.text(row.getCapacity()),
                            RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<CompetitionDto>() {
                    @Override public void onSelected(CompetitionDto row) { select(row); }
                });
        if (role == Role.STUDENT) {
            JButton register = new PrimaryButton("报名"); register.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { register(); }
            }); value.addAction(register);
            JButton cancel = new DangerButton("取消报名"); cancel.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancel(); }
            }); value.addAction(cancel);
        } else if (role == Role.ACADEMIC_ADMIN) {
            JButton create = new PrimaryButton("新建比赛"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); value.addAction(create);
            JButton refresh = new JButton("刷新名单"); refresh.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { loadRoster(); }
            }); value.addAction(refresh);
        }
        return value;
    }

    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8)); panel.setOpaque(false); panel.add(detail, BorderLayout.NORTH);
        if (role == Role.ACADEMIC_ADMIN) { roster.setEditable(false); panel.add(new JScrollPane(roster), BorderLayout.CENTER); }
        return panel;
    }

    private void select(CompetitionDto value) {
        if (value == null) { detail.setText("选择比赛查看详情。"); if (editor != null) editor.startNew(); return; }
        detail.setText("比赛详情：" + RealUi.text(value.getTitle()) + "　" + RealUi.text(value.getDescription())
                + "　报名 " + value.getRegisteredCount() + "/" + RealUi.text(value.getCapacity()));
        if (editor != null) { editor.showCompetition(value); loadRoster(); }
    }

    private void register() {
        final CompetitionDto value = selected(); if (value == null) return;
        AsyncTask.run(new AsyncTask.Work<Object>() {
            @Override public Object run() throws Exception { return service.registerCompetition(value.getId()); }
        }, new AsyncTask.Callback<Object>() {
            @Override public void onSuccess(Object result) { page.showSuccess("报名成功。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void cancel() {
        final CompetitionDto value = selected(); if (value == null) return;
        if (!RealUi.confirm(this, "确认取消“" + RealUi.text(value.getTitle()) + "”的报名？")) return;
        AsyncTask.run(new AsyncTask.Work<Boolean>() {
            @Override public Boolean run() throws Exception { service.cancelCompetition(value.getId()); return Boolean.TRUE; }
        }, new AsyncTask.Callback<Boolean>() {
            @Override public void onSuccess(Boolean result) { page.showSuccess("报名已取消。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void save(CompetitionSaveRequest request) {
        final CompetitionSaveRequest finalRequest = request;
        AsyncTask.run(new AsyncTask.Work<CompetitionDto>() {
            @Override public CompetitionDto run() throws Exception { return service.saveCompetition(finalRequest); }
        }, new AsyncTask.Callback<CompetitionDto>() {
            @Override public void onSuccess(CompetitionDto result) { page.showSuccess("比赛已保存。"); table.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void loadRoster() {
        final CompetitionDto value = selected(); if (value == null || role != Role.ACADEMIC_ADMIN) return;
        roster.setText("正在加载报名名单…");
        AsyncTask.run(new AsyncTask.Work<CampusPage<CompetitionRegistrationDto>>() {
                    @Override public CampusPage<CompetitionRegistrationDto> run() throws Exception { return service.competitionRoster(value.getId(), CampusPageQuery.all()); }
                },
                new AsyncTask.Callback<CampusPage<CompetitionRegistrationDto>>() {
                    @Override public void onSuccess(CampusPage<CompetitionRegistrationDto> result) {
                        if (result.getItems().isEmpty()) { roster.setText("暂无报名记录"); return; }
                        StringBuilder text = new StringBuilder();
                        for (CompetitionRegistrationDto item : result.getItems()) text.append("学生 ").append(item.getStudentUserId())
                                .append("　").append(RealUi.status(item.getStatus())).append("　")
                                .append(RealUi.dateTime(item.getRegisteredAt())).append('\n');
                        roster.setText(text.toString());
                    }
                    @Override public void onFailure(Throwable error) { roster.setText("名单加载失败：" + AsyncTask.message(error)); }
                });
    }

    private CompetitionDto selected() {
        CompetitionDto value = table.selectedItem(); if (value == null) page.showWarning("请先选择比赛。"); return value;
    }
    private static String status(String filter) {
        if ("已发布".equals(filter)) return "PUBLISHED"; if ("已截止".equals(filter)) return "CLOSED";
        return "已取消".equals(filter) ? "CANCELLED" : null;
    }
}
