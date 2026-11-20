package dev.blockgate.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** 설정으로 HikariCP 풀을 만든다. 호출은 DB 실행기(비동기)에서만 한다. */
public final class DataSourceFactory {

    static final String POOL_NAME = "BlockGateCore-DB";

    private DataSourceFactory() {
    }

    public static HikariDataSource create(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(POOL_NAME);
        config.setJdbcUrl(settings.url());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.poolSize());
        // relocate 뒤에도 드라이버를 확실히 찾도록 명시
        config.setDriverClassName(org.postgresql.Driver.class.getName());
        return new HikariDataSource(config);
    }
}
