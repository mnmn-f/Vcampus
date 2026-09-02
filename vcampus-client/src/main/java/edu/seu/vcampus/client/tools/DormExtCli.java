package edu.seu.vcampus.client.tools;

import edu.seu.vcampus.client.auth.NetworkAuthClientService;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/**
 * 宿舍扩展命令的命令行验证工具。
 *
 * <p>扩展命令（抄表、出账）还没有界面入口，但需要在真实数据库上验证闭环，
 * 因此提供这个只读写协议、不含任何业务逻辑的小工具：它像普通客户端一样登录、
 * 发命令、打印结果，走的是和 Swing 客户端完全相同的 Socket 与鉴权路径。</p>
 *
 * <p>用法（服务端需以 DormServerMain 启动）：</p>
 * <pre>
 * java -cp vcampus-client\target\vCampusClient.jar edu.seu.vcampus.client.tools.DormExtCli status
 * java -cp ... DormExtCli meters
 * java -cp ... DormExtCli meter 1 2026-09-01 2026-09-30 120.5 8.0 0.60 3.50
 * java -cp ... DormExtCli bill 2026-09-01 2026-09-30
 * java -cp ... DormExtCli bill 2026-09-01 2026-09-30 1
 * </pre>
 *
 * <p>可用系统属性覆盖：{@code vcampus.server.host}、{@code vcampus.server.port}、
 * {@code vcampus.cli.user}、{@code vcampus.cli.password}。默认以 demo_dorm 登录。</p>
 */
