package edu.seu.vcampus.common.validation;

import java.util.regex.Pattern;

/** 客户端与服务端共用的格式型输入规则。 */
public final class InputRules {
    private static final Pattern ACCOUNT = Pattern.compile("[A-Za-z0-9_.@-]{3,64}");
    private static final Pattern PERSON_NAME = Pattern.compile("[\\p{L}][\\p{L}·•.'’\\- ]+");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._%+\\-]{0,63}@[A-Za-z0-9][A-Za-z0-9.\\-]{1,189}\\.[A-Za-z]{2,63}");
    private static final Pattern MOBILE = Pattern.compile("1[3-9][0-9]{9}");
    private static final Pattern CONTACT_PHONE = Pattern.compile(
            "(?:1[3-9][0-9]{9}|0[0-9]{2,3}-?[0-9]{7,8})");
    private static final Pattern STUDENT_NUMBER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{3,31}");
    private static final Pattern ID_DOCUMENT = Pattern.compile("[A-Za-z0-9]{6,20}");

    private InputRules() { }

    public static String account(String value) {
        return match(required(value, "校园账号"), ACCOUNT, "校园账号应为3至64位字母、数字或 . _ @ -");
    }

    public static String personName(String value, String label) {
        String text = required(value, label);
        if (text.length() < 2 || text.length() > 50 || !PERSON_NAME.matcher(text).matches()) {
            throw new IllegalArgumentException(label + "应为2至50个中文或英文字母，可含间隔号、空格、连字符");
        }
        return text;
    }

    public static String email(String value, boolean required) {
        String text = optional(value);
        if (text == null) {
            if (required) throw new IllegalArgumentException("邮箱不能为空");
            return null;
        }
        if (text.length() > 254 || text.contains("..") || !EMAIL.matcher(text).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确，例如 name@seu.edu.cn");
        }
        return text;
    }

    public static String mobile(String value, boolean required) {
        String text = optional(value);
        if (text == null) {
            if (required) throw new IllegalArgumentException("手机号不能为空");
            return null;
        }
        return match(text, MOBILE, "手机号应为11位中国大陆手机号");
    }

    public static String contactPhone(String value, boolean required) {
        String text = optional(value);
        if (text == null) {
            if (required) throw new IllegalArgumentException("联系电话不能为空");
            return null;
        }
        return match(text, CONTACT_PHONE, "联系电话应为11位手机号或带区号的固定电话");
    }

    public static String studentNumber(String value) {
        return match(required(value, "学号"), STUDENT_NUMBER,
                "学号应为4至32位字母、数字、连字符或下划线");
    }

    public static String identityDocument(String value) {
        return match(required(value, "证件号"), ID_DOCUMENT, "证件号应为6至20位字母或数字");
    }

    public static String password(String value, String label) {
        String text = required(value, label);
        if (text.length() < 8 || text.length() > 72 || containsWhitespace(text)
                || !text.matches(".*[A-Z].*") || !text.matches(".*[a-z].*")
                || !text.matches(".*[0-9].*")) {
            throw new IllegalArgumentException(label + "应为8至72位，包含大小写字母和数字且不能有空格");
        }
        return text;
    }

    public static String limited(String value, int max, String label, boolean required) {
        String text = optional(value);
        if (text == null) {
            if (required) throw new IllegalArgumentException(label + "不能为空");
            return null;
        }
        if (text.length() > max) throw new IllegalArgumentException(label + "不能超过" + max + "个字符");
        return text;
    }

    private static String required(String value, String label) {
        String text = optional(value);
        if (text == null) throw new IllegalArgumentException(label + "不能为空");
        return text;
    }

    private static String optional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String match(String value, Pattern pattern, String message) {
        if (!pattern.matcher(value).matches()) throw new IllegalArgumentException(message);
        return value;
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) if (Character.isWhitespace(value.charAt(i))) return true;
        return false;
    }
}
