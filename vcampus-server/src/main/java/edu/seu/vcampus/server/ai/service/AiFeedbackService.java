package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiFeedbackEntry;
import edu.seu.vcampus.common.ai.AiFeedbackRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.repository.AiFeedbackRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.List;

public final class AiFeedbackService {
    private final AiFeedbackRepository repository;
    private final TransactionManager transactions;
    public AiFeedbackService(AiFeedbackRepository repository, TransactionManager transactions) {
        this.repository = repository; this.transactions = transactions;
    }
    public void save(final SessionContext session, final AiFeedbackRequest request) {
        validate(request);
        Boolean saved = tx(new Work<Boolean>() { public Boolean run(Connection c) throws Exception {
            return Boolean.valueOf(repository.save(c, session.getUserId(), request));
        }});
        if (!saved.booleanValue()) throw new AiServiceException(ResultCodes.NOT_FOUND,
                "只能评价本人会话中的回答");
    }
    public List<AiFeedbackEntry> latest() {
        return tx(new Work<List<AiFeedbackEntry>>() { public List<AiFeedbackEntry> run(Connection c)
                throws Exception { return repository.latest(c, 100); }});
    }
    private void validate(AiFeedbackRequest r) {
        if (r == null || blank(r.getSessionId()) || blank(r.getRequestId())
                || !("HELPFUL".equals(r.getRating()) || "UNHELPFUL".equals(r.getRating()))) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "反馈参数不正确");
        }
        if (r.getComment() != null && r.getComment().length() > 500) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "反馈说明不能超过 500 字");
        }
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private <T> T tx(final Work<T> work) {
        try { return transactions.execute(new TransactionWork<T>() {
            public T execute(Connection c) throws Exception { return work.run(c); }
        }); } catch (AiServiceException ex) { throw ex; }
        catch (Exception ex) { throw new AiServiceException(ResultCodes.INTERNAL_ERROR,
                "反馈服务暂时不可用", ex); }
    }
    private interface Work<T> { T run(Connection c) throws Exception; }
}
