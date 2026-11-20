-- BlockGate 기준 스키마. 이후 변경은 V<n>__<desc>.sql을 새로 추가한다 (기존 파일 수정 금지).
CREATE TABLE bg_meta (
    key   text PRIMARY KEY,
    value text NOT NULL
);
