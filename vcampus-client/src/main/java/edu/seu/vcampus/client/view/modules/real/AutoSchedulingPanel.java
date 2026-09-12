package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.AutoScheduleConfirmRequest;
import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.AutoScheduleSaveResult;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.SchedulingTeacherDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** Admin preview-first UI for teacher preferences and one-click scheduling. */
public final class AutoSchedulingPanel extends JPanel {
    private final BasePage page;
    private final AcademicClientService service;
    private final JComboBox<SchedulingTeacherDto> teacher = new JComboBox<SchedulingTeacherDto>();
    private final JComboBox<String> weekday = new JComboBox<String>(new String[]{"星期一","星期二","星期三","星期四","星期五"});
    private final JSpinner start = new JSpinner(new SpinnerNumberModel(1,1,10,1));
    private final JSpinner end = new JSpinner(new SpinnerNumberModel(2,1,10,1));
    private final JComboBox<TimePreferenceType> type = new JComboBox<TimePreferenceType>(TimePreferenceType.values());
    private final DefaultTableModel preferenceModel = model(new String[]{"教师","星期","节次","类型"});
    private final JTable preferenceTable = table(preferenceModel);
    private final DefaultTableModel previewModel = model(new String[]{"课程","教师","班级/教学班","星期","节次","教室"});
    private final JTable previewTable = table(previewModel);
    private final JLabel state = UiFactory.muted(" ");
    private List<TeacherTimePreferenceDto> preferences = Collections.emptyList();
    private List<AutoScheduleEntryDto> preview = Collections.emptyList();
    private final JButton generate = new PrimaryButton("开始自动排课");
    private final JButton regenerate = new SecondaryButton("重新排课");
    private final JButton confirm = new PrimaryButton("确认保存");

    public AutoSchedulingPanel(BasePage page, AcademicClientService service) {
        super(); this.page=page; this.service=service; setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        add(preferenceCard()); add(javax.swing.Box.createVerticalStrut(DesignTokens.SPACE_12)); add(previewCard());
        wire(); loadOverview();
    }

    private SectionCard preferenceCard() {
        SectionCard card=new SectionCard("教师时间偏好 / 不可用时间","");
        JPanel fields=new JPanel(new GridLayout(2,3,8,8));fields.setOpaque(false);
        fields.add(UiFactory.labelledField("教师",teacher));fields.add(UiFactory.labelledField("星期",weekday));
        fields.add(UiFactory.labelledField("开始节次",start));fields.add(UiFactory.labelledField("结束节次",end));fields.add(UiFactory.labelledField("类型",type));
        JPanel actions=UiFactory.horizontal(8);JButton save=new PrimaryButton("保存偏好");JButton delete=new DangerButton("删除选中");actions.add(save);actions.add(delete);
        save.addActionListener(e->savePreference());delete.addActionListener(e->deletePreference());
        JScrollPane scroll=new JScrollPane(preferenceTable);scroll.setPreferredSize(new Dimension(850,150));
        JPanel content=new JPanel(new BorderLayout(0,8));content.setOpaque(false);content.add(fields,BorderLayout.NORTH);content.add(scroll,BorderLayout.CENTER);content.add(actions,BorderLayout.SOUTH);card.setContent(content);return card;
    }

    private SectionCard previewCard() {
        SectionCard card=new SectionCard("一键自动排课","");
        JPanel actions=UiFactory.horizontal(8);actions.add(generate);actions.add(regenerate);actions.add(confirm);actions.add(state);
        JScrollPane scroll=new JScrollPane(previewTable);scroll.setPreferredSize(new Dimension(900,260));
        JPanel content=new JPanel(new BorderLayout(0,8));content.setOpaque(false);content.add(actions,BorderLayout.NORTH);content.add(scroll,BorderLayout.CENTER);card.setContent(content);return card;
    }

