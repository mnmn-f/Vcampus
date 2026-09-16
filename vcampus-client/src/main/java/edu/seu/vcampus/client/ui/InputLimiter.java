package edu.seu.vcampus.client.ui;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.util.regex.Pattern;

/** 输入阶段的字符与长度限制；完整格式仍由公共 InputRules 校验。 */
public final class InputLimiter {
    private InputLimiter() { }

    public static void length(JTextComponent field, int max) { install(field, max, null); }
    public static void account(JTextComponent field) { install(field, 64, "[A-Za-z0-9_.@-]*"); }
    public static void personName(JTextComponent field) { install(field, 50, "[\\p{L}·•.'’\\- ]*"); }
    public static void email(JTextComponent field) { install(field, 254, "[A-Za-z0-9@._%+\\-]*"); }
    public static void mobile(JTextComponent field) { install(field, 11, "[0-9]*"); }
    public static void contactPhone(JTextComponent field) { install(field, 16, "[0-9-]*"); }
    public static void studentNumber(JTextComponent field) { install(field, 32, "[A-Za-z0-9_-]*"); }
    public static void identityDocument(JTextComponent field) { install(field, 20, "[A-Za-z0-9]*"); }
    public static void code(JTextComponent field, int max) { install(field, max, "[A-Za-z0-9_.-]*"); }
    public static void unsignedInteger(JTextComponent field, int digits) { install(field, digits, "[0-9]*"); }
    public static void signedInteger(JTextComponent field, int digits) { install(field, digits + 1, "-?[0-9]*"); }
    public static void numericList(JTextComponent field, int max) { install(field, max, "[0-9, ]*"); }
    public static void isbn(JTextComponent field) { install(field, 20, "[0-9Xx-]*"); }
    public static void time(JTextComponent field) { install(field, 5, "[0-9:]*"); }
    public static void decimal(JTextComponent field, int integerDigits, int scale) {
        install(field, integerDigits + scale + 1, "[0-9]{0," + integerDigits
                + "}(?:\\.[0-9]{0," + scale + "})?");
    }

    private static void install(JTextComponent field, int max, String allowed) {
        if (field == null || max <= 0 || !(field.getDocument() instanceof AbstractDocument)) return;
        ((AbstractDocument) field.getDocument()).setDocumentFilter(
                new LimitedFilter(max, allowed == null ? null : Pattern.compile(allowed)));
    }

    private static final class LimitedFilter extends DocumentFilter {
        private final int max;
        private final Pattern allowed;
        private LimitedFilter(int max, Pattern allowed) { this.max = max; this.allowed = allowed; }

        @Override public void insertString(FilterBypass bypass, int offset, String text,
                                           AttributeSet attributes) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override public void replace(FilterBypass bypass, int offset, int length, String text,
                                      AttributeSet attributes) throws BadLocationException {
            String addition = text == null ? "" : text;
            String old = bypass.getDocument().getText(0, bypass.getDocument().getLength());
            String candidate = old.substring(0, offset) + addition + old.substring(offset + length);
            if (candidate.length() <= max && (allowed == null || allowed.matcher(candidate).matches())) {
                bypass.replace(offset, length, text, attributes);
            }
        }
    }
}
