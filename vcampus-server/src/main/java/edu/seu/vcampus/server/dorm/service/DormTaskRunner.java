package edu.seu.vcampus.server.dorm.service;

/**
 * 按需触发定时任务。
 *
 * <p>接口放在 service 包而不是 schedule 包，是为了让依赖只有一个方向：调度器认识
 * 服务，服务只认识这个接口。服务端启动时由 {@code DormScheduler} 把自己挂上来，
 * 没挂（比如用 {@code -Dvcampus.dorm.scheduler.enabled=false} 关掉了调度器）时，
 * 相关命令会明确报「调度器未启动」，而不是假装成功。</p>
 */
public interface DormTaskRunner {
    /**
     * 立刻在当前线程执行任务并把结果写进台账。
     *
     * @param taskName 任务名；为 null 表示全部依次执行
     */
    void runNow(String taskName);
}
