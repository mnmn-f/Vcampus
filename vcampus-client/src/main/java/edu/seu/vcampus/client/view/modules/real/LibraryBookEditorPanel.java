package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BookUpsertRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 图书管理员的行内书目和库存表单。 */
public final class LibraryBookEditorPanel extends SectionCard {
    public interface Listener { void onSave(BookUpsertRequest request); }

    private final JTextField isbn = field(); private final JTextField title = field();
    private final JTextField author = field(); private final JTextField publisher = field();
    private final JTextField category = field(); private final JTextField total = field();
    private final JTextField available = field(); private final JTextField location = field();
    private final JTextArea description = UiFactory.textArea(3, 28);
    private final JComboBox<String> status = new JComboBox<String>(new String[]{"ON_SHELF", "UNAVAILABLE", "ARCHIVED"});
    private final JLabel error = UiFactory.muted(" "); private final Listener listener; private long id;

    public LibraryBookEditorPanel(Listener listener) {
        super("图书详情与维护", "维护书目、库存和启用状态；库存调整受借阅情况限制。");
        this.listener = listener; status.setFont(DesignTokens.regular(13)); RealUi.codeRenderer(status);
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "ISBN", isbn); add(fields, "书名", title); add(fields, "作者", author); add(fields, "出版社", publisher);
        add(fields, "分类", category); add(fields, "总库存", total); add(fields, "可借库存", available); add(fields, "馆藏位置", location); add(fields, "状态", status);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.NORTH);
        content.add(UiFactory.labelledField("简介", description), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        javax.swing.JButton save = new PrimaryButton("保存图书"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(clear); actions.add(save); actions.add(error);
        content.add(actions, BorderLayout.SOUTH); setContent(content); startNew();
    }

    public void startNew() { id = 0; clear(); status.setSelectedItem("ON_SHELF"); error.setText(" "); }
    public void showBook(BookDetail book) {
        if (book == null) { startNew(); return; } id = book.getId(); isbn.setText(RealUi.input(book.getIsbn())); title.setText(RealUi.input(book.getTitle()));
        author.setText(RealUi.input(book.getAuthor())); publisher.setText(RealUi.input(book.getPublisher())); category.setText(RealUi.input(book.getCategory()));
        total.setText(String.valueOf(book.getTotalCopies())); available.setText(String.valueOf(book.getAvailableCopies())); location.setText(RealUi.input(book.getLocation()));
        description.setText(book.getDescription() == null ? "" : book.getDescription()); status.setSelectedItem(book.getStatus()); error.setText(" ");
    }

    private void save() {
        try {
            int all = Integer.parseInt(required(total.getText(), "总库存")); int free = Integer.parseInt(required(available.getText(), "可借库存"));
            if (all < 0 || free < 0 || free > all) throw new IllegalArgumentException("库存数量不正确");
            if (listener != null) listener.onSave(new BookUpsertRequest(id, optional(isbn.getText()), required(title.getText(), "书名"),
                    optional(author.getText()), optional(publisher.getText()), optional(category.getText()), Integer.valueOf(all), Integer.valueOf(free),
                    optional(location.getText()), optional(description.getText()), String.valueOf(status.getSelectedItem()))); error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("库存必须是数字"); } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private void clear() { isbn.setText(""); title.setText(""); author.setText(""); publisher.setText(""); category.setText(""); total.setText(""); available.setText(""); location.setText(""); description.setText(""); }
    private static void add(JPanel panel, String label, java.awt.Component component) { panel.add(UiFactory.labelledField(label, component)); }
    private static JTextField field() { return UiFactory.textField(12); }
    private static String required(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + "不能为空"); return value.trim(); }
    private static String optional(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
