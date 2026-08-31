package edu.seu.vcampus.server.student.repository;

import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 学籍服务测试桩；只在内存中保存用户和档案，不模拟 JDBC。 */
final class InMemoryStudentProfileRepository
        implements DelegatingStudentRecordRepository.ProfileStore {
    private final Map<Long, String> accounts = new HashMap<Long, String>();
    private final Map<Long, String> names = new HashMap<Long, String>();
    private final Map<Long, StudentProfileDto> profiles =
            new HashMap<Long, StudentProfileDto>();

    void addUser(long userId, String account, String displayName) {
        accounts.put(userId, account);
        names.put(userId, displayName);
    }

    @Override
    public StudentProfileDto find(Connection ignored, long userId) {
        return profiles.get(userId);
    }

    @Override
    public StudentProfilePage search(Connection ignored, StudentProfileQuery query) {
        List<StudentProfileDto> rows = new ArrayList<StudentProfileDto>();
        for (StudentProfileDto profile : profiles.values()) {
            if (matches(profile, query)) rows.add(profile);
        }
        Collections.sort(rows, new Comparator<StudentProfileDto>() {
            @Override
            public int compare(StudentProfileDto left, StudentProfileDto right) {
                return left.getStudentNo().compareTo(right.getStudentNo());
            }
        });
        long total = rows.size();
        int from = Math.min(query.getOffset(), rows.size());
        int to = Math.min(from + query.getPageSize(), rows.size());
        return new StudentProfilePage(rows.subList(from, to), total,
                query.getPage(), query.getPageSize());
    }

    @Override
    public boolean userExists(Connection ignored, long userId) {
        return accounts.containsKey(userId);
    }

    @Override
    public boolean profileExists(Connection ignored, long userId) {
        return profiles.containsKey(userId);
    }

    @Override
    public boolean studentNoExists(Connection ignored, String studentNo, long excludedUserId) {
        String key = normalize(studentNo);
        for (StudentProfileDto profile : profiles.values()) {
            if (profile.getUserId() != excludedUserId
                    && key.equals(normalize(profile.getStudentNo()))) return true;
        }
        return false;
    }

    @Override
    public void insert(Connection ignored, StudentProfileWriteRequest request) {
        profiles.put(request.getUserId(), toDto(request));
    }

    @Override
    public void update(Connection ignored, StudentProfileWriteRequest request) {
        profiles.put(request.getUserId(), toDto(request));
    }

    private StudentProfileDto toDto(StudentProfileWriteRequest request) {
        long id = request.getUserId();
        return new StudentProfileDto(id, names.get(id), accounts.get(id), request.getStudentNo(),
                request.getCollege(), request.getMajor(), request.getClassName(),
                request.getEnrollmentYear(), request.getExpectedGraduationYear(),
                request.getDegreeLevel(), request.getGender(), request.getBirthDate(),
                request.getAddress(), request.getEmergencyContact(),
                request.getEmergencyPhone(), request.getStatus());
    }

    private static boolean matches(StudentProfileDto profile, StudentProfileQuery query) {
        return contains(profile.getStudentNo(), query.getStudentNo())
                && contains(profile.getDisplayName(), query.getDisplayName())
                && contains(profile.getCollege(), query.getCollege())
                && contains(profile.getMajor(), query.getMajor())
                && contains(profile.getClassName(), query.getClassName())
                && (query.getStatus() == null || query.getStatus() == profile.getStatus());
    }

    private static boolean contains(String value, String expected) {
        return expected == null || (value != null
                && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
