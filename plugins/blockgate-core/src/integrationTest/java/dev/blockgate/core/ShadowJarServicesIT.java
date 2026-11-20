package dev.blockgate.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/** 배포 JAR의 relocate 결과 검사 — 실서버 없이 Shadow 설정 문제를 잡는다. */
@Tag("integration")
class ShadowJarServicesIT {

    private static final String FLYWAY_PLUGIN_SERVICE = "META-INF/services/org.flywaydb.core.extensibility.Plugin";
    private static final String RELOCATED_PREFIX = "dev.blockgate.core.libs.flyway";

    @Test
    @DisplayName("T16 / AC-8: 배포 JAR의 Flyway 플러그인 서비스 목록이 원본 항목을 relocate된 이름으로 모두 포함")
    void shadedJarKeepsAllFlywayPluginServices() throws IOException {
        List<String> expected = new ArrayList<>();
        for (URL url : Collections.list(getClass().getClassLoader().getResources(FLYWAY_PLUGIN_SERVICE))) {
            for (String line : readLines(url.openStream())) {
                expected.add(line.replace("org.flywaydb", RELOCATED_PREFIX));
            }
        }
        assertThat(expected).as("원본 서비스 항목 (flyway-core + flyway-database-postgresql)").hasSizeGreaterThan(3);

        Path jar = Path.of(System.getProperty("blockgate.shadowJar"));
        List<String> actual;
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var entry = jarFile.getEntry(
                    "META-INF/services/" + RELOCATED_PREFIX + ".core.extensibility.Plugin");
            assertThat(entry).as("relocate된 Flyway 서비스 파일").isNotNull();
            actual = readLines(jarFile.getInputStream(entry));
        }

        assertThat(actual).containsAll(expected);
    }

    private static List<String> readLines(java.io.InputStream in) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
        }
        return lines;
    }
}
