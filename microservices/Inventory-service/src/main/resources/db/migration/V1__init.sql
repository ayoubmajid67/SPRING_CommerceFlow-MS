CREATE TABLE t_inventory
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(255),
    quantity INT
);

INSERT INTO t_inventory (sku_code, quantity)
VALUES ('iphone_13', 100),
       ('iphone_13_red', 0);
