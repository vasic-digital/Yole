-- SPDX-FileCopyrightText: 2026 Milos Vasic
-- SPDX-License-Identifier: Apache-2.0
-- iter-58 F2 Phase 6 fixture: SQL.

CREATE TABLE IF NOT EXISTS greetings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO greetings (name, message) VALUES
    ('Yole', 'Hello, Yole!'),
    ('World', 'Hello, World!');

SELECT name, message
FROM greetings
WHERE created_at > '2026-01-01'
ORDER BY id ASC;
