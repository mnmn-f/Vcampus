package edu.seu.vcampus.client.view.modules.real;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/** Swing 网络任务边界：后台执行，完成回调始终回到 EDT。 */
public final class AsyncTask {
    private static final java.util.concurrent.atomic.AtomicInteger PENDING = new java.util.concurrent.atomic.AtomicInteger();
    public static boolean isIdle() { return PENDING.get() == 0; }
    private AsyncTask() {
    }

    public interface Work<T> {
        T run() throws Exception;
    }

    public interface Callback<T> {
        void onSuccess(T value);
        void onFailure(Throwable error);
    }

    public static <T> void run(final Work<T> work, final Callback<T> callback) {
        PENDING.incrementAndGet();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception { return work.run(); }

            @Override
            protected void done() {
                try {
                    callback.onSuccess(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    callback.onFailure(ex);
                } catch (ExecutionException ex) {
                    callback.onFailure(ex.getCause() == null ? ex : ex.getCause());
                } catch (Exception ex) {
                    callback.onFailure(ex);
                } finally {
                    PENDING.decrementAndGet();
                }
            }
        }.execute();
    }

    public static String message(Throwable error) {
        Throwable current = unwrap(error);
        while (current != null) {
            String text = current.getMessage();
            if (text != null && text.trim().length() > 0) {
                return clean(text);
            }
            current = current.getCause();
        }
        return "请求失败，请稍后重试。";
    }

    private static String clean(String value) {
        String text = value.replace('\r', ' ').replace('\n', ' ').trim();
        int colon = text.indexOf(':');
        if (colon > 0) {
            String prefix = text.substring(0, colon).trim();
            if (prefix.indexOf('.') > 0 && prefix.matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+")) {
                text = text.substring(colon + 1).trim();
            }
        }
        return text.length() == 0 ? "请求失败，请稍后重试。" : text;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof ExecutionException) {
            Throwable cause = current.getCause();
            if (cause == null || cause == current) break;
            current = cause;
        }
        return current;
    }
}
