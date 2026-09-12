package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import java.awt.BorderLayout;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 本地选择仅准备上传；保存商品时与商品资料一并提交。 */
final class ProductImageEditor extends JPanel {
    private final ProductImageView preview;
    private final JLabel state = UiFactory.muted(" ");
    private String reference;
    private byte[] bytes;
    private int serial;
    private boolean loading;
    ProductImageEditor(StoreClientService service) {
        super(new BorderLayout(8, 4)); setOpaque(false); preview = new ProductImageView(120, 100, service);
        JPanel actions = UiFactory.horizontal(6);
        SecondaryButton select = new SecondaryButton("选择商品图片"), remove = new SecondaryButton("移除图片");
        select.addActionListener(e -> choose()); remove.addActionListener(e -> showImage(null));
        actions.add(select); actions.add(remove); actions.add(state); add(preview, BorderLayout.WEST); add(actions, BorderLayout.CENTER);
    }
    void showImage(String value) { serial++; loading = false; bytes = null; reference = value; preview.load(value); state.setText(" "); }
    String reference() { return reference; }
    byte[] bytes() { if (loading) throw new IllegalArgumentException("图片正在处理，请稍候保存"); return bytes == null ? null : bytes.clone(); }
    private void choose() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("选择商品图片");
        chooser.setAcceptAllFileFilterUsed(false); chooser.setFileFilter(new FileNameExtensionFilter("JPG / PNG 图片", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) selectFile(chooser.getSelectedFile());
    }
    void selectFile(File file) {
        final int request = ++serial; loading = true; state.setText("正在处理图片…");
        AsyncTask.run(() -> ImageUploadSupport.encode(file, 640, 480, false), new AsyncTask.Callback<byte[]>() {
            @Override public void onSuccess(byte[] value) { if (request != serial) return; loading = false; bytes = value; reference = null; preview.showBytes(value); state.setText("待保存"); }
            @Override public void onFailure(Throwable error) { if (request != serial) return; loading = false; state.setText(AsyncTask.message(error)); }
        });
    }
}
