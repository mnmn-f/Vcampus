package edu.seu.vcampus.server.ai.handler;

import edu.seu.vcampus.common.ai.AiCompetitionRegistrationView;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ai.repository.AiLiveDataRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** 为助手提供当前用户范围内、原 DTO 尚未覆盖的只读实时视图。 */
public final class AiLiveDataCommandHandler implements CommandHandler {
    private final AiLiveDataRepository repository;
    private final TransactionManager transactions;

    public AiLiveDataCommandHandler(AiLiveDataRepository repository,
            TransactionManager transactions) {
        this.repository = repository; this.transactions = transactions;
    }

    public Message handle(final Message request, final SessionContext session) {
        try {
            List<AiCompetitionRegistrationView> values = transactions.execute(
                    new TransactionWork<List<AiCompetitionRegistrationView>>() {
                public List<AiCompetitionRegistrationView> execute(Connection connection)
                        throws Exception {
                    return repository.myCompetitionRegistrations(connection, session.getUserId());
                }
            });
            return Message.success(request,
                    new ArrayList<AiCompetitionRegistrationView>(values));
        } catch (Exception ex) {
            return Message.failure(request, ResultCodes.INTERNAL_ERROR, "本人竞赛报名暂时无法查询");
        }
    }

    public Permission requiredPermission() { return Permission.COMPETITION_ENROLL; }
    public boolean requiresAuthentication() { return true; }
}
