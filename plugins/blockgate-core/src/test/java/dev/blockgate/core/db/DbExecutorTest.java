package dev.blockgate.core.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DbExecutorTest {

    private final DbExecutor executor = new DbExecutor(2);

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    @DisplayName("T9 / AC-5: 맡긴 작업이 BlockGateCore-DB- 스레드에서 실행되고 결과를 돌려준다")
    void runsOnDedicatedThread() throws Exception {
        String caller = Thread.currentThread().getName();

        String worker = executor.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS);

        assertThat(worker).startsWith("BlockGateCore-DB-").isNotEqualTo(caller);
    }

    @Test
    @DisplayName("T10 / AC-5: 작업 예외는 같은 원인을 가진 실패 future가 된다")
    void exceptionBecomesFailedFuture() {
        var boom = new java.sql.SQLException("boom");

        var future = executor.submit(() -> {
            throw boom;
        });

        assertThat(future)
                .failsWithin(5, TimeUnit.SECONDS)
                .withThrowableOfType(java.util.concurrent.ExecutionException.class)
                .havingCause()
                .isSameAs(boom);
    }

    @Test
    @DisplayName("T17 / AC-5: 작업이 Error를 던져도 같은 원인의 실패 future가 된다")
    void errorBecomesFailedFuture() {
        var linkage = new LinkageError("shaded class missing");

        var future = executor.submit(() -> {
            throw linkage;
        });

        assertThat(future)
                .failsWithin(5, TimeUnit.SECONDS)
                .withThrowableOfType(java.util.concurrent.ExecutionException.class)
                .havingCause()
                .isSameAs(linkage);
    }

    @Test
    @DisplayName("T11 / AC-5: 닫힌 실행기에 맡기면 IllegalStateException 실패 future")
    void submitAfterCloseFails() {
        executor.close();

        var future = executor.submit(() -> "never");

        assertThat(future)
                .failsWithin(5, TimeUnit.SECONDS)
                .withThrowableOfType(java.util.concurrent.ExecutionException.class)
                .havingCause()
                .isInstanceOf(IllegalStateException.class);
    }
}
