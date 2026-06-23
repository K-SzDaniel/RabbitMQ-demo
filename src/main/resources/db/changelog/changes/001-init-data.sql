--liquibase formatted sql

--changeset miltdev:002-init-30000-pending-invoices
INSERT INTO invoices (
    invoice_number,
    customer_name,
    customer_email,
    customer_address,
    issue_date,
    due_date,
    subtotal,
    tax_amount,
    total_amount,
    currency,
    status,
    created_at,
    updated_at
)
SELECT
    CONCAT('INV-', LPAD(seq.n + 1, 6, '0')) AS invoice_number,
    CONCAT('Customer ', seq.n + 1) AS customer_name,
    CONCAT('customer', seq.n + 1, '@example.com') AS customer_email,
    CONCAT('Address ', seq.n + 1) AS customer_address,
    CURRENT_DATE AS issue_date,
    DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY) AS due_date,
    100.00 AS subtotal,
    27.00 AS tax_amount,
    127.00 AS total_amount,
    'EUR' AS currency,
    'PENDING' AS status,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM (
    SELECT
        ones.n
        + tens.n * 10
        + hundreds.n * 100
        + thousands.n * 1000
        + ten_thousands.n * 10000 AS n
    FROM
        (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ten_thousands
) seq
WHERE seq.n < 30000;

--rollback DELETE FROM invoices WHERE invoice_number BETWEEN 'INV-000001' AND 'INV-030000';
