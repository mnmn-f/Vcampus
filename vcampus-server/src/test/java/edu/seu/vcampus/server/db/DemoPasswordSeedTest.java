package edu.seu.vcampus.server.db;

import edu.seu.vcampus.server.security.PasswordHasher;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 防止演示数据再次出现“有哈希但没有可用密码”的不可登录状态。 */
public class DemoPasswordSeedTest {
    private static final Pattern USER = Pattern.compile(
            "\\('([^']+)', '([^']+)', '演示");

    @Test
    public void documentedDemoPasswordsMatchSeedHashes() throws Exception {
        Map<String, String> passwords = new LinkedHashMap<String, String>();
        passwords.put("demo_student", "student123");
        passwords.put("demo_teacher", "teacher123");
        passwords.put("demo_registrar", "registrar123");
        passwords.put("demo_academic", "academic123");
        passwords.put("demo_librarian", "library123");
        passwords.put("demo_store", "store123");
        passwords.put("demo_dorm", "dorm123");
        passwords.put("demo_ai", "ai123");
        passwords.put("demo_system", "system123");

        String sql = resource("/db/migration/V2__demo_data.sql");
        Matcher matcher = USER.matcher(sql);
        int verified = 0;
        PasswordHasher hasher = new PasswordHasher();
        while (matcher.find()) {
            String password = passwords.get(matcher.group(1));
            if (password != null) {
                assertTrue(matcher.group(1), hasher.matches(password, matcher.group(2)));
                verified++;
            }
        }
        assertEquals(passwords.size(), verified);
    }

    private static String resource(String name) throws Exception {
        InputStream input = DemoPasswordSeedTest.class.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("missing resource " + name);
        }
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
