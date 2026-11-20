package dev.blockgate.core.db;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** DB 전용 실행기. 메인 스레드에서 DB를 호출하지 않기 위해 모든 DB 작업을 여기로 보낸다. */
public final class DbExecutor implements AutoCloseable {

    static final String THREAD_PREFIX = "BlockGateCore-DB-";

    private final ExecutorService pool;

    public DbExecutor(int threads) {
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, THREAD_PREFIX + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        this.pool = Executors.newFixedThreadPool(threads, factory);
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pool.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
