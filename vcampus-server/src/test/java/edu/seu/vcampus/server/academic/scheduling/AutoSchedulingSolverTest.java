package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleConfirmRequest;
import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.AutoScheduleRequest;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AutoSchedulingSolverTest {
    private final AutoSchedulingSolver solver = new AutoSchedulingSolver();

    @Test public void sameTeacherCannotShareSlot() {
        assertFalse(solve(list(session(1,"A",1,"G1"),session(2,"B",1,"G2")),
                list(room(1,60),room(2,60)), noneFixed(), nonePreferences(), list(slot(1,1))).isSuccess());
    }
    @Test public void sameRoomCannotShareSlot() {
        assertFalse(solve(list(session(1,"A",1,"G1"),session(2,"B",2,"G2")),
                list(room(1,60)), noneFixed(), nonePreferences(), list(slot(1,1))).isSuccess());
    }
    @Test public void sameStudentGroupCannotShareSlot() {
        assertFalse(solve(list(session(1,"A",1,"G1"),session(2,"B",2,"G1")),
                list(room(1,60),room(2,60)), noneFixed(), nonePreferences(), list(slot(1,1))).isSuccess());
    }
    @Test public void unavailableTimeIsHardConstraint() {
        assertFalse(solve(list(session(1,"A",1,"G1")),list(room(1,60)),noneFixed(),
                list(preference(1,1,1,TimePreferenceType.UNAVAILABLE)),list(slot(1,1))).isSuccess());
    }
    @Test public void insufficientCapacityExplainsFailure() {
        AutoSchedulePreviewDto result=solve(list(session(1,"A",1,"G1",80,null)),list(room(1,60)),noneFixed(),nonePreferences(),list(slot(1,1)));
        assertFalse(result.isSuccess());assertTrue(result.getExplanations().get(0).contains("容量"));
    }
    @Test public void incompatibleRoomTypeIsRejected() {
        AutoSchedulePreviewDto result=solve(list(session(1,"A",1,"G1",20,"LAB")),list(new AutoSchedulingSolver.Room(1,"教室","TEACHING",60)),noneFixed(),nonePreferences(),list(slot(1,1)));
        assertFalse(result.isSuccess());assertTrue(result.getExplanations().get(0).contains("LAB"));
    }
    @Test public void legalProblemProducesCompletePlan() {
        AutoSchedulePreviewDto result=solve(list(session(1,"A",1,"G1"),session(2,"B",2,"G2")),list(room(1,60)),noneFixed(),nonePreferences(),list(slot(1,1),slot(1,3)));
        assertTrue(result.isSuccess());assertEquals(2,result.getEntries().size());
    }
    @Test public void repeatedCourseSessionsPreferDifferentDays() {
        AutoSchedulingSolver.Session first=session(1,"A",1,"G1"), second=session(1,"A",1,"G1");
        AutoSchedulePreviewDto result=solve(list(first,second),list(room(1,60)),noneFixed(),nonePreferences(),list(slot(1,1),slot(1,3),slot(2,1)));
        assertTrue(result.isSuccess());assertNotEquals(result.getEntries().get(0).getWeekday(),result.getEntries().get(1).getWeekday());
    }
    @Test public void preferredTimeRanksBeforeOrdinaryTime() {
        AutoSchedulePreviewDto result=solve(list(session(1,"A",1,"G1")),list(room(1,60)),noneFixed(),
                list(preference(1,2,1,TimePreferenceType.PREFERRED)),list(slot(1,1),slot(2,1)));
        assertEquals(2,result.getEntries().get(0).getWeekday());
    }
    @Test public void avoidTimeHasHigherPenaltyThanOrdinaryTime() {
        AutoSchedulePreviewDto result=solve(list(session(1,"A",1,"G1")),list(room(1,60)),noneFixed(),
                list(preference(1,1,1,TimePreferenceType.AVOID)),list(slot(1,1),slot(2,1)));
        assertEquals(2,result.getEntries().get(0).getWeekday());
    }
    @Test public void saveValidationDetectsNewFixedConflict() {
        AutoSchedulingSolver.Problem problem=problem(list(session(1,"A",1,"G1")),list(room(1,60)),
                list(new AutoSchedulingSolver.Fixed(list(Long.valueOf(1)),list("G9"),2,slot(1,1))),nonePreferences(),list(slot(1,1)));
        List<String> errors=solver.validatePlan(problem,list(entry(1,1,1,1)));
        assertFalse(errors.isEmpty());
    }
    @Test public void previewNeverCallsRepositorySave() throws Exception {
        FakeRepository repository=new FakeRepository(problem(list(session(1,"A",1,"G1")),list(room(1,60)),noneFixed(),nonePreferences(),list(slot(1,1))));
        AutoSchedulingService service=new AutoSchedulingService(repository);
        assertTrue(service.preview(admin(),new AutoScheduleRequest(100)).isSuccess());assertEquals(0,repository.saved);
    }
    @Test public void confirmRevalidatesAndRejectsStalePreview() throws Exception {
        FakeRepository repository=new FakeRepository(problem(list(session(1,"A",1,"G1")),list(room(1,60)),
                list(new AutoSchedulingSolver.Fixed(list(Long.valueOf(1)),list("OTHER"),2,slot(1,1))),nonePreferences(),list(slot(1,1))));
        try { new AutoSchedulingService(repository).confirm(admin(),new AutoScheduleConfirmRequest(list(entry(1,1,1,1))));fail(); }
        catch(AcademicException expected){assertEquals(0,repository.saved);assertTrue(repository.locked);}
    }

    private AutoSchedulePreviewDto solve(List<AutoSchedulingSolver.Session>s,List<AutoSchedulingSolver.Room>r,List<AutoSchedulingSolver.Fixed>f,List<AutoSchedulingSolver.Preference>p,List<AutoSchedulingSolver.TimeSlot>t){return solver.solve(problem(s,r,f,p,t),100);}
    private static AutoSchedulingSolver.Problem problem(List<AutoSchedulingSolver.Session>s,List<AutoSchedulingSolver.Room>r,List<AutoSchedulingSolver.Fixed>f,List<AutoSchedulingSolver.Preference>p,List<AutoSchedulingSolver.TimeSlot>t){return new AutoSchedulingSolver.Problem(s,r,f,p,t);}
    private static AutoSchedulingSolver.Session session(long id,String name,long teacher,String group){return session(id,name,teacher,group,30,null);}
    private static AutoSchedulingSolver.Session session(long id,String name,long teacher,String group,int capacity,String type){return new AutoSchedulingSolver.Session(name+"-1",id,name,name,list(Long.valueOf(teacher)),"教师"+teacher,list(group),capacity,type);}
    private static AutoSchedulingSolver.Room room(long id,int capacity){return new AutoSchedulingSolver.Room(id,"教室"+id,"TEACHING",capacity);}
    private static AutoSchedulingSolver.TimeSlot slot(int day,int start){return new AutoSchedulingSolver.TimeSlot(day,start,start+1);}
    private static AutoSchedulingSolver.Preference preference(long teacher,int day,int start,TimePreferenceType type){return new AutoSchedulingSolver.Preference(teacher,slot(day,start),type);}
    private static AutoScheduleEntryDto entry(long course,long teacher,int day,long room){return new AutoScheduleEntryDto(course,"A","A",list(Long.valueOf(teacher)),"教师",list("G1"),day,1,2,room,"教室");}
    private static SessionContext admin(){return new SessionContext("admin-token",1,"admin","教务",EnumSet.of(Role.ACADEMIC_ADMIN),Role.ACADEMIC_ADMIN);}
    private static List<AutoSchedulingSolver.Fixed> noneFixed(){return Collections.emptyList();}
    private static List<AutoSchedulingSolver.Preference> nonePreferences(){return Collections.emptyList();}
    @SafeVarargs private static <T> List<T> list(T...values){List<T>r=new ArrayList<T>();Collections.addAll(r,values);return r;}

    private static final class FakeRepository implements SchedulingRepository {
        private final AutoSchedulingSolver.Problem problem;int saved;boolean locked;
        FakeRepository(AutoSchedulingSolver.Problem problem){this.problem=problem;}
        @Override public AutoSchedulingSolver.Problem loadProblem(Connection c){return problem;}
        @Override public SchedulingOverviewDto overview(Connection c){return new SchedulingOverviewDto(null,null);}
        @Override public TeacherTimePreferenceDto savePreference(Connection c,TeacherTimePreferenceDto v){return v;}
        @Override public boolean deletePreference(Connection c,long id){return true;}
        @Override public void lockSchedules(Connection c){locked=true;}
        @Override public int savePlan(Connection c,List<AutoScheduleEntryDto> entries){saved+=entries.size();return entries.size();}
    }
}
