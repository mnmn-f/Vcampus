package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.protocol.ResultCodes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 限制单用户并发 AI 请求并支持按请求编号取消。 */
final class AiQueryLimiter {
    private static final int MAX_PER_USER = 2;
    private final ConcurrentMap<String, ActiveQuery> active =
            new ConcurrentHashMap<String, ActiveQuery>();
    private final ConcurrentMap<Long, AtomicInteger> userCounts =
            new ConcurrentHashMap<Long, AtomicInteger>();

    void register(String requestId, long userId) {
        AtomicInteger count = userCounts.computeIfAbsent(Long.valueOf(userId),
                key -> new AtomicInteger());
        if (count.incrementAndGet() > MAX_PER_USER) {
            releaseCount(userId, count);
            throw new AiServiceException(ResultCodes.CONFLICT, "同时进行的校园助手请求过多");
        }
        ActiveQuery query = new ActiveQuery(Thread.currentThread(), userId);
        if (active.putIfAbsent(requestId, query) != null) {
            releaseCount(userId, count);
            throw new AiServiceException(ResultCodes.CONFLICT, "请求编号正在使用");
        }
    }

    boolean cancel(long userId, String requestId) {
        ActiveQuery query = requestId == null ? null : active.get(requestId);
        if (query == null || query.userId != userId) return false;
        query.thread.interrupt();
        return true;
    }

    void release(String requestId, long userId) {
        if (active.remove(requestId) == null) return;
        AtomicInteger count = userCounts.get(Long.valueOf(userId));
        if (count != null) releaseCount(userId, count);
    }

    private void releaseCount(long userId, AtomicInteger count) {
        if (count.decrementAndGet() == 0) userCounts.remove(Long.valueOf(userId), count);
    }

    private static final class ActiveQuery {
        private final Thread thread;
        private final long userId;
        private ActiveQuery(Thread thread, long userId) {
            this.thread = thread; this.userId = userId;
        }
    }
}
