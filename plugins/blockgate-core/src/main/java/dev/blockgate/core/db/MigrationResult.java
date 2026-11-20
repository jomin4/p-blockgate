package dev.blockgate.core.db;

/** 마이그레이션 결과: 적용 후 스키마 버전과 이번 실행에서 적용한 마이그레이션 수. */
public record MigrationResult(String schemaVersion, int migrationsApplied) {
}
