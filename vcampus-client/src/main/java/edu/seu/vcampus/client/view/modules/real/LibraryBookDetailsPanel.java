package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.ResponsiveGridLayout;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.common.dto.library.BookDetail;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** 可自适应的封面、馆藏信息和简介展示面板。 */
public final class LibraryBookDetailsPanel extends JPanel {
    public LibraryBookDetailsPanel() { super(new BorderLayout(20, 16)); setOpaque(false); }
    public void showBook(BookDetail book) {
        this.removeAll();
        JPanel top = new JPanel(new ResponsiveGridLayout(240, 2, 20)); top.setOpaque(false);
        JLabel cover = new JLabel("暂无封面", JLabel.CENTER);
        cover.setPreferredSize(new Dimension(210, 280));
        cover.setOpaque(true); cover.setBackground(edu.seu.vcampus.client.ui.DesignTokens.PRIMARY_LIGHT);
        cover.setVerticalTextPosition(JLabel.BOTTOM); cover.setHorizontalTextPosition(JLabel.CENTER);
        cover.setIcon(edu.seu.vcampus.client.ui.LineIcon.of(edu.seu.vcampus.client.ui.LineIcon.Kind.LIBRARY,
                edu.seu.vcampus.client.ui.DesignTokens.PRIMARY, 80));
        byte[] bytes = book.getCoverImage();
        if (bytes != null && bytes.length > 0) { cover.setText(""); cover.setIcon(new ImageIcon(bytes)); }
        top.add(cover);
        JTextArea info = UiFactory.textArea(10, 22); info.setEditable(false);
        info.setText(book.getTitle() + "\n\n作者：" + RealUi.text(book.getAuthor())
                + "\n出版社：" + RealUi.text(book.getPublisher()) + "\n出版年份：" + RealUi.text(book.getPublicationYear())
                + "\nISBN：" + RealUi.text(book.getIsbn()) + "\n分类：" + RealUi.text(book.getCategory())
                + "\n馆藏位置：" + RealUi.text(book.getLocation()) + "\n可借数量 / 馆藏总量："
                + book.getAvailableCopies() + " / " + book.getTotalCopies() + "\n状态：" + RealUi.status(book.getStatus()));
        top.add(info); this.add(top, BorderLayout.NORTH);
        JTextArea description = UiFactory.textArea(4, 28); description.setEditable(false);
        description.setText(RealUi.text(book.getDescription()));
        this.add(UiFactory.labelledField("内容简介", description), BorderLayout.CENTER);

        revalidate(); repaint();
    }
}