    private void wire(){generate.addActionListener(e->generate());regenerate.addActionListener(e->generate());confirm.addActionListener(e->confirm());setBusy(false);}
    private void loadOverview(){setBusy(true);AsyncTask.run(()->service.schedulingOverview(),new AsyncTask.Callback<SchedulingOverviewDto>(){
        @Override public void onSuccess(SchedulingOverviewDto value){teacher.removeAllItems();for(SchedulingTeacherDto t:value.getTeachers())teacher.addItem(t);preferences=new ArrayList<TeacherTimePreferenceDto>(value.getPreferences());refreshPreferences();setBusy(false);}
        @Override public void onFailure(Throwable cause){state.setText(AsyncTask.message(cause));setBusy(false);}});}
    private void savePreference(){SchedulingTeacherDto selected=(SchedulingTeacherDto)teacher.getSelectedItem();if(selected==null){state.setText("没有可选教师。");return;}int s=((Number)start.getValue()).intValue(),e=((Number)end.getValue()).intValue();if(e<s){state.setText("结束节次不能早于开始节次。");return;}setBusy(true);TeacherTimePreferenceDto request=new TeacherTimePreferenceDto(0,selected.getUserId(),weekday.getSelectedIndex()+1,s,e,String.valueOf(type.getSelectedItem()));AsyncTask.run(()->service.saveTimePreference(request),new AsyncTask.Callback<TeacherTimePreferenceDto>(){
        @Override public void onSuccess(TeacherTimePreferenceDto value){page.showSuccess("教师时间偏好已保存。");loadOverview();}
        @Override public void onFailure(Throwable cause){state.setText(AsyncTask.message(cause));setBusy(false);}});}
    private void deletePreference(){int row=preferenceTable.getSelectedRow();if(row<0||row>=preferences.size()){state.setText("请先选择一条偏好记录。");return;}TeacherTimePreferenceDto value=preferences.get(row);if(!RealUi.confirm(this,"确认删除选中的教师时间偏好？"))return;setBusy(true);AsyncTask.run(()->{service.deleteTimePreference(value.getId());return Boolean.TRUE;},new AsyncTask.Callback<Boolean>(){
        @Override public void onSuccess(Boolean value){page.showSuccess("教师时间偏好已删除。");loadOverview();}
        @Override public void onFailure(Throwable cause){state.setText(AsyncTask.message(cause));setBusy(false);}});}
    private void generate(){setBusy(true);state.setText("正在搜索合法方案…");AsyncTask.run(()->service.previewAutoSchedule(8000),new AsyncTask.Callback<AutoSchedulePreviewDto>(){
        @Override public void onSuccess(AutoSchedulePreviewDto value){preview=new ArrayList<AutoScheduleEntryDto>(value.getEntries());refreshPreview();state.setText(join(value.getExplanations())+(value.isSuccess()?"  总惩罚："+value.getTotalPenalty():""));setBusy(false);}
        @Override public void onFailure(Throwable cause){state.setText(AsyncTask.message(cause));setBusy(false);}});}
    private void confirm(){if(preview.isEmpty()){state.setText("请先生成可保存的排课预览。");return;}if(!RealUi.confirm(this,"确认保存预览中的 "+preview.size()+" 条课次？保存前将再次检查全部冲突。"))return;setBusy(true);final AutoScheduleConfirmRequest request=new AutoScheduleConfirmRequest(preview);AsyncTask.run(()->service.confirmAutoSchedule(request),new AsyncTask.Callback<AutoScheduleSaveResult>(){
        @Override public void onSuccess(AutoScheduleSaveResult value){preview=Collections.emptyList();refreshPreview();page.showSuccess("已原子保存 "+value.getSavedCount()+" 条自动课表。");state.setText("保存成功，可刷新课程与排课查看结果。");setBusy(false);}
        @Override public void onFailure(Throwable cause){state.setText(AsyncTask.message(cause));setBusy(false);}});}
    private void refreshPreferences(){preferenceModel.setRowCount(0);for(TeacherTimePreferenceDto p:preferences)preferenceModel.addRow(new Object[]{teacherName(p.getTeacherUserId()),day(p.getWeekday()),p.getStartPeriod()+"-"+p.getEndPeriod(),RealUi.status(p.getPreferenceType())});}
    private void refreshPreview(){previewModel.setRowCount(0);for(AutoScheduleEntryDto p:preview)previewModel.addRow(new Object[]{p.getCourseCode()+" "+p.getCourseName(),p.getTeacherNames(),join(p.getStudentGroups()),day(p.getWeekday()),p.getStartPeriod()+"-"+p.getEndPeriod(),p.getClassroomName()});}
    private String teacherName(long id){for(int i=0;i<teacher.getItemCount();i++){SchedulingTeacherDto t=teacher.getItemAt(i);if(t.getUserId()==id)return t.toString();}return "教师";}
    private void setBusy(boolean busy){generate.setEnabled(!busy);regenerate.setEnabled(!busy);confirm.setEnabled(!busy&&!preview.isEmpty());}
    private static JTable table(DefaultTableModel m){JTable t=new JTable(m);t.setRowHeight(34);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setFillsViewportHeight(true);return t;}
    private static DefaultTableModel model(String[] columns){return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    private static String day(int d){return d>=1&&d<=7?new String[]{"星期一","星期二","星期三","星期四","星期五","星期六","星期日"}[d-1]:"星期"+d;}
    private static String join(List<String> values){StringBuilder r=new StringBuilder();for(String v:values){if(r.length()>0)r.append("；");r.append(v);}return r.toString();}
}
