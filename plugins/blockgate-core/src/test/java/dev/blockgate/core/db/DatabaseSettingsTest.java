package dev.blockgate.core.db;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
