package edu.seu.vcampus.server.academic.scheduling;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the repository query boundary without changing the user's database. */
public final class MySqlSchedulingSemesterTest {
    @Test public void missingSemesterNeverLoadsAllCourses() throws Exception {
        List<String> queries = new ArrayList<String>();
        AutoSchedulingSolver.Problem result = new MySqlSchedulingRepository()
                .loadProblem(connection(queries, new ArrayList<String>()), null);
        assertTrue(result.sessions.isEmpty());
        assertFalse(queries.stream().anyMatch(sql -> sql.startsWith("SELECT id,course_code")));
    }

    @Test public void explicitSemesterIsBoundToCourseQuery() throws Exception {
        List<String> queries = new ArrayList<String>();
        List<String> bindings = new ArrayList<String>();
        new MySqlSchedulingRepository().loadProblem(connection(queries, bindings), " 2026-FALL ");
        assertTrue(queries.stream().anyMatch(sql -> sql.startsWith("SELECT id,course_code")
                && sql.contains("AND semester_code=?")));
        assertEquals(java.util.Collections.singletonList("2026-FALL"), bindings);
    }

    private static Connection connection(List<String> queries, List<String> bindings) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        queries.add((String) args[0]);
                        return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                new Class<?>[]{PreparedStatement.class}, (statement, operation, values) -> {
                                    if (operation.getName().equals("setString")) bindings.add((String) values[1]);
                                    if (operation.getName().equals("executeQuery")) {
                                        return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                                                new Class<?>[]{ResultSet.class}, (rows, call, parameters) -> {
                                                    if (call.getName().equals("next")) return false;
                                                    return null;
                                                });
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
    }
}
