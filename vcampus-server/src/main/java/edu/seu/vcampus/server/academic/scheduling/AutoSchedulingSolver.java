package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded CSP solver using MRV, backtracking, forward checking and soft penalties. */
public final class AutoSchedulingSolver {
    private static final int MAX_NODES = 200000;

    public AutoSchedulePreviewDto solve(Problem problem, int timeLimitMillis) {
        if (problem == null) throw new IllegalArgumentException("problem is required");
        if (problem.sessions.isEmpty()) return new AutoSchedulePreviewDto(true, 0,
                Collections.<AutoScheduleEntryDto>emptyList(),
                Collections.singletonList("当前没有待排课次。"));
        Search search = new Search(problem, Math.max(100, Math.min(15000, timeLimitMillis)));
        List<String> initial = search.initialErrors();
        if (!initial.isEmpty()) return new AutoSchedulePreviewDto(false, 0,
                Collections.<AutoScheduleEntryDto>emptyList(), initial);
        search.backtrack(0, 0);
        if (search.best == null) return new AutoSchedulePreviewDto(false, 0,
                Collections.<AutoScheduleEntryDto>emptyList(), asList(
                "自动排课未找到完整可行方案。",
                "主要限制：教师、教室或学生班级的可用时段组合不足，请调整约束或固定课表后重试。"));
        return new AutoSchedulePreviewDto(true, search.bestPenalty, toEntries(search.best),
                Collections.singletonList(search.timedOut
                        ? "已在时间限制内返回当前最佳合法方案。" : "已生成完整合法方案。"));
    }

    /** Revalidates a preview against the latest database snapshot before saving. */
    public List<String> validatePlan(Problem problem, List<AutoScheduleEntryDto> entries) {
        List<String> errors = new ArrayList<String>();
        if (problem == null) { errors.add("无法读取最新排课数据。"); return errors; }
        List<AutoScheduleEntryDto> values = entries == null
                ? Collections.<AutoScheduleEntryDto>emptyList() : entries;
        Map<Long, List<Session>> byCourse = new HashMap<Long, List<Session>>();
        for (Session session : problem.sessions) list(byCourse, session.courseId).add(session);
        Map<Long, Integer> used = new HashMap<Long, Integer>();
        Occupancy occupancy = new Occupancy(problem.fixed);
        for (AutoScheduleEntryDto value : values) {
            if (value.getWeekday() < 1 || value.getWeekday() > 7
                    || value.getStartPeriod() < 1
                    || value.getEndPeriod() < value.getStartPeriod()
                    || value.getEndPeriod() > 255 || value.getClassroomId() <= 0) {
                errors.add("课程 " + value.getCourseName() + " 的星期、节次或教室编号不正确。");
                continue;
            }
            List<Session> sessions = byCourse.get(Long.valueOf(value.getCourseId()));
            int index = used.containsKey(Long.valueOf(value.getCourseId()))
                    ? used.get(Long.valueOf(value.getCourseId())).intValue() : 0;
            used.put(Long.valueOf(value.getCourseId()), Integer.valueOf(index + 1));
            if (sessions == null || index >= sessions.size()) {
                errors.add("课程 " + value.getCourseCode() + " 的待排课次已变化。"); continue;
            }
            Session session = sessions.get(index);
            Room room = room(problem.rooms, value.getClassroomId());
            TimeSlot slot = new TimeSlot(value.getWeekday(), value.getStartPeriod(), value.getEndPeriod());
            if (room == null || room.capacity < session.requiredCapacity
                    || !compatible(session.requiredRoomType, room.type)) {
                errors.add("课程 " + session.courseName + " 的教室已不满足容量或类型要求。");
            } else if (unavailable(problem.preferences, session.teacherIds, slot)) {
                errors.add("课程 " + session.courseName + " 违反教师不可用时间。");
            } else {
                Candidate candidate = new Candidate(session, slot, room);
                if (!occupancy.available(candidate)) errors.add("课程 " + session.courseName
                        + " 与最新教师、教室或班级课表冲突。");
                else occupancy.add(candidate);
            }
        }
        for (Map.Entry<Long, List<Session>> expected : byCourse.entrySet()) {
            Integer actual = used.get(expected.getKey());
            if (actual == null || actual.intValue() != expected.getValue().size())
                errors.add("课程 " + expected.getValue().get(0).courseName + " 的预览课次数量已变化。");
        }
        return errors;
    }

