
CREATE TABLE IF NOT EXISTS test_table (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);

INSERT INTO test_table (id, name) VALUES (1, 'TestData');

-- Verify the setup
SELECT 'MySQL initialized successfully' as status;