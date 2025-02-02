-- `team` 테이블에 데이터 추가
INSERT INTO team (team_id, post_id, professor_id, name, exhibition_year, category)
VALUES (0, NULL, NULL, 'admin', 2025, 'WEB');

-- `account` 테이블에 데이터 추가 (team_id=0을 참조)
INSERT INTO account (account_id, team_id, user_email, default_pwd, pwd, recent, role)
VALUES (0, 0, 'admin', 'admin', 'admin', null, 'ADMIN');
