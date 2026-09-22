ALTER TABLE tb_order_service ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'unknown';
ALTER TABLE tb_order_service ADD COLUMN last_modified_by VARCHAR(255) NOT NULL DEFAULT 'unknown';
ALTER TABLE tb_order_service ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;

UPDATE tb_order_service SET last_modified_at = created_at WHERE last_modified_at IS NULL;

ALTER TABLE tb_order_service ALTER COLUMN last_modified_at SET NOT NULL;
ALTER TABLE tb_order_service ALTER COLUMN created_by DROP DEFAULT;
ALTER TABLE tb_order_service ALTER COLUMN last_modified_by DROP DEFAULT;