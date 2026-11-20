package dev.blockgate.core.db;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class DatabaseSettingsTest {

    private static YamlConfiguration config(String url, String user, String password, Integer poolSize) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (url != null) yaml.set("database.url", url);
        if (user != null) yaml.set("database.user", user);
        if (password != null) yaml.set("database.password", password);
        if (poolSize != null) yaml.set("database.pool-size", poolSize);
        return yaml;
    }

    @Test
    @DisplayName("T1 / AC-1: 환경변수가 없으면 config.yml 값(url·user·password·pool-size)을 쓴다")
    void usesConfigWhenNoEnv() {
        var yaml = config("jdbc:postgresql://cfg-host:5432/cfgdb", "cfg-user", "cfg-pass", 6);

        DatabaseSettings settings = DatabaseSettings.resolve(Map.of(), yaml);

        assertThat(settings.url()).isEqualTo("jdbc:postgresql://cfg-host:5432/cfgdb");
        assertThat(settings.user()).isEqualTo("cfg-user");
        assertThat(settings.password()).isEqualTo("cfg-pass");
        assertThat(settings.poolSize()).isEqualTo(6);
    }

    @Test
    @DisplayName("T2 / AC-1: 환경변수가 있으면 config.yml보다 우선한다 (빈 문자열 환경변수는 무시)")
    void envOverridesConfig() {
        var yaml = config("jdbc:postgresql://cfg-host:5432/cfgdb", "cfg-user", "cfg-pass", 6);
        var env = Map.of(
                "BLOCKGATE_DB_URL", "jdbc:postgresql://env-host:5432/envdb",
                "BLOCKGATE_DB_USER", "",
                "BLOCKGATE_DB_PASSWORD", "env-pass");

        DatabaseSettings settings = DatabaseSettings.resolve(env, yaml);

        assertThat(settings.url()).isEqualTo("jdbc:postgresql://env-host:5432/envdb");
        assertThat(settings.user()).isEqualTo("cfg-user");
        assertThat(settings.password()).isEqualTo("env-pass");
        assertThat(settings.poolSize()).isEqualTo(6);
    }

    @Test
    @DisplayName("T3 / AC-1: pool-size 미설정이면 4")
    void defaultPoolSizeIsFour() {
        var yaml = config("jdbc:postgresql://cfg-host:5432/cfgdb", "cfg-user", "cfg-pass", null);

        DatabaseSettings settings = DatabaseSettings.resolve(Map.of(), yaml);

        assertThat(settings.poolSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("T4 / AC-2: URL이 어디에도 없으면 안내 메시지와 함께 실패")
    void failsWhenUrlMissing() {
        var yaml = config(null, "cfg-user", "cfg-pass", 4);

        assertThatThrownBy(() -> DatabaseSettings.resolve(Map.of("BLOCKGATE_DB_URL", " "), yaml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DB URL이 설정되지 않았습니다 (BLOCKGATE_DB_URL 또는 config.yml database.url)");
    }

    @Test
    @DisplayName("T20 / AC-1: toString()에 비밀번호가 드러나지 않는다")
    void toStringMasksPassword() {
        var settings = new DatabaseSettings("jdbc:postgresql://h:5432/db", "u", "s3cret-value", 4);

        assertThat(settings.toString())
                .doesNotContain("s3cret-value")
                .contains("password=***")
                .contains("jdbc:postgresql://h:5432/db");
    }

    @Test
    @DisplayName("T5 / AC-2: pool-size 0이면 실패, 1은 허용")
    void poolSizeMustBeAtLeastOne() {
        String url = "jdbc:postgresql://cfg-host:5432/cfgdb";

        assertThatThrownBy(() -> DatabaseSettings.resolve(Map.of(), config(url, "u", "p", 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database.pool-size는 1 이상이어야 합니다 (현재: 0)");
        assertThat(DatabaseSettings.resolve(Map.of(), config(url, "u", "p", 1)).poolSize()).isEqualTo(1);
    }
}
