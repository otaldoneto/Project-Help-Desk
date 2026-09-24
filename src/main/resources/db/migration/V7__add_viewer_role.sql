ALTER TABLE tb_user DROP CONSTRAINT ck_user_role;
ALTER TABLE tb_user ADD CONSTRAINT ck_user_role CHECK (role IN ('ADMIN', 'USER', 'VIEWER'));
