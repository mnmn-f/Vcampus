package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseInstructorDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.SchedulingTeacherDto;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JList;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 课程教师必须从真实教师选项多选，编辑时按关联表回填。 */
public final class CourseEditorTeacherSelectionTest {
    @Test public void multipleTeachersRefillAndSaveByUserId() {
        Capture listener = new Capture(); CourseEditorPanel panel = new CourseEditorPanel(listener);
        panel.setAvailableTeachers(Arrays.asList(teacher(1,"张三","T001"),teacher(2,"李四","T002"),teacher(3,"王五","T003")));
        panel.showCourse(new CourseDto(7,"CS101","程序设计","REQUIRED",BigDecimal.valueOf(3),
                Integer.valueOf(48),60,0,"说明","PUBLISHED",Collections.emptyList(),
                Arrays.asList(new CourseInstructorDto(1,"张三","T001","PRIMARY"),
                        new CourseInstructorDto(3,"王五","T003","ASSISTANT")),"2026-FALL"));
        JList<?> list=find(panel,JList.class);
        assertEquals(2,list.getSelectedIndices().length);
        list.setSelectedIndices(new int[]{0,1});
        findButton(panel,"保存课程").doClick();
        assertNotNull(listener.request);
        assertEquals(Arrays.asList(Long.valueOf(1),Long.valueOf(2)),listener.request.getInstructorUserIds());
    }
    private static SchedulingTeacherDto teacher(long id,String name,String no){return new SchedulingTeacherDto(id,name,no);}
    private static <T extends Component>T find(Component root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Container)for(Component child:((Container)root).getComponents()){T found=find(child,type);if(found!=null)return found;}return null;}
    private static JButton findButton(Component root,String text){if(root instanceof JButton&&text.equals(((JButton)root).getText()))return(JButton)root;if(root instanceof Container)for(Component child:((Container)root).getComponents()){JButton found=findButton(child,text);if(found!=null)return found;}return null;}
    private static final class Capture implements CourseEditorPanel.Listener { CourseSaveRequest request; @Override public void onSave(CourseSaveRequest value,boolean update){request=value;} }
}
