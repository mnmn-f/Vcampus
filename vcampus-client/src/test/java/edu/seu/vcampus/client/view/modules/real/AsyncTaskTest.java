package edu.seu.vcampus.client.view.modules.real;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** 异步错误只向页面暴露用户可读消息，不泄漏包装异常类型。 */
public final class AsyncTaskTest {
    @Test
    public void unwrapsExecutionException() {
        Throwable error = new ExecutionException(
                new IllegalStateException("网络连接中断，请稍后重试"));

        String message = AsyncTask.message(error);

        assertEquals("网络连接中断，请稍后重试", message);
        assertFalse(message.contains("ExecutionException"));
        assertFalse(message.contains("java."));
    }

    @Test
    public void hidesMissingExceptionMessage() {
        assertEquals("请求失败，请稍后重试。", AsyncTask.message(new RuntimeException()));
    }

    @Test
    public void stripsQualifiedExceptionPrefix() {
        assertEquals("服务器拒绝请求", AsyncTask.message(
                new RuntimeException("edu.seu.vcampus.client.NetworkClientException: 服务器拒绝请求")));
    }
}
