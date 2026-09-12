package edu.seu.vcampus.server.integration;

import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.server.campus.repository.mysql.MySqlCampusClassroomRepository;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.dorm.repository.mysql.MySqlDormBillingRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** 使用已导入的演示数据，姓名修改仅在本事务中可见，结束时回滚。 */
public class MySqlPersonDisplayIntegrationTest {
    @Test public void reservationAndBillNamesFollowCurrentUserAndSupportSearch() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        try (Connection connection = new JdbcConnectionFactory().open()) {
            connection.setAutoCommit(false);
            try {
                MySqlCampusClassroomRepository campus = new MySqlCampusClassroomRepository();
                ClassroomReservationDto reservation = campus.listReservations(connection,null,CampusPageQuery.all()).getItems().get(0);
                assertNotNull(reservation.getApplicantLabel());
                rename(connection,reservation.getApplicantId(),"申请人同步核验");
                assertTrue(campus.findReservation(connection,reservation.getId()).getApplicantLabel().startsWith("申请人同步核验（"));
                assertFalse(campus.listReservations(connection,null,new CampusPageQuery(1,20,"申请人同步核验",null)).getItems().isEmpty());
                MySqlDormBillingRepository dorm = new MySqlDormBillingRepository();
                UtilityBillDto bill = dorm.listAllBills(connection,UtilityBillQuery.all()).getItems().get(0);
                assertNotNull(bill.getStudentLabel());
                rename(connection,bill.getStudentUserId(),"账单住户同步核验");
                UtilityBillDto searched = dorm.listAllBills(connection,new UtilityBillQuery(1,20,"账单住户同步核验",null,null,null,null)).getItems().get(0);
                assertTrue(searched.getStudentLabel().startsWith("账单住户同步核验（"));
            } finally { connection.rollback(); }
        }
    }
    private static void rename(Connection c,long user,String name) throws Exception {
        try (PreparedStatement statement = c.prepareStatement("UPDATE users SET display_name=? WHERE id=?")) {
            statement.setString(1,name);statement.setLong(2,user);assertEquals(1,statement.executeUpdate());
        }
    }
}
