package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** 图书管理员线上资源新增、编辑和启停表单。 */
public final class ResourceEditorPanel extends SectionCard implements RealUi.EditorActions {
    public interface Listener { void onSave(OnlineResourceUpsertRequest request); }
    private final JTextField title = field(); private final JTextField type = field(); private final JTextField url = field();
    private final JTextArea description = UiFactory.textArea(3, 28); private final JComboBox<String> status = new JComboBox<String>(new String[]{"ACTIVE", "INACTIVE"});
    private final javax.swing.JLabel error = UiFactory.muted(" "); private final Listener listener; private long id;

    public ResourceEditorPanel(Listener listener) {
        super("线上资源维护", "编辑资源信息并启用或停用；普通用户只显示已启用资源。"); this.listener = listener; status.setFont(DesignTokens.regular(13)); RealUi.codeRenderer(status);
        JPanel fields = RealUi.editorFields(); add(fields, "资源名称", title); add(fields, "资源类型", type); add(fields, "资源地址", url); add(fields, "状态", status);
        JPanel actions = RealUi.editorActions("保存资源", this, error); JPanel content = RealUi.editorContent(fields, description, actions); setContent(content); startNew();
    }
    public void startNew() { id = 0; title.setText(""); type.setText(""); url.setText(""); description.setText(""); status.setSelectedItem("ACTIVE"); error.setText(" "); }
    public void showResource(OnlineResourceView value) { if (value == null) { startNew(); return; } id = value.getId(); title.setText(RealUi.input(value.getTitle())); type.setText(RealUi.input(value.getResourceType())); url.setText(RealUi.input(value.getUrl())); description.setText(value.getDescription() == null ? "" : value.getDescription()); status.setSelectedItem(value.getStatus()); error.setText(" "); }
    @Override public void save() { try { String name = RealUi.required(title.getText(), "资源名称"); String resourceType = RealUi.required(type.getText(), "资源类型"); String resourceUrl = RealUi.required(url.getText(), "资源地址"); if (listener != null) listener.onSave(new OnlineResourceUpsertRequest(id, name, resourceType, resourceUrl, RealUi.optional(description.getText()), String.valueOf(status.getSelectedItem()))); error.setText(" "); } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); } }
    private static JTextField field() { return UiFactory.textField(12); }
    private static void add(JPanel p, String label, java.awt.Component c) { p.add(UiFactory.labelledField(label, c)); }
}
