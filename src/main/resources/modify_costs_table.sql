-- 修改 costs 表，将 cost_date 字段改为 cost_month
-- 删除原有的 cost_date 字段
ALTER TABLE costs DROP COLUMN cost_date;

-- 添加新的 cost_month 字段，格式为 YYYY-MM
ALTER TABLE costs ADD COLUMN cost_month VARCHAR(7) NOT NULL COMMENT '成本月份，格式：YYYY-MM';