    private static final class Search {
        private final Problem problem; private final long deadline; private final Occupancy busy;
        private final List<List<Candidate>> domains = new ArrayList<List<Candidate>>();
        private final boolean[] assigned; private final List<Candidate> current = new ArrayList<Candidate>();
        private final Map<Long, Set<Integer>> courseDays = new HashMap<Long, Set<Integer>>();
        private final Map<String, Integer> teacherLoads = new HashMap<String, Integer>();
        private List<Candidate> best; private int bestPenalty = Integer.MAX_VALUE;
        private int nodes; private boolean timedOut;

        Search(Problem problem, int millis) {
            this.problem = problem; deadline = System.currentTimeMillis() + millis;
            busy = new Occupancy(problem.fixed); assigned = new boolean[problem.sessions.size()];
            for (Session session : problem.sessions) domains.add(domain(session));
        }

        List<String> initialErrors() {
            List<String> errors = new ArrayList<String>();
            for (int i = 0; i < domains.size(); i++) {
                Session session = problem.sessions.get(i);
                if (session.teacherIds.isEmpty()) {
                    errors.add("未排课程：" + session.courseName + "（" + session.sessionKey + "）；尚未设置授课教师。");
                    continue;
                }
                if (!domains.get(i).isEmpty()) continue;
                boolean capacity = false, type = false;
                for (Room room : problem.rooms) {
                    if (room.capacity >= session.requiredCapacity) capacity = true;
                    if (room.capacity >= session.requiredCapacity && compatible(session.requiredRoomType, room.type)) type = true;
                }
                String reason = !capacity ? "没有容量 ≥ " + session.requiredCapacity + " 的可用教室。"
                        : !type ? "没有满足 " + session.requiredRoomType + " 类型要求的可用教室。"
                        : "教师可用时段过少或均与固定课表冲突。";
                errors.add("未排课程：" + session.courseName + "（" + session.sessionKey + "）；" + reason);
            }
            return errors;
        }

        void backtrack(int depth, int penalty) {
            if (System.currentTimeMillis() >= deadline || nodes++ >= MAX_NODES) { timedOut = true; return; }
            if (depth == assigned.length) {
                if (penalty < bestPenalty) { bestPenalty = penalty; best = new ArrayList<Candidate>(current); }
                return;
            }
            if (best != null && penalty >= bestPenalty) return;
            int selected = mrv(); if (selected < 0) return;
            List<Candidate> values = feasible(selected);
            Collections.sort(values, new Comparator<Candidate>() {
                @Override public int compare(Candidate a, Candidate b) { return Integer.compare(penalty(a), penalty(b)); }
            });
            assigned[selected] = true;
            for (Candidate value : values) {
                int added = penalty(value); add(value);
                if (forwardCheck()) backtrack(depth + 1, penalty + added);
                remove(value); if (timedOut && best != null) break;
            }
            assigned[selected] = false;
        }

        private int mrv() {
            int selected = -1, minimum = Integer.MAX_VALUE;
            for (int i = 0; i < assigned.length; i++) if (!assigned[i]) {
                int size = feasible(i).size();
                if (size < minimum) { minimum = size; selected = i; if (size == 0) break; }
            }
            return selected;
        }

        private boolean forwardCheck() {
            for (int i = 0; i < assigned.length; i++) if (!assigned[i] && feasible(i).isEmpty()) return false;
            return true;
        }

        private List<Candidate> feasible(int index) {
            List<Candidate> result = new ArrayList<Candidate>();
            for (Candidate value : domains.get(index)) if (busy.available(value)) result.add(value);
            return result;
        }

        private List<Candidate> domain(Session session) {
            List<Candidate> result = new ArrayList<Candidate>();
            for (TimeSlot slot : problem.timeSlots) if (!unavailable(problem.preferences, session.teacherIds, slot))
                for (Room room : problem.rooms) if (room.capacity >= session.requiredCapacity
                        && compatible(session.requiredRoomType, room.type)) {
                    Candidate value = new Candidate(session, slot, room);
                    if (busy.available(value)) result.add(value);
                }
            return result;
        }

