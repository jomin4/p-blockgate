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
}
