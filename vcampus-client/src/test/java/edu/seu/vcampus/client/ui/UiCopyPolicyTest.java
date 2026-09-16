package edu.seu.vcampus.client.ui;

import edu.seu.vcampus.client.ui.components.PageHeader;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.modules.real.DormUi;
import org.junit.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 页面保留业务标题，不显示开发说明式副标题。 */
public final class UiCopyPolicyTest {
    @Test public void pageHeaderDoesNotRenderDescription() {
        PageHeader header = new PageHeader("住宿与空间", "开发说明不应出现在页面中");
        assertTrue(hasLabel(header, "住宿与空间"));
        assertFalse(hasLabel(header, "开发说明不应出现在页面中"));
    }

    @Test public void sectionCardDoesNotRenderSubtitle() {
        SectionCard card = new SectionCard("学生档案", "选中一行后在下方编辑");
        assertTrue(hasLabel(card, "学生档案"));
        assertFalse(hasLabel(card, "选中一行后在下方编辑"));
    }

    @Test public void dormHeaderDoesNotRenderSubtitle() {
        Container header = DormUi.header("住宿与空间", "选中一行后在下方编辑", null, false);
        assertTrue(hasLabel(header, "住宿与空间"));
        assertFalse(hasLabel(header, "选中一行后在下方编辑"));
    }

    private static boolean hasLabel(Component root, String text) {
        if (root instanceof JLabel && text.equals(((JLabel) root).getText())) return true;
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                if (hasLabel(child, text)) return true;
            }
        }
        return false;
    }
}
