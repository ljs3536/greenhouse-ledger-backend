CREATE TABLE IF NOT EXISTS houses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(8) NOT NULL UNIQUE,
  name VARCHAR(40) NOT NULL,
  crop_code VARCHAR(32) NOT NULL UNIQUE,
  crop_name VARCHAR(40) NOT NULL,
  day_temp VARCHAR(40) NOT NULL,
  night_temp VARCHAR(40) NOT NULL,
  light_note VARCHAR(80) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batches (
  id INT AUTO_INCREMENT PRIMARY KEY,
  house_id INT NOT NULL,
  planted_on DATE NOT NULL,
  stage ENUM('SEEDLING', 'GROWTH', 'SPIKE', 'BLOOM', 'READY') NOT NULL DEFAULT 'SEEDLING',
  note VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_batches_house FOREIGN KEY (house_id) REFERENCES houses (id),
  INDEX idx_batches_house (house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS movements (
  id INT AUTO_INCREMENT PRIMARY KEY,
  batch_id INT NOT NULL,
  type ENUM('INBOUND', 'DISPOSAL', 'SALE') NOT NULL,
  quantity INT NOT NULL,
  occurred_on DATE NOT NULL,
  memo VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_movements_batch FOREIGN KEY (batch_id) REFERENCES batches (id) ON DELETE CASCADE,
  CONSTRAINT chk_movements_quantity CHECK (quantity > 0),
  INDEX idx_movements_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stage_changes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  batch_id INT NOT NULL,
  from_stage ENUM('SEEDLING', 'GROWTH', 'SPIKE', 'BLOOM', 'READY') NOT NULL,
  to_stage ENUM('SEEDLING', 'GROWTH', 'SPIKE', 'BLOOM', 'READY') NOT NULL,
  changed_on DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stage_batch FOREIGN KEY (batch_id) REFERENCES batches (id) ON DELETE CASCADE,
  INDEX idx_stage_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
