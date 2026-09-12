package edu.seu.vcampus.client.view.modules.real;

import java.awt.Component;
import java.io.File;
import java.util.Base64;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 头像选择及内嵌编码，复用统一图片限制和裁剪。 */
final class AvatarUploadSupport {
    private AvatarUploadSupport() { }
    static String chooseAndEncode(Component parent) throws Exception {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("选择头像");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件（JPG、PNG）", "jpg", "jpeg", "png"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? encode(chooser.getSelectedFile()) : null;
    }
    static String encode(File file) throws Exception {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(ImageUploadSupport.encode(file, 256, 256, true));
    }
}
