package dev.blockgate.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BlockGateCoreTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("T12 / AC-6: 연결할 수 없는 DB로 활성화 → onEnable 예외 없음, SEVERE 'DB 연결 실패:' 로그, 플러그인 비활성화")
    void disablesItselfWhenDatabaseUnreachable() {
        // 포트 1: 접속이 즉시 거부되는 주소
        Map<String, String> env = Map.of("BLOCKGATE_DB_URL", "jdbc:postgresql://127.0.0.1:1/none");

        BlockGateCore plugin = MockBukkit.load(BlockGateCore.class, env);
        List<LogRecord> logs = capture(plugin);

        assertThat(plugin.isEnabled()).isTrue(); // onEnable은 DB를 기다리지 않고 끝난다
        plugin.startupFuture().handle((r, e) -> null).orTimeout(30, TimeUnit.SECONDS).join();
        server.getScheduler().performOneTick();

        assertThat(plugin.isEnabled()).isFalse();
        assertThat(logs).anySatisfy(r -> {
            assertThat(r.getLevel()).isEqualTo(Level.SEVERE);
            assertThat(r.getMessage()).startsWith("DB 연결 실패:");
        });
    }

    @Test
    @DisplayName("T19 / AC-2, AC-6: URL 미설정으로 활성화 → SEVERE 안내, 비활성화, 설정은 메인 스레드에서 해석(DB 실행기 미생성)")
    void failsFastOnMissingUrl() {
        BlockGateCore plugin = MockBukkit.load(BlockGateCore.class, Map.<String, String>of());
        List<LogRecord> logs = capture(plugin);

        assertThat(plugin.dbExecutor()).as("설정 오류면 DB 스레드를 만들지 않는다").isNull();
        server.getScheduler().performOneTick();

        assertThat(plugin.isEnabled()).isFalse();
        assertThat(logs).anySatisfy(r -> {
            assertThat(r.getLevel()).isEqualTo(Level.SEVERE);
            assertThat(r.getMessage()).isEqualTo(
                    "DB 연결 실패: DB URL이 설정되지 않았습니다 (BLOCKGATE_DB_URL 또는 config.yml database.url)");
        });
    }

    private static List<LogRecord> capture(BlockGateCore plugin) {
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        plugin.getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return records;
    }
}
