package dev.blockgate.core;

import com.zaxxer.hikari.HikariDataSource;
import dev.blockgate.core.db.DataSourceFactory;
import dev.blockgate.core.db.DatabaseSettings;
import dev.blockgate.core.db.DbExecutor;
import dev.blockgate.core.db.MigrationResult;
import dev.blockgate.core.db.Migrator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * BlockGate 공통 기반 플러그인. 활성화 시 DB 연결·마이그레이션을 DB 실행기에서 비동기로 하고,
 * 결과는 메인 스레드에서 로그로 남긴다 (실패하면 스스로 비활성화).
 */
public class BlockGateCore extends JavaPlugin {

    private static final int DB_THREADS = 2;
    private static final java.time.Duration SHUTDOWN_TIMEOUT = java.time.Duration.ofSeconds(10);

    private final Map<String, String> env;
    private final Function<DatabaseSettings, HikariDataSource> poolOpener;
    private volatile boolean shuttingDown;
    private DbExecutor dbExecutor;
    private volatile HikariDataSource dataSource;
    private CompletableFuture<MigrationResult> startup;

    public BlockGateCore() {
        this(System.getenv());
    }

    /** 테스트용: 환경변수를 주입한다 (MockBukkit.load가 public 생성자를 요구). */
    public BlockGateCore(Map<String, String> env) {
        this(env, DataSourceFactory::create);
    }

    /** 테스트용: 풀 생성도 주입한다 (초기화 도중 비활성화 재현용). */
    public BlockGateCore(Map<String, String> env, Function<DatabaseSettings, HikariDataSource> poolOpener) {
        this.env = Map.copyOf(env);
        this.poolOpener = poolOpener;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dbExecutor = new DbExecutor(DB_THREADS);
        startup = dbExecutor.submit(this::connectAndMigrate)
                .whenComplete((result, error) -> {
                    if (!shuttingDown) { // 비활성화된 플러그인으로는 작업을 예약할 수 없다
                        getServer().getScheduler().runTask(this, () -> onStartupFinished(result, error));
                    }
                });
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        // 진행 중인 DB 작업(초기화 포함)이 끝나기를 기다린 뒤 풀을 닫는다
        if (dbExecutor != null) {
            dbExecutor.closeAndAwait(SHUTDOWN_TIMEOUT);
        }
        HikariDataSource ds = dataSource;
        dataSource = null;
        if (ds != null) {
            ds.close();
        }
    }

    private MigrationResult connectAndMigrate() {
        DatabaseSettings settings = DatabaseSettings.resolve(env, getConfig());
        HikariDataSource ds = poolOpener.apply(settings);
        try {
            abortIfShuttingDown();
            MigrationResult result = Migrator.migrate(ds, getClassLoader());
            abortIfShuttingDown();
            dataSource = ds; // 마이그레이션이 끝난 풀만 공개한다
            return result;
        } catch (Throwable t) {
            ds.close();
            throw t;
        }
    }

    private void abortIfShuttingDown() {
        if (shuttingDown) {
            throw new IllegalStateException("플러그인 종료 중이라 DB 초기화를 중단했습니다");
        }
    }

    private void onStartupFinished(MigrationResult result, Throwable error) {
        if (error != null) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            getLogger().log(Level.SEVERE, "DB 연결 실패: " + cause.getMessage(), cause);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("DB 연결 성공 (스키마 버전 " + result.schemaVersion() + ")");
    }

    /** 초기화된 DB 풀 (초기화 전·실패 시 null). */
    HikariDataSource dataSource() {
        return dataSource;
    }

    /** 비활성화(종료)가 시작됐는지. */
    boolean isShuttingDown() {
        return shuttingDown;
    }

    DbExecutor dbExecutor() {
        return dbExecutor;
    }

    /** 테스트용: 비동기 초기화(결과 처리 예약까지)가 끝나면 완료되는 future. */
    CompletableFuture<MigrationResult> startupFuture() {
        return startup;
    }
}
