// blockgate-core — 첫 플러그인 모듈 (DB 풀, Flyway, 비동기 실행기, 활성화·헬스 로그)
// 테스트 규칙: docs/04-tdd-testing.md (skip = 실패, 단위/통합 분리, JaCoCo 병합)
plugins {
    java
    jacoco
    alias(libs.plugins.shadow)
}

group = "dev.blockgate"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// ── 소스셋: test(단위) + integrationTest(통합, Testcontainers) ──
val integrationTest: SourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

// Mockito inline mock maker를 Java 21+에서 경고/실패 없이 쓰기 위한 agent
val mockitoAgent: Configuration = configurations.create("mockitoAgent")

dependencies {
    compileOnly(libs.paper.api)

    // DB — Shadow로 포함하고 relocate (slf4j는 Paper가 제공)
    implementation(libs.hikaricp) { exclude(group = "org.slf4j") }
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockbukkit)

    "integrationTestImplementation"(platform(libs.testcontainers.bom))
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.testcontainers.junit)

    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

// ── 테스트 공통: JUnit Platform, Mockito agent, skip = 실패 ──
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null && result.skippedTestCount > 0) {
                throw GradleException(
                    "${result.skippedTestCount}개 테스트가 skip됨 — skip은 실패로 간주 (MockBukkit 미구현 메서드 확인)"
                )
            }
        }
    })
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "통합 테스트 (Testcontainers PostgreSQL 17)"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTestTask)
}

// ── JaCoCo: test + integrationTest 실행 데이터 병합, XML + HTML ──
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTestTask)
    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") }
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ── 배포 JAR: 의존성 포함 + relocate ──
tasks.shadowJar {
    archiveFileName.set("BlockGateCore.jar")
    mergeServiceFiles()
    val libs = "dev.blockgate.core.libs"
    relocate("com.zaxxer.hikari", "$libs.hikari")
    relocate("org.postgresql", "$libs.postgresql")
    relocate("org.flywaydb", "$libs.flyway")
    relocate("com.fasterxml", "$libs.fasterxml")
    relocate("tools.jackson", "$libs.jackson")
}

tasks.jar {
    enabled = false // 배포물은 shadowJar 하나만
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// ── 로컬 개발 서버 반영: shadowJar → infra/dev/data/plugins/ → mc 재시작 ──
val devServerDir = rootProject.layout.projectDirectory.dir("../infra/dev")

val copyToDevServer = tasks.register<Copy>("copyToDevServer") {
    from(tasks.shadowJar)
    into(devServerDir.dir("data/plugins"))
}

tasks.register<Exec>("deployDev") {
    description = "로컬 개발 서버(blockgate-dev)에 플러그인을 반영하고 mc를 재시작한다 (/reload 금지)"
    group = "blockgate"
    dependsOn(copyToDevServer)
    commandLine("docker", "compose", "-p", "blockgate-dev", "restart", "mc")
}
