package edu.seu.vcampus.common.protocol.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Java 7 兼容的协议反序列化输入流。
 *
 * <p>只接受 common DTO、协议类型和协议实际使用的 JDK 值类型。底层预算是
 * 整条持久连接的累计字节数，不是单个对象或单个请求的大小；调用方应在协议
 * 错误时关闭连接。</p>
 */
public final class SafeObjectInputStream extends ObjectInputStream {
    /** 默认的单连接累计反序列化预算。 */
    public static final int MAX_STREAM_BYTES = 8 * 1024 * 1024;

    public SafeObjectInputStream(InputStream source) throws IOException {
        this(source, MAX_STREAM_BYTES);
    }

    public SafeObjectInputStream(InputStream source, long maxBytes) throws IOException {
        super(new CumulativeLimitedInputStream(source, maxBytes));
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException,
            ClassNotFoundException {
        String name = descriptor.getName();
        if (!isAllowed(name)) {
            throw new InvalidClassException("serialized class is not allowed: " + name);
        }
        return super.resolveClass(descriptor);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
        throw new InvalidClassException("serialized proxy classes are not allowed");
    }

    private static boolean isAllowed(String name) {
        if (name == null || name.length() == 0) return false;
        if (name.charAt(0) == '[') return isAllowedArray(name);
        if (name.startsWith("edu.seu.vcampus.common.dto.")
                || name.startsWith("edu.seu.vcampus.common.protocol.")
                || name.startsWith("edu.seu.vcampus.common.ai.")) {
            return true;
        }
        if ("edu.seu.vcampus.common.security.Role".equals(name)
                || "edu.seu.vcampus.common.security.Permission".equals(name)) {
            return true;
        }
        return isAllowedJdkValue(name);
    }

    private static boolean isAllowedJdkValue(String name) {
        if (isJavaLangValue(name) || isAllowedNumber(name)) return true;
        if (isAllowedTime(name)) return true;
        return isAllowedCollection(name) || "java.util.UUID".equals(name);
    }

    private static boolean isAllowedNumber(String name) {
        return "java.math.BigDecimal".equals(name)
                || "java.math.BigInteger".equals(name);
    }

    private static boolean isAllowedTime(String name) {
        return "org.threeten.bp.LocalDate".equals(name)
                || "org.threeten.bp.LocalDateTime".equals(name)
                || "org.threeten.bp.LocalTime".equals(name)
                || "org.threeten.bp.Ser".equals(name);
    }

    private static boolean isJavaLangValue(String name) {
        return "java.lang.Boolean".equals(name) || "java.lang.Byte".equals(name)
                || "java.lang.Character".equals(name) || "java.lang.Double".equals(name)
                || "java.lang.Float".equals(name) || "java.lang.Integer".equals(name)
                || "java.lang.Long".equals(name) || "java.lang.Number".equals(name)
                || "java.lang.Short".equals(name) || "java.lang.Enum".equals(name)
                || "java.lang.String".equals(name);
    }

    private static boolean isAllowedCollection(String name) {
        return "java.util.ArrayList".equals(name)
                || "java.util.Arrays$ArrayList".equals(name)
                || "java.util.HashMap".equals(name)
                || "java.util.LinkedHashMap".equals(name)
                || "java.util.HashSet".equals(name)
                || "java.util.LinkedHashSet".equals(name)
                || "java.util.EnumSet".equals(name)
                || "java.util.RegularEnumSet".equals(name)
                || "java.util.JumboEnumSet".equals(name)
                || "java.util.EnumSet$SerializationProxy".equals(name)
                || "java.util.Collections$EmptyList".equals(name)
                || "java.util.Collections$EmptySet".equals(name)
                || "java.util.Collections$EmptyMap".equals(name)
                || "java.util.Collections$SingletonList".equals(name)
                || "java.util.Collections$SingletonSet".equals(name)
                || "java.util.Collections$SingletonMap".equals(name)
                || "java.util.Collections$UnmodifiableCollection".equals(name)
                || "java.util.Collections$UnmodifiableList".equals(name)
                || "java.util.Collections$UnmodifiableRandomAccessList".equals(name)
                || "java.util.Collections$UnmodifiableSet".equals(name)
                || "java.util.Collections$UnmodifiableMap".equals(name);
    }

    private static boolean isAllowedArray(String name) {
        int index = 0;
        while (index < name.length() && name.charAt(index) == '[') index++;
        if (index >= name.length()) return false;
        char component = name.charAt(index);
        return "BCDFIJSZ".indexOf(component) >= 0
                || component == 'L' && name.endsWith(";")
                && isAllowed(name.substring(index + 1, name.length() - 1));
    }

    /** Counts bytes consumed by every object read from one persistent stream. */
    private static final class CumulativeLimitedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long consumed;

        private CumulativeLimitedInputStream(InputStream source, long maxBytes) {
            super(requireSource(source));
            if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            ensureAvailable();
            int value = super.read();
            if (value >= 0) consumed++;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) return 0;
            ensureAvailable();
            int allowed = (int) Math.min((long) length, maxBytes - consumed);
            int count = super.read(bytes, offset, allowed);
            if (count > 0) consumed += count;
            return count;
        }

        @Override
        public long skip(long length) throws IOException {
            if (length <= 0) return 0L;
            ensureAvailable();
            long allowed = Math.min(length, maxBytes - consumed);
            long skipped = super.skip(allowed);
            consumed += skipped;
            return skipped;
        }

        private void ensureAvailable() throws IOException {
            if (consumed >= maxBytes) throw new IOException("serialized stream exceeds size limit");
        }

        private static InputStream requireSource(InputStream source) {
            if (source == null) throw new IllegalArgumentException("source is required");
            return source;
        }
    }
}
