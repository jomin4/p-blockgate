package dev.blockgate.core.db;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/** DB 접속 설정. 환경변수가 config.yml보다 우선한다 (docs/01-tech-stack.md "설정" 행). */
public record DatabaseSettings(String url, String user, String password, int poolSize) {

    static final int DEFAULT_POOL_SIZE = 4;
    static final String ENV_URL = "BLOCKGATE_DB_URL";
    static final String ENV_USER = "BLOCKGATE_DB_USER";
    static final String ENV_PASSWORD = "BLOCKGATE_DB_PASSWORD";

    public static DatabaseSettings resolve(Map<String, String> env, ConfigurationSection config) {
        return new DatabaseSettings(
                pick(env, ENV_URL, config.getString("database.url")),
                pick(env, ENV_USER, config.getString("database.user")),
                pick(env, ENV_PASSWORD, config.getString("database.password")),
                config.getInt("database.pool-size", DEFAULT_POOL_SIZE));
    }

    private static String pick(Map<String, String> env, String key, String fallback) {
        String value = env.get(key);
        return value != null && !value.isBlank() ? value : fallback;
    }
}
