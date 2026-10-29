CREATE TABLE IF NOT EXISTS plants (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(40) NOT NULL,
  day_temp VARCHAR(40) NULL,
  night_temp VARCHAR(40) NULL,
  light_note VARCHAR(80) NULL,
  seedling_days INT NULL,
  growth_days INT NULL,
  spike_days INT NULL,
  bloom_days INT NULL,
  day_temp_min DECIMAL(4, 1) NULL,
  day_temp_max DECIMAL(4, 1) NULL,
  night_temp_min DECIMAL(4, 1) NULL,
  night_temp_max DECIMAL(4, 1) NULL,
  humidity_min INT NULL,
  humidity_max INT NULL,
  light_min INT NULL,
  light_max INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plants_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS greenhouses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(40) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_greenhouses_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batches (
  id INT AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id INT NOT NULL,
  plant_id INT NOT NULL,
  planted_on DATE NOT NULL,
  stage ENUM('SEEDLING', 'GROWTH', 'SPIKE', 'BLOOM', 'READY') NOT NULL DEFAULT 'SEEDLING',
  note VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_batches_greenhouse FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id),
  CONSTRAINT fk_batches_plant FOREIGN KEY (plant_id) REFERENCES plants (id),
  INDEX idx_batches_greenhouse (greenhouse_id),
  INDEX idx_batches_plant (plant_id)
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

CREATE TABLE IF NOT EXISTS sensor_readings (
  greenhouse_id INT PRIMARY KEY,
  temperature DECIMAL(4, 1) NOT NULL,
  humidity TINYINT UNSIGNED NOT NULL,
  light INT NOT NULL,
  recorded_at DATETIME NOT NULL,
  CONSTRAINT fk_sensors_greenhouse FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE,
  CONSTRAINT chk_sensor_humidity CHECK (humidity <= 100),
  CONSTRAINT chk_sensor_light CHECK (light >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS greenhouse_controls (
  greenhouse_id INT PRIMARY KEY,
  heater BOOLEAN NOT NULL DEFAULT 0,
  cooler BOOLEAN NOT NULL DEFAULT 0,
  vent BOOLEAN NOT NULL DEFAULT 0,
  humidifier BOOLEAN NOT NULL DEFAULT 0,
  grow_light BOOLEAN NOT NULL DEFAULT 0,
  shade BOOLEAN NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_controls_greenhouse FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE
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
