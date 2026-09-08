package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.academic.registry.AcademicCommandRegistry;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.campus.registry.CampusCommandRegistry;
import edu.seu.vcampus.server.campus.repository.InMemoryCampusRepository;
import edu.seu.vcampus.server.campus.service.CampusService;
import edu.seu.vcampus.server.dorm.registry.DormCommandRegistry;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormRepository;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.identity.registry.IdentityCommandRegistry;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRecordRepository;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.library.registry.LibraryCommandRegistry;
import edu.seu.vcampus.server.library.repository.InMemoryBookRepository;
import edu.seu.vcampus.server.library.repository.InMemoryBorrowRepository;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomRepository;
import edu.seu.vcampus.server.library.repository.InMemoryStudyRoomReservationRepository;
import edu.seu.vcampus.server.library.service.LibraryService;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import edu.seu.vcampus.server.student.registry.StudentCommandRegistry;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import edu.seu.vcampus.server.store.registry.StoreCommandRegistry;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.service.InMemoryStoreTransactionRunner;
import edu.seu.vcampus.server.store.service.StoreService;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 只供跨模块集成测试使用的全内存真实命令装配。 */
public final class IntegrationFixture {
    public static final String MULTI_ACCOUNT = "multi";
    public static final String MULTI_PASSWORD = "Secret1!";

    public final PasswordHasher hasher = new PasswordHasher(4);
    public final SessionManager sessions = new SessionManager();
    public final InMemoryIdentityRecordRepository identity;
    public final InMemoryStudentRecordRepository students = new InMemoryStudentRecordRepository();
    public final InMemoryAcademicRepository academicRepository = new InMemoryAcademicRepository();
    public final InMemoryBookRepository books = new InMemoryBookRepository();
    public final InMemoryBorrowRepository borrowings = new InMemoryBorrowRepository();
    public final InMemoryStudyRoomRepository studyRooms = new InMemoryStudyRoomRepository();
    public final InMemoryStudyRoomReservationRepository reservations =
            new InMemoryStudyRoomReservationRepository();
    public final InMemoryOnlineResourceRepository resources = new InMemoryOnlineResourceRepository();
    public final InMemoryStoreRecordRepository store = new InMemoryStoreRecordRepository();
    public final InMemoryDormRepository dorm = new InMemoryDormRepository();
    public final InMemoryCampusRepository campus = new InMemoryCampusRepository();
    public final StudentRecordService studentService;
    public final AcademicService academicService;
    public final LibraryService libraryService;
    public final StoreService storeService;
    public final DormService dormService;
    public final CampusService campusService;
    public final IdentityService identityService;
    public final CommandRouter router;

    public IntegrationFixture() {
        identity = new InMemoryIdentityRecordRepository(new InMemoryUserRepository(), sessions);
        seedUsers();
        seedModules();
        studentService = new StudentRecordService(students, new StudentTransactions(students));
        academicService = new AcademicService(academicRepository);
        libraryService = new LibraryService(books, borrowings, studyRooms, reservations,
                resources, null);
        storeService = new StoreService(store, new InMemoryStoreTransactionRunner(store));
        dormService = new DormService(dorm);
        campusService = new CampusService(campus);
        identityService = IdentityCommandRegistry.createInMemoryService(identity, hasher);
        router = ServerMain.createRouter(identity.getAuthRepository(), hasher, sessions);
        registerModules();
    }

    public SessionContext session(long userId, Role role) {
        return sessions.createSession(userId, "user" + userId, "用户" + userId,
                Collections.singleton(role), role);
    }

    private void seedUsers() {
        seedUser(1L, MULTI_ACCOUNT, MULTI_PASSWORD, "多角色用户",
                EnumSet.of(Role.STUDENT, Role.STORE_MANAGER, Role.SYSTEM_ADMIN));
        seedUser(2L, "student2", MULTI_PASSWORD, "学生二", EnumSet.of(Role.STUDENT));
        seedUser(3L, "student3", MULTI_PASSWORD, "学生三", EnumSet.of(Role.STUDENT));
        seedUser(9L, "sysadmin", MULTI_PASSWORD, "系统管理员", EnumSet.of(Role.SYSTEM_ADMIN));
        seedUser(10L, "academic", MULTI_PASSWORD, "教务管理员", EnumSet.of(Role.ACADEMIC_ADMIN));
        seedUser(11L, "teacher", MULTI_PASSWORD, "任课教师", EnumSet.of(Role.TEACHER));
        students.addUser(1L, MULTI_ACCOUNT, "多角色用户");
        students.addUser(2L, "student2", "学生二");
        students.addUser(3L, "student3", "学生三");
    }

    private void seedModules() {
        academicRepository.addActiveStudent(1L);
        academicRepository.addActiveStudent(2L);
        academicRepository.addActiveStudent(3L);
        academicRepository.addActiveTeacher(11L, "任课教师");
        store.addProduct(new ProductDto(1L, "SKU-1", "测试商品", "测试",
                new BigDecimal("5.00"), 1, "ON_SALE"));
        store.addAccount(1L, new BigDecimal("100.00"));
        store.addAccount(2L, new BigDecimal("100.00"));
        store.addAccount(3L, new BigDecimal("100.00"));
        store.addOrder(new OrderDto(41L, "ORDER-1", 1L, BigDecimal.ZERO,
                "CREATED", Collections.<OrderItemDto>emptyList()));
        store.addOrder(new OrderDto(42L, "ORDER-2", 2L, BigDecimal.ZERO,
                "CREATED", Collections.<OrderItemDto>emptyList()));
    }

    private void registerModules() {
        StudentCommandRegistry.registerAll(router, studentService);
        AcademicCommandRegistry.registerAll(router, academicService);
        LibraryCommandRegistry.registerAll(router, libraryService);
        StoreCommandRegistry.registerAll(router, storeService);
        DormCommandRegistry.registerAll(router, dormService);
        CampusCommandRegistry.registerAll(router, campusService);
        IdentityCommandRegistry.registerAll(router, identityService);
    }

    private void seedUser(long id, String account, String password, String name,
                          Set<Role> roles) {
        identity.addUser(new IdentityUserRecord(id, account, hasher.hash(password), name,
                null, null, null, "ACTIVE", roles, LocalDateTime.now(),
                LocalDateTime.now(), null));
    }

    private static final class StudentTransactions implements StudentTransactionRunner {
        private final Object lock;

        private StudentTransactions(Object lock) {
            this.lock = lock;
        }

        @Override
        public <T> T execute(edu.seu.vcampus.server.db.TransactionWork<T> work)
                throws Exception {
            synchronized (lock) {
                return work.execute(null);
            }
        }
    }
}
