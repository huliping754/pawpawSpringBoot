-- 宠物预约/入住记录
CREATE TABLE IF NOT EXISTS pets (
  id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  breed VARCHAR(100) NOT NULL,
  gender ENUM('弟弟','妹妹','未知') DEFAULT '未知',
  age INT NULL,
  neutered ENUM('已绝育','未绝育','未知') DEFAULT '未知',
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  daily_fee DECIMAL(10,2) NOT NULL,
  other_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  remark TEXT NULL,
  status ENUM('booked','checkedIn','checkedOut') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_pets_status_dates (status, start_date, end_date),
  INDEX idx_pets_start_date (start_date),
  INDEX idx_pets_end_date (end_date),
  INDEX idx_pets_status (status),
  CHECK (start_date < end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 成本流水
CREATE TABLE IF NOT EXISTS costs (
  id BIGINT NOT NULL PRIMARY KEY,
  water_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  electricity_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  rent_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  other_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_cost DECIMAL(10,2) NOT NULL,
  cost_date DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_costs_date (cost_date),
  CHECK (water_fee >= 0 AND electricity_fee >= 0 AND rent_fee >= 0 AND other_fee >= 0),
  CHECK (total_cost = water_fee + electricity_fee + rent_fee + other_fee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 系统配置
CREATE TABLE IF NOT EXISTS settings (
  `key` VARCHAR(64) NOT NULL PRIMARY KEY,
  `value` VARCHAR(255) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO settings(`key`, `value`) VALUES ('max_capacity', '10')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

-- 收入账单记录
CREATE TABLE IF NOT EXISTS incomes (
  id BIGINT NOT NULL PRIMARY KEY,
  pet_id BIGINT NOT NULL,
  daily_fee DECIMAL(10,2) NOT NULL,
  other_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_amount DECIMAL(10,2) NOT NULL,
  days_stayed INT NOT NULL,
  settled_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  remark TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_incomes_pet_id FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE RESTRICT,
  INDEX idx_incomes_pet_id (pet_id),
  INDEX idx_incomes_settled (settled_amount),
  INDEX idx_incomes_created (created_at),
  CHECK (daily_fee >= 0 AND other_fee >= 0 AND total_amount >= 0 AND days_stayed >= 0 AND settled_amount >= 0),
  CHECK (settled_amount <= total_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


