package dev.blockgate.core.db;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/** DB 접속 설정. 환경변수가 config.yml보다 우선한다 (docs/01-tech-stack.md "설정" 행). */
public record DatabaseSettings(String url, String user, String password, int poolSize) {

    public static DatabaseSettings resolve(Map<String, String> env, ConfigurationSection config) {
        return new DatabaseSettings(
                config.getString("database.url"),
                config.getString("database.user"),
                config.getString("database.password"),
                config.getInt("database.pool-size"));
    }
}
