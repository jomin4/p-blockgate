package dev.blockgate.core.db;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
class MigratorIT {

    // 테스트마다 빈 DB가 필요하므로 인스턴스 필드(테스트마다 새 컨테이너)
    @Container
    final PostgreSQLContainer pg = PostgresContainerSupport.newContainer();

    private HikariDataSource ds;

    @BeforeEach
    void setUp() {
        ds = DataSourceFactory.create(PostgresContainerSupport.settingsFor(pg, 2));
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    @Test
    @DisplayName("T7 / AC-4: 빈 DB 마이그레이션 → 스키마 버전 1, bg_meta 테이블 존재")
    void migratesEmptyDatabase() throws Exception {
        MigrationResult result = Migrator.migrate(ds, getClass().getClassLoader());

        assertThat(result.schemaVersion()).isEqualTo("1");
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT column_name FROM information_schema.columns "
                             + "WHERE table_name = 'bg_meta' ORDER BY ordinal_position")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("key");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("value");
        }
    }
}
