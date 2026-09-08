package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.PdfClientService;
import edu.seu.vcampus.client.service.library.PdfFileTransfers;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.nio.file.Path;

/** 学生 PDF 上传表单；校验和上传在后台执行。 */
public final class PdfUploadPanel extends SectionCard {
    private final BasePage page;
    private final PdfClientService service;
    private final JTextField title = UiFactory.textField(20);
    private final JTextArea description = UiFactory.textArea(4, 24);
    private final JLabel selected = UiFactory.muted("尚未选择文件");
    private final JLabel message = UiFactory.muted("仅支持 PDF，单个文件最大 50 MB。审核通过后公开。");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JButton choose = new SecondaryButton("选择 PDF");
    private final JButton upload = new PrimaryButton("提交审核");
    private Path file;
    public PdfUploadPanel(BasePage page, PdfClientService service) {
        super("上传 PDF 资源", "填写资源信息并上传文件。"); this.page = page; this.service = service;
        JPanel form = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(220, 1, 12)); form.setOpaque(false);
        form.add(UiFactory.labelledField("资源名称", title)); form.add(UiFactory.labelledField("资源简介", description));
        JPanel selectRow = new JPanel(new edu.seu.vcampus.client.ui.WrapLayout(8)); selectRow.setOpaque(false);
        selectRow.add(choose); selectRow.add(selected); form.add(selectRow);
        choose.addActionListener(event -> selectFile()); upload.addActionListener(event -> upload());
        JPanel footer = new JPanel(new BorderLayout(8, 8)); footer.setOpaque(false);
        JPanel actions = new JPanel(new edu.seu.vcampus.client.ui.WrapLayout(8)); actions.setOpaque(false);
        actions.add(upload); actions.add(message); footer.add(actions, BorderLayout.NORTH);
        progress.setStringPainted(true); footer.add(progress, BorderLayout.SOUTH);
        JPanel body = new JPanel(new BorderLayout(10, 10)); body.setOpaque(false);
        body.add(form, BorderLayout.CENTER); body.add(footer, BorderLayout.SOUTH); setContent(body);
    }
    private void selectFile() {
        JFileChooser chooser = new JFileChooser(); chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF 文件", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        file = chooser.getSelectedFile().toPath(); selected.setText(file.getFileName().toString()); selected.setToolTipText(file.toString());
        if (title.getText().trim().isEmpty()) title.setText(file.getFileName().toString().replaceFirst("(?i)\\.pdf$", ""));
    }
    private void upload() {
        if (file == null) { message.setText("请先选择 PDF 文件"); return; }
        final Path source = file; final String name = title.getText().trim(), summary = description.getText().trim();
        if (name.isEmpty()) { message.setText("请填写资源名称"); return; }
        upload.setEnabled(false); choose.setEnabled(false); title.setEnabled(false); description.setEnabled(false);
        progress.setValue(0); progress.setIndeterminate(true); message.setText("正在校验并上传文件…");
        AsyncTask.run(() -> new PdfFileTransfers(service).upload(source, name, summary,
                (bytes, total) -> SwingUtilities.invokeLater(() -> {
                    progress.setIndeterminate(false); progress.setValue((int) (bytes * 100 / Math.max(1, total)));
                    message.setText(bytes == total ? "上传完成，正在提交审核…" : "正在上传：" + PdfUi.size(bytes) + " / " + PdfUi.size(total));
                })), new AsyncTask.Callback<PdfResourceView>() {
            @Override public void onSuccess(PdfResourceView value) {
                enabled(); message.setText("已提交，审核状态：待审核。请在“我的上传”中查看结果。");
                page.putClientProperty("library.pdf.version", System.nanoTime());
            }
            @Override public void onFailure(Throwable error) { enabled(); message.setText("上传失败：" + AsyncTask.message(error)); }
        });
    }
    private void enabled() { progress.setIndeterminate(false); upload.setEnabled(true); choose.setEnabled(true); title.setEnabled(true); description.setEnabled(true); }
}
