// BlockGate 플러그인 Gradle 루트 — 명세: docs/01-tech-stack.md, docs/04-tdd-testing.md 8장
rootProject.name = "blockgate"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include("blockgate-core")