        private int penalty(Candidate value) {
            int result = 10;
            for (Preference preference : problem.preferences)
                if (value.session.teacherIds.contains(Long.valueOf(preference.teacherId))
                        && preference.slot.overlaps(value.slot)) {
                    if (preference.type == TimePreferenceType.PREFERRED) result -= 10;
                    if (preference.type == TimePreferenceType.AVOID) result += 40;
                }
            Set<Integer> days = courseDays.get(Long.valueOf(value.session.courseId));
            if (days != null && days.contains(Integer.valueOf(value.slot.weekday))) result += 25;
            for (Long teacher : value.session.teacherIds) {
                String key = teacher + ":" + value.slot.weekday;
                Integer load = teacherLoads.get(key);
                if (load != null && load.intValue() >= 3) result += 15 * (load.intValue() - 2);
                for (Candidate old : current) if (old.slot.weekday == value.slot.weekday
                        && old.session.teacherIds.contains(teacher)) {
                    int gap = Math.max(old.slot.startPeriod, value.slot.startPeriod)
                            - Math.min(old.slot.endPeriod, value.slot.endPeriod) - 1;
                    if (gap > 1) result += (gap - 1) * 3;
                }
            }
            for (Candidate old : current) if (old.session.courseId == value.session.courseId
                    && old.room.id != value.room.id) result += 2;
            return Math.max(0, result);
        }

        private void add(Candidate value) {
            current.add(value); busy.add(value);
            Set<Integer> days = courseDays.get(Long.valueOf(value.session.courseId));
            if (days == null) { days = new HashSet<Integer>(); courseDays.put(Long.valueOf(value.session.courseId), days); }
            days.add(Integer.valueOf(value.slot.weekday));
            for (Long teacher : value.session.teacherIds) load(teacher, value.slot.weekday, 1);
        }

        private void remove(Candidate value) {
            current.remove(current.size() - 1); busy.remove(value);
            Set<Integer> days = courseDays.get(Long.valueOf(value.session.courseId));
            boolean sameDay = false;
            for (Candidate old : current) if (old.session.courseId == value.session.courseId
                    && old.slot.weekday == value.slot.weekday) sameDay = true;
            if (!sameDay && days != null) days.remove(Integer.valueOf(value.slot.weekday));
            for (Long teacher : value.session.teacherIds) load(teacher, value.slot.weekday, -1);
        }

        private void load(Long teacher, int day, int delta) {
            String key = teacher + ":" + day; Integer old = teacherLoads.get(key);
            int value = (old == null ? 0 : old.intValue()) + delta;
            if (value == 0) teacherLoads.remove(key); else teacherLoads.put(key, Integer.valueOf(value));
        }
    }

    private static final class Occupancy {
        private final Set<String> teachers = new HashSet<String>();
        private final Set<String> rooms = new HashSet<String>();
        private final Set<String> groups = new HashSet<String>();
        Occupancy(List<Fixed> fixed) { for (Fixed value : fixed) mark(value.teacherIds, value.groups, value.roomId, value.slot, true); }
        boolean available(Candidate value) { return free(teachers, value.session.teacherIds, value.slot)
                && free(groups, value.session.groups, value.slot)
                && free(rooms, Collections.singletonList(Long.valueOf(value.room.id)), value.slot); }
        void add(Candidate value) { mark(value.session.teacherIds, value.session.groups, value.room.id, value.slot, true); }
        void remove(Candidate value) { mark(value.session.teacherIds, value.session.groups, value.room.id, value.slot, false); }
        private void mark(List<Long> t, List<String> g, long room, TimeSlot slot, boolean add) {
            mark(teachers, t, slot, add); mark(groups, g, slot, add);
            mark(rooms, Collections.singletonList(Long.valueOf(room)), slot, add);
        }
        private static boolean free(Set<String> busy, List<?> ids, TimeSlot slot) {
            for (Object id : ids) for (int p = slot.startPeriod; p <= slot.endPeriod; p++)
                if (busy.contains(key(id, slot.weekday, p))) return false;
            return true;
        }
        private static void mark(Set<String> busy, List<?> ids, TimeSlot slot, boolean add) {
            for (Object id : ids) for (int p = slot.startPeriod; p <= slot.endPeriod; p++) {
                String key = key(id, slot.weekday, p); if (add) busy.add(key); else busy.remove(key);
            }
        }
        private static String key(Object id, int day, int period) { return id + ":" + day + ":" + period; }
    }

