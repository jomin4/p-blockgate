package dev.blockgate.core.db;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
        try {
            pool.execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Throwable t) { // Error(LinkageError 등)도 future로 넘겨야 호출자가 영원히 기다리지 않는다
                    future.completeExceptionally(t);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(new IllegalStateException("DB 실행기가 이미 닫혔습니다", e));
        }
        return future;
    }

    public boolean isTerminated() {
        return pool.isTerminated();
    }

    @Override
    public void close() {
        pool.shutdown();
    }

    /** 새 작업을 막고, 진행 중인 작업이 끝나기를 최대 timeout까지 기다린다. 넘기면 중단(interrupt)한다. */
    public void closeAndAwait(Duration timeout) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
