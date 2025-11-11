PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

-- USERS
CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  full_name     TEXT    NOT NULL,
  email         TEXT    NOT NULL UNIQUE,
  avatar        TEXT,
  password_hash TEXT    NOT NULL DEFAULT '',
  created_at    INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000),
  updated_at    INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000)
);

CREATE TRIGGER IF NOT EXISTS users_au_touch_updated_at
AFTER UPDATE ON users
BEGIN
  UPDATE users
     SET updated_at = CAST(strftime('%s','now') AS INTEGER) * 1000
   WHERE rowid = NEW.rowid;
END;

-- HIKES
CREATE TABLE IF NOT EXISTS hikes (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id          INTEGER NOT NULL,
  name             TEXT    NOT NULL,
  location         TEXT    NOT NULL,
  hike_date        INTEGER NOT NULL CHECK(hike_date >= 0),
  parking          INTEGER NOT NULL DEFAULT 0 CHECK(parking IN (0,1)),
  length_km        REAL    NOT NULL CHECK(length_km > 0),
  difficulty       TEXT    NOT NULL CHECK(difficulty IN ('Easy','Moderate','Hard')),
  description      TEXT,
  elevation_gain_m INTEGER CHECK(elevation_gain_m IS NULL OR elevation_gain_m >= 0),
  max_group_size   INTEGER CHECK(max_group_size IS NULL OR max_group_size > 0),
  cover_image      TEXT,
  created_at       INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000),
  updated_at       INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TRIGGER IF NOT EXISTS hikes_au_touch_updated_at
AFTER UPDATE ON hikes
BEGIN
  UPDATE hikes
     SET updated_at = CAST(strftime('%s','now') AS INTEGER) * 1000
   WHERE rowid = NEW.rowid;
END;

-- OBSERVATIONS
CREATE TABLE IF NOT EXISTS observations (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  hike_id      INTEGER NOT NULL,
  note         TEXT, -- để trống được (UI của bạn đang để tùy chọn)
  observed_at  INTEGER NOT NULL DEFAULT (CAST(strftime('%s', 'now') AS INTEGER) * 1000),
  comments     TEXT,
  FOREIGN KEY(hike_id) REFERENCES hikes(id) ON DELETE CASCADE
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_hikes_user  ON hikes(user_id);
CREATE INDEX IF NOT EXISTS idx_hikes_date  ON hikes(hike_date);
CREATE INDEX IF NOT EXISTS idx_hikes_name  ON hikes(name COLLATE NOCASE);
CREATE INDEX IF NOT EXISTS idx_obs_hike    ON observations(hike_id);

-- SEED (tùy chọn). Vì password_hash có DEFAULT '', không cần truyền.
INSERT INTO users (id, full_name, email, avatar, created_at, updated_at)
VALUES (1, 'David Nguyen', 'david@example.com', NULL,
        CAST(strftime('%s','now') AS INTEGER) * 1000,
        CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT INTO hikes
  (id, user_id, name, location, hike_date, parking, length_km, difficulty,
   description, elevation_gain_m, max_group_size, created_at, updated_at, cover_image)
VALUES
  (1, 1, 'Lang Biang – Sunrise', 'Đà Lạt', 1761389660000, 1, 12.5, 'Moderate',
   NULL, NULL, NULL, 1761389660000, 1761389660000, 'content://sample/langbiang.jpg');

INSERT INTO observations (id, hike_id, note, observed_at, comments)
VALUES
  (1, 1, 'Thấy chim mỏ sừng', 1761389719000, 'khoảng km 3'),
  (2, 1, 'Mây đẹp',            1761389719000, 'đi sáng');

COMMIT;