    public static final class Problem {
        final List<Session> sessions; final List<Room> rooms; final List<Fixed> fixed;
        final List<Preference> preferences; final List<TimeSlot> timeSlots;
        public Problem(List<Session> sessions, List<Room> rooms, List<Fixed> fixed,
                List<Preference> preferences, List<TimeSlot> timeSlots) {
            this.sessions = copy(sessions); this.rooms = copy(rooms); this.fixed = copy(fixed);
            this.preferences = copy(preferences); this.timeSlots = copy(timeSlots);
        }
    }
    public static final class Session {
        final String sessionKey; final long courseId; final String courseCode, courseName;
        final List<Long> teacherIds; final String teacherNames; final List<String> groups;
        final int requiredCapacity; final String requiredRoomType;
        public Session(String key, long id, String code, String name, List<Long> teachers,
                String teacherNames, List<String> groups, int capacity, String roomType) {
            sessionKey = key; courseId = id; courseCode = code; courseName = name;
            teacherIds = copy(teachers); this.teacherNames = teacherNames; this.groups = copy(groups);
            requiredCapacity = capacity; requiredRoomType = roomType;
        }
    }
    public static final class Room {
        final long id; final String name, type; final int capacity;
        public Room(long id, String name, String type, int capacity) { this.id=id; this.name=name; this.type=type; this.capacity=capacity; }
    }
    public static final class TimeSlot {
        final int weekday, startPeriod, endPeriod;
        public TimeSlot(int day, int start, int end) { weekday=day; startPeriod=start; endPeriod=end; }
        boolean overlaps(TimeSlot other) { return weekday == other.weekday && startPeriod <= other.endPeriod && other.startPeriod <= endPeriod; }
    }
    public static final class Fixed {
        final List<Long> teacherIds; final List<String> groups; final long roomId; final TimeSlot slot;
        public Fixed(List<Long> teachers, List<String> groups, long room, TimeSlot slot) {
            teacherIds=copy(teachers); this.groups=copy(groups); roomId=room; this.slot=slot;
        }
    }
    public static final class Preference {
        final long teacherId; final TimeSlot slot; final TimePreferenceType type;
        public Preference(long teacher, TimeSlot slot, TimePreferenceType type) { teacherId=teacher; this.slot=slot; this.type=type; }
    }
    private static final class Candidate {
        final Session session; final TimeSlot slot; final Room room;
        Candidate(Session session, TimeSlot slot, Room room) { this.session=session; this.slot=slot; this.room=room; }
    }

    private static <T> List<T> copy(List<T> values) { return values == null ? new ArrayList<T>() : new ArrayList<T>(values); }
    private static <T> List<T> list(Map<Long,List<T>> map, long key) {
        List<T> value = map.get(Long.valueOf(key));
        if (value == null) { value = new ArrayList<T>(); map.put(Long.valueOf(key), value); }
        return value;
    }
    private static Room room(List<Room> rooms, long id) { for (Room value : rooms) if (value.id == id) return value; return null; }
    private static boolean compatible(String required, String actual) { return required == null || required.trim().isEmpty() || required.equalsIgnoreCase(actual); }
    private static boolean unavailable(List<Preference> values, List<Long> teachers, TimeSlot slot) {
        for (Preference value : values) if (value.type == TimePreferenceType.UNAVAILABLE
                && teachers.contains(Long.valueOf(value.teacherId)) && value.slot.overlaps(slot)) return true;
        return false;
    }
    private static List<AutoScheduleEntryDto> toEntries(List<Candidate> values) {
        List<AutoScheduleEntryDto> result = new ArrayList<AutoScheduleEntryDto>();
        for (Candidate value : values) result.add(new AutoScheduleEntryDto(value.session.courseId,
                value.session.courseCode, value.session.courseName, value.session.teacherIds,
                value.session.teacherNames, value.session.groups, value.slot.weekday,
                value.slot.startPeriod, value.slot.endPeriod, value.room.id, value.room.name));
        Collections.sort(result, new Comparator<AutoScheduleEntryDto>() {
            @Override public int compare(AutoScheduleEntryDto a, AutoScheduleEntryDto b) {
                int day = Integer.compare(a.getWeekday(), b.getWeekday());
                return day != 0 ? day : Integer.compare(a.getStartPeriod(), b.getStartPeriod());
            }
        });
        return result;
    }
    private static List<String> asList(String a, String b) { List<String> r=new ArrayList<String>(); r.add(a); r.add(b); return r; }
}
