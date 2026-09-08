package edu.seu.vcampus.common.protocol.io;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.PriorityQueue;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 协议流共享白名单、连接累计预算和 JDK 容器边界。 */
public final class SafeObjectInputStreamTest {
    @Test
    public void protocolDtosAndLoginRoleSetAreAllowed() throws Exception {
        Message request = Message.request("auth.login", null,
                new LoginRequest("student", "secret"));
        LoginResult result = new LoginResult(7L, "student", "学生",
                EnumSet.of(Role.STUDENT, Role.TEACHER), Role.STUDENT, "token");
        byte[] data = serialize(request, Message.success(request, result));
        ObjectInputStream input = new SafeObjectInputStream(
                new ByteArrayInputStream(data));
        assertEquals(request.getCommand(), ((Message) input.readObject()).getCommand());
        Message response = (Message) input.readObject();
        assertEquals(Role.STUDENT, ((LoginResult) response.getPayload()).getActiveRole());
    }

    @Test
    public void nonProtocolPriorityQueueIsRejected() throws Exception {
        byte[] data = serialize(new PriorityQueue<String>());
        try {
            new SafeObjectInputStream(new ByteArrayInputStream(data)).readObject();
            fail("PriorityQueue must not be accepted by the protocol stream");
        } catch (InvalidClassException expected) {
            assertTrue(expected.getMessage().contains("PriorityQueue"));
        }
    }

    @Test
    public void approvedValueTypesAndContainersAreAllowed() throws Exception {
        ArrayList<String> values = new ArrayList<String>();
        values.add("value");
        byte[] data = serialize(new BigDecimal("1.25"), LocalDate.of(2026, 8, 29),
                LocalDateTime.of(2026, 8, 29, 12, 30), LocalTime.of(12, 30),
                Collections.unmodifiableList(values), UUID.randomUUID());
        ObjectInputStream input = new SafeObjectInputStream(
                new ByteArrayInputStream(data));
        assertEquals(new BigDecimal("1.25"), input.readObject());
        assertEquals(LocalDate.of(2026, 8, 29), input.readObject());
        assertEquals(LocalDateTime.of(2026, 8, 29, 12, 30), input.readObject());
        assertEquals(LocalTime.of(12, 30), input.readObject());
        assertEquals(Collections.singletonList("value"), input.readObject());
        assertTrue(input.readObject() instanceof UUID);
    }

    @Test
    public void byteBudgetIsCumulativeAcrossObjects() throws Exception {
        byte[] data = serialize("first", "second");
        SafeObjectInputStream input = new SafeObjectInputStream(
                new ByteArrayInputStream(data), data.length - 1L);
        assertEquals("first", input.readObject());
        try {
            input.readObject();
            fail("the second object should exceed the cumulative connection budget");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("size limit"));
        }
    }

    private static byte[] serialize(Object... values) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        for (Object value : values) output.writeObject(value);
        output.flush();
        return bytes.toByteArray();
    }
}
