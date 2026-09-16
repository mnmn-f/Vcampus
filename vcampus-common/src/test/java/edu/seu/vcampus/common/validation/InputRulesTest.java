package edu.seu.vcampus.common.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public final class InputRulesTest {
    @Test public void acceptsCampusIdentityInputs() {
        assertEquals("demo_student", InputRules.account(" demo_student "));
        assertEquals("王晨茜", InputRules.personName(" 王晨茜 ", "姓名"));
        assertEquals("Ada Lovelace", InputRules.personName("Ada Lovelace", "姓名"));
        assertEquals("student@seu.edu.cn", InputRules.email("student@seu.edu.cn", false));
        assertEquals("17740208438", InputRules.mobile("17740208438", false));
        assertEquals("025-83790000", InputRules.contactPhone("025-83790000", false));
        assertEquals("090260100", InputRules.studentNumber("090260100"));
        assertNull(InputRules.email(" ", false));
    }

    @Test public void rejectsMalformedIdentityInputs() {
        rejects(() -> InputRules.personName("王123", "姓名"));
        rejects(() -> InputRules.email("student@", false));
        rejects(() -> InputRules.mobile("123456", false));
        rejects(() -> InputRules.studentNumber("学号 01"));
        rejects(() -> InputRules.password("password", "密码"));
    }

    private static void rejects(Runnable work) {
        try { work.run(); fail("应拒绝非法输入"); }
        catch (IllegalArgumentException expected) { }
    }
}
