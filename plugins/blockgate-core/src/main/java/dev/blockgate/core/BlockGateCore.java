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
import java.util.logging.Level;

/**
 * BlockGate 공통 기반 플러그인. 활성화 시 DB 연결·마이그레이션을 DB 실행기에서 비동기로 하고,
 * 결과는 메인 스레드에서 로그로 남긴다 (실패하면 스스로 비활성화).
 */
public class BlockGateCore extends JavaPlugin {

    private static final int DB_THREADS = 2;

    private final Map<String, String> env;
    private DbExecutor dbExecutor;
    private volatile HikariDataSource dataSource;
    private CompletableFuture<MigrationResult> startup;

    public BlockGateCore() {
        this(System.getenv());
    }

    /** 테스트용: 환경변수를 주입한다 (MockBukkit.load가 public 생성자를 요구). */
    public BlockGateCore(Map<String, String> env) {
        this.env = Map.copyOf(env);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dbExecutor = new DbExecutor(DB_THREADS);
        startup = dbExecutor.submit(this::connectAndMigrate)
                .whenComplete((result, error) ->
                        getServer().getScheduler().runTask(this, () -> onStartupFinished(result, error)));
    }

    @Override
    public void onDisable() {
        HikariDataSource ds = dataSource;
        if (ds != null) {
            ds.close();
        }
        if (dbExecutor != null) {
            dbExecutor.close();
        }
    }

    private MigrationResult connectAndMigrate() {
        DatabaseSettings settings = DatabaseSettings.resolve(env, getConfig());
        dataSource = DataSourceFactory.create(settings);
        return Migrator.migrate(dataSource, getClassLoader());
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

    /** 테스트용: 비동기 초기화(결과 처리 예약까지)가 끝나면 완료되는 future. */
    CompletableFuture<MigrationResult> startupFuture() {
        return startup;
    }
}
