package edu.seu.vcampus.server.ai.tool;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 默认工具表覆盖设计说明书中的业务模块，写操作具有确认标记。 */
public final class AiToolRegistryTest {
    @Test public void coversCampusBusinessModules() {
        AiToolRegistry registry = AiToolRegistry.campusDefaults();
        assertNotNull(registry.get("identity.profile.read"));
        assertNotNull(registry.get("student.grades.read"));
        assertNotNull(registry.get("academic.schedule.read"));
        assertNotNull(registry.get("library.book.search"));
        assertNotNull(registry.get("store.account.read"));
        assertNotNull(registry.get("dorm.accommodation.read"));
        assertNotNull(registry.get("campus.announcement.read"));
        assertTrue(registry.all().size() >= 25);
    }

    @Test public void distinguishesReadAndWriteOperations() {
        AiToolRegistry registry = AiToolRegistry.campusDefaults();
        assertFalse(registry.get("student.grades.read").isWriteOperation());
        assertTrue(registry.get("academic.course.drop").isWriteOperation());
        assertTrue(registry.get("library.book.return").isWriteOperation());
        assertTrue(registry.get("store.order.create").isWriteOperation());
        assertTrue(registry.get("campus.competition.register").isWriteOperation());
    }
}
