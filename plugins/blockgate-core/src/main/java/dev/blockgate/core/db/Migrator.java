package dev.blockgate.core.db;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/** 플러그인 안의 db/migration을 Flyway로 적용한다. 호출은 DB 실행기(비동기)에서만 한다. */
public final class Migrator {

    private Migrator() {
    }

    /**
     * @param classLoader 마이그레이션 파일을 찾을 클래스로더 (플러그인 클래스로더)
     */
    public static MigrationResult migrate(DataSource dataSource, ClassLoader classLoader) {
        Flyway flyway = Flyway.configure(classLoader)
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        int applied = flyway.migrate().migrationsExecuted;
        var current = flyway.info().current();
        return new MigrationResult(current == null ? null : current.getVersion().getVersion(), applied);
    }
}
