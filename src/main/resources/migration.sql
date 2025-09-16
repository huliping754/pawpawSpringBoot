-- 修改外键约束，支持级联删除
-- 当删除 pets 表中的记录时，自动删除相关的 incomes 记录

-- 首先删除现有的外键约束
ALTER TABLE incomes DROP FOREIGN KEY incomes_ibfk_1;

-- 重新创建外键约束，使用 CASCADE 删除
ALTER TABLE incomes 
ADD CONSTRAINT incomes_ibfk_1 
FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE;
