package edu.seu.vcampus.server.network;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** TCP 连接线程命名和守护线程策略。 */
final class ConnectionThreadFactory implements ThreadFactory {
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable,
                "vcampus-client-" + sequence.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
