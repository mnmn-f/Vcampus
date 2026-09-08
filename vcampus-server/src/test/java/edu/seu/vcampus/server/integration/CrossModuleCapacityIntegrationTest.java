package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.EnrollmentRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderStatus;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 通过真实 CommandRouter 并发验证选课容量和商店支付库存的一致性。 */
public final class CrossModuleCapacityIntegrationTest {
    private IntegrationFixture fixture;

    @Before
    public void setUp() {
        fixture = new IntegrationFixture();
    }

    @Test
    public void concurrentEnrollmentCannotExceedCourseCapacity() throws Exception {
        SessionContext academic = fixture.session(10L, Role.ACADEMIC_ADMIN);
        CourseDto course = fixture.academicService.createCourse(academic,
                CourseSaveRequest.create("CAP-1", "容量测试", CourseType.ELECTIVE,
                        BigDecimal.ONE, 16, 1, "并发", CourseStatus.PUBLISHED,
                        Collections.singletonList(11L)));
        SessionContext first = fixture.session(1L, Role.STUDENT);
        SessionContext second = fixture.session(2L, Role.STUDENT);
        List<Message> responses = concurrently(
                request(AcademicCommands.STUDENT_ENROLL, first,
                        new EnrollmentRequest(course.getId())),
                request(AcademicCommands.STUDENT_ENROLL, second,
                        new EnrollmentRequest(course.getId())));

        int successes = countSuccess(responses);
        assertEquals(1, successes);
        assertEquals(1L, fixture.academicRepository.countEnrolled(null, course.getId()));
        for (Message response : responses) {
            assertTrue(response.isSuccess()
                    || AcademicCommands.COURSE_CAPACITY_FULL.equals(response.getResultCode()));
        }
    }

    @Test
    public void concurrentPaymentsCannotOversellOrDoubleDebit() throws Exception {
        SessionContext first = fixture.session(1L, Role.STUDENT);
        SessionContext second = fixture.session(2L, Role.STUDENT);
        addCart(first);
        addCart(second);
        OrderDto firstOrder = createOrder(first);
        OrderDto secondOrder = createOrder(second);

        List<Message> responses = concurrently(
                request(StoreCommands.ORDER_PAY, first,
                        new PaymentRequest(firstOrder.getId(), "pay-1")),
                request(StoreCommands.ORDER_PAY, second,
                        new PaymentRequest(secondOrder.getId(), "pay-2")));

        assertEquals(1, countSuccess(responses));
        assertEquals(0, fixture.store.getStock(1L));
        BigDecimal firstBalance = fixture.store.getAccount(1L).getBalance();
        BigDecimal secondBalance = fixture.store.getAccount(2L).getBalance();
        assertEquals(new BigDecimal("195.00"), firstBalance.add(secondBalance));
        assertTrue(new BigDecimal("95.00").equals(firstBalance)
                || new BigDecimal("95.00").equals(secondBalance));
        assertTrue(OrderStatus.PAID.name().equals(fixture.store.getOrder(firstOrder.getId()).getStatus())
                || OrderStatus.PAID.name().equals(fixture.store.getOrder(secondOrder.getId()).getStatus()));
        for (Message response : responses) {
            assertTrue(response.isSuccess() || ResultCodes.CONFLICT.equals(response.getResultCode()));
        }
    }

    private void addCart(SessionContext session) {
        Message response = route(StoreCommands.CART_ADD_ITEM, session,
                new CartItemRequest(1L, 1));
        assertTrue(response.isSuccess());
    }

    private OrderDto createOrder(SessionContext session) {
        Message response = route(StoreCommands.ORDER_CREATE, session, null);
        assertTrue(response.isSuccess());
        return (OrderDto) response.getPayload();
    }

    private Message route(String command, SessionContext session, java.io.Serializable payload) {
        return fixture.router.route(Message.request(command, session.getSessionToken(), payload));
    }

    private Callable<Message> request(final String command, final SessionContext session,
                                      final java.io.Serializable payload) {
        return new Callable<Message>() {
            @Override
            public Message call() {
                return fixture.router.route(Message.request(command,
                        session.getSessionToken(), payload));
            }
        };
    }

    private List<Message> concurrently(Callable<Message> first,
                                        Callable<Message> second) throws Exception {
        final CountDownLatch ready = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Message>> futures = new ArrayList<Future<Message>>();
            futures.add(executor.submit(await(ready, first)));
            futures.add(executor.submit(await(ready, second)));
            ready.countDown();
            List<Message> result = new ArrayList<Message>();
            for (Future<Message> future : futures) result.add(future.get(5, TimeUnit.SECONDS));
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<Message> await(final CountDownLatch ready,
                                            final Callable<Message> request) {
        return new Callable<Message>() {
            @Override
            public Message call() throws Exception {
                ready.await(5, TimeUnit.SECONDS);
                return request.call();
            }
        };
    }

    private static int countSuccess(List<Message> responses) {
        int count = 0;
        for (Message response : responses) if (response.isSuccess()) count++;
        return count;
    }
}
