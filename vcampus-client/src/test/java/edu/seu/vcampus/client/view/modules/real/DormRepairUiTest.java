package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkerDto;
import org.junit.Test;

import javax.swing.JComboBox;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 宿舍报修表单的可见字段不泄露内部编号，受控值使用选择器。 */
public final class DormRepairUiTest {
    @Test
    public void workerFallbackDoesNotExposeUserId() {
        String text = new RepairWorkerDto(9876L, null, 2).summary();
        assertFalse(text.contains("9876"));
        assertTrue(text.contains("维修员"));
    }

    @Test
    public void studentRepairUsesCategorySelectorAndNoManualRoomId() throws Exception {
        assertEquals(JComboBox.class, field("composeCategory", DormStudentRepairPage.class).getType());
        try {
            field("composeRoomId", DormStudentRepairPage.class);
            throw new AssertionError("student repair must use current accommodation");
        } catch (NoSuchFieldException expected) {
            // Room is supplied by the student's active accommodation.
        }
    }

    @Test
    public void spaceEditorUsesBuildingAndRoomSelectors() throws Exception {
        assertEquals(JComboBox.class, field("roomBuilding", DormSpaceEditorPanel.class).getType());
        assertEquals(JComboBox.class, field("bedRoom", DormSpaceEditorPanel.class).getType());
    }

    @Test
    public void managerSelectsWorkerFromControlledList() throws Exception {
        assertEquals(JComboBox.class, field("workerSelect", DormManagerRepairPage.class).getType());
    }

    private static Field field(String name, Class<?> type) throws Exception {
        Field value = type.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }
}
