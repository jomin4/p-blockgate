package dev.blockgate.core.db;

import com.zaxxer.hikari.HikariDataSource;
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
class DataSourceFactoryIT {

    @Container
    static final PostgreSQLContainer PG = PostgresContainerSupport.newContainer();

    @Test
    @DisplayName("T6 / AC-3: 풀 이름 BlockGateCore-DB, 최대 크기 = pool-size, SELECT 1 = 1")
    void createsWorkingPool() throws Exception {
        try (HikariDataSource ds = DataSourceFactory.create(PostgresContainerSupport.settingsFor(PG, 3));
             Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {

            assertThat(ds.getPoolName()).isEqualTo("BlockGateCore-DB");
            assertThat(ds.getMaximumPoolSize()).isEqualTo(3);
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
