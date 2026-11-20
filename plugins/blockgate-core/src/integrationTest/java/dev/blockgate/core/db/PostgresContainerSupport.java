package dev.blockgate.core.db;

import org.testcontainers.postgresql.PostgreSQLContainer;

/** 통합 테스트 공용 PostgreSQL 17 컨테이너 (운영·로컬과 같은 이미지 태그). */
public final class PostgresContainerSupport {

    public static final String IMAGE = "postgres:17.12";

    private PostgresContainerSupport() {
    }

    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer(IMAGE);
    }

    static DatabaseSettings settingsFor(PostgreSQLContainer pg, int poolSize) {
        return new DatabaseSettings(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword(), poolSize);
    }
}
