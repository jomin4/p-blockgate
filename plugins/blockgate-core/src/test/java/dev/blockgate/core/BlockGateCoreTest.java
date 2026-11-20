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