public final class DormExtCli {
    private DormExtCli() { }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            usage();
            return;
        }
        String host = System.getProperty("vcampus.server.host", "127.0.0.1").trim();
        int port = Integer.parseInt(System.getProperty("vcampus.server.port", "8888").trim());
        String account = System.getProperty("vcampus.cli.user", "demo_dorm").trim();
        String password = System.getProperty("vcampus.cli.password", "dorm123");

        SocketClientGateway gateway = new SocketClientGateway(host, port, 5000, 15000);
        NetworkClientService network = new NetworkClientService(gateway);
        NetworkAuthClientService auth = new NetworkAuthClientService(gateway);
        try {
            LoginResult login = auth.login(account, password);
            network.setSessionToken(login.getSessionToken());
            System.out.println("已登录：" + login.getDisplayName() + " / 职责 "
                    + login.getActiveRole().getDisplayName());
            dispatch(network, args);
            auth.logout();
        } catch (Exception ex) {
            System.out.println("失败：" + ex.getMessage());
        } finally {
            network.close();
        }
    }

    private static void dispatch(NetworkClientService network, String[] args) throws Exception {
        String action = args[0].trim();
        if ("status".equals(action)) {
            status(network);
        } else if ("meters".equals(action)) {
            meters(network);
        } else if ("meter".equals(action)) {
            meter(network, args);
        } else if ("bill".equals(action)) {
            bill(network, args);
        } else if ("access".equals(action)) {
            access(network, args);
        } else if ("scan".equals(action)) {
            scan(network, args);
        } else if ("warnings".equals(action)) {
            warnings(network);
        } else if ("notify".equals(action)) {
            notify(network, args);
        } else if ("verify".equals(action)) {
            verify(network, args);
        } else if ("config".equals(action)) {
            config(network, args);
        } else {
            usage();
        }
    }

    private static void status(NetworkClientService network) throws Exception {
        Message response = network.request(DormExtCommands.STATUS, null);
        DormExtStatusDto status = (DormExtStatusDto) response.getPayload();
        System.out.println("模块版本  " + status.getModuleVersion());
        System.out.println("调度器    " + (status.isSchedulerRunning() ? "运行中" : "未启动"));
        System.out.println("服务端时间 " + status.getServerTime());
        if (status.getScheduledTasks().isEmpty()) {
            System.out.println("定时任务  （未接入）");
        } else {
            System.out.println("定时任务  共 " + status.getScheduledTasks().size() + " 项");
            for (String line : status.getScheduledTasks()) {
                System.out.println("  " + line);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void meters(NetworkClientService network) throws Exception {
        Message response = network.request(DormExtCommands.METER_LIST, DormPageQuery.all());
        DormPage<MeterReadingDto> page = (DormPage<MeterReadingDto>) response.getPayload();
        System.out.println("抄表读数共 " + page.getTotalElements() + " 条");
        for (MeterReadingDto item : page.getItems()) {
            System.out.println("  #" + item.getId() + "  " + item.getBuildingCode() + " "
                    + item.getRoomNo() + "  " + item.getPeriodStart() + " ~ " + item.getPeriodEnd()
                    + "  电 " + item.getElectricityUnits() + "  水 " + item.getWaterUnits()
                    + "  应缴 " + item.getTotalAmount()
                    + (item.isLocked() ? "  [已出账 bill=" + item.getBillId() + "]" : "  [待出账]"));
        }
    }

    private static void meter(NetworkClientService network, String[] args) throws Exception {
        if (args.length < 8) {
            System.out.println("用法：meter <房间号> <账期开始> <账期结束> <用电量> <用水量> <电价> <水价>");
            return;
        }
        MeterReadingRequest request = new MeterReadingRequest(Long.parseLong(args[1]),
                LocalDate.parse(args[2]), LocalDate.parse(args[3]),
                new BigDecimal(args[4]), new BigDecimal(args[5]),
                new BigDecimal(args[6]), new BigDecimal(args[7]));
        Message response = network.request(DormExtCommands.METER_SUBMIT, request);
        MeterReadingDto saved = (MeterReadingDto) response.getPayload();
        System.out.println("已保存读数 #" + saved.getId() + "  " + saved.getBuildingCode() + " "
                + saved.getRoomNo() + "  应缴 " + saved.getTotalAmount() + " 元");
    }

    private static void bill(NetworkClientService network, String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("用法：bill <账期开始> <账期结束> [房间号]");
            return;
        }
        Long roomId = args.length > 3 ? Long.valueOf(Long.parseLong(args[3])) : null;
        BillGenerateRequest request = new BillGenerateRequest(roomId,
                LocalDate.parse(args[1]), LocalDate.parse(args[2]), null);
        Message response = network.request(DormExtCommands.BILL_GENERATE, request);
        BillGenerateResultDto result = (BillGenerateResultDto) response.getPayload();
        System.out.println("生成账单 " + result.getBillCount() + " 张，分摊 "
                + result.getAllocationCount() + " 条，跳过 " + result.getSkippedCount()
                + " 条，合计 " + result.getTotalAmount() + " 元");
        for (String note : result.getNotes()) {
            System.out.println("  " + note);
        }
    }

    /**
     * 登记一条门禁进出。
     *
     * <p>走的是宿舍模块既有的 {@code dorm.access.record} 命令，不是扩展命令——
     * 服务端按登录会话确定学生身份，因此要给某个学生造离宿记录，必须以该学生的
     * 账号登录：{@code "-Dvcampus.cli.user=demo_student" "-Dvcampus.cli.password=student123"}。</p>
     */
    private static void access(NetworkClientService network, String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("用法：access <ENTRY|EXIT> <yyyy-MM-ddTHH:mm> [门禁点]");
            return;
        }
        String door = args.length > 3 ? args[3] : "南门";
        AccessRecordRequest request = new AccessRecordRequest(args[1].toUpperCase(),
                LocalDateTime.parse(args[2]), door, "MANUAL", "命令行登记");
        Message response = network.request(DormCommands.ACCESS_RECORD, request);
        AccessRecordDto saved = (AccessRecordDto) response.getPayload();
        System.out.println("已登记门禁 #" + saved.getId() + "  " + saved.getRecordType()
                + "  " + saved.getOccurredAt() + "  " + saved.getDoorName());
    }

    private static void scan(NetworkClientService network, String[] args) throws Exception {
        LocalDate date = args.length > 1 ? LocalDate.parse(args[1]) : null;
        Message response = network.request(DormExtCommands.WARNING_SCAN,
                new WarningScanRequest(date));
        WarningScanResultDto result = (WarningScanResultDto) response.getPayload();
        System.out.println("扫描日 " + result.getScanDate() + "：在住 "
                + result.getResidentsScanned() + " 人，生成预警 " + result.getWarningCount()
                + " 条（一般 " + result.getNormalCount() + " / 严重 " + result.getSevereCount()
                + " / 已豁免 " + result.getExemptCount() + "）");
    }

    @SuppressWarnings("unchecked")
    private static void warnings(NetworkClientService network) throws Exception {
        Message response = network.request(DormExtCommands.WARNING_LIST, DormPageQuery.all());
        DormPage<AbsenceWarningDto> page = (DormPage<AbsenceWarningDto>) response.getPayload();
        System.out.println("未归预警共 " + page.getTotalElements() + " 条");
        for (AbsenceWarningDto item : page.getItems()) {
            System.out.println("  #" + item.getId() + "  学号 " + item.getStudentUserId()
                    + "  " + item.getBuildingCode() + " " + item.getRoomNo()
                    + "  扫描日 " + item.getScanDate()
                    + "  未归 " + item.getAbsenceDays() + " 天"
                    + "  " + levelText(item.getWarningLevel())
                    + "  " + statusText(item.getHandleStatus())
                    + (item.getNotifiedTeacherId() == null ? ""
                            : "  已通知 " + item.getNotifiedTeacherId()));
        }
    }

    private static void notify(NetworkClientService network, String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("用法：notify <预警编号> <辅导员用户号> [备注]");
            return;
        }
        WarningHandleRequest request = new WarningHandleRequest(Long.parseLong(args[1]),
                Long.valueOf(Long.parseLong(args[2])), args.length > 3 ? args[3] : null);
        Message response = network.request(DormExtCommands.WARNING_NOTIFY, request);
        AbsenceWarningDto value = (AbsenceWarningDto) response.getPayload();
        System.out.println("预警 #" + value.getId() + " 已通知 " + value.getNotifiedTeacherId()
                + "，时间 " + value.getNotifiedAt());
    }

    private static void verify(NetworkClientService network, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法：verify <预警编号> [备注]");
            return;
        }
        WarningHandleRequest request = new WarningHandleRequest(Long.parseLong(args[1]),
                null, args.length > 2 ? args[2] : null);
        Message response = network.request(DormExtCommands.WARNING_VERIFY, request);
        AbsenceWarningDto value = (AbsenceWarningDto) response.getPayload();
        System.out.println("预警 #" + value.getId() + " 已核实");
    }

    private static void config(NetworkClientService network, String[] args) throws Exception {
        Message response;
        if (args.length >= 4) {
            response = network.request(DormExtCommands.WARNING_CONFIG_SET,
                    new WarningConfigRequest(Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                            Boolean.parseBoolean(args[3])));
        } else {
            response = network.request(DormExtCommands.WARNING_CONFIG_GET, null);
        }
        WarningConfigDto config = (WarningConfigDto) response.getPayload();
        System.out.println("预警阈值：连续未归 " + config.getWarnDays() + " 天预警，"
                + config.getNotifyDays() + " 天升严重，请假豁免 "
                + (config.isExemptOnLeave() ? "开" : "关"));
    }

    private static String levelText(String level) {
        if (AbsenceWarningDto.LEVEL_SEVERE.equals(level)) return "[严重]";
        if (AbsenceWarningDto.LEVEL_EXEMPT.equals(level)) return "[已豁免]";
        return "[一般]";
    }

    private static String statusText(String status) {
        if (AbsenceWarningDto.STATUS_NOTIFIED.equals(status)) return "已通知";
        if (AbsenceWarningDto.STATUS_VERIFIED.equals(status)) return "已核实";
        return "待处理";
    }

    private static void usage() {
        System.out.println("宿舍扩展命令行工具");
        System.out.println("  status                                                     查看模块状态");
        System.out.println("  meters                                                     列出抄表读数");
        System.out.println("  meter <房间号> <开始> <结束> <电量> <水量> <电价> <水价>   录入抄表读数");
        System.out.println("  bill  <开始> <结束> [房间号]                               生成账单与分摊");
        System.out.println("  access <ENTRY|EXIT> <yyyy-MM-ddTHH:mm> [门禁点]            登记门禁进出（按登录账号）");
        System.out.println("  scan  [yyyy-MM-dd]                                         触发未归扫描");
        System.out.println("  warnings                                                   列出未归预警");
        System.out.println("  notify <预警编号> <辅导员用户号> [备注]                    通知辅导员");
        System.out.println("  verify <预警编号> [备注]                                   标记已核实");
        System.out.println("  config [预警天数 通知天数 豁免true/false]                   查看或修改阈值");
        System.out.println();
        System.out.println("可用系统属性：-Dvcampus.cli.user / -Dvcampus.cli.password（默认 demo_dorm）");
    }
}
