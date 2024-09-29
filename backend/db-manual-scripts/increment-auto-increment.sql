SET
@current_minute = FLOOR(UNIX_TIMESTAMP() / 60);

SET
@sql = CONCAT('ALTER TABLE trade AUTO_INCREMENT = ', @current_minute);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
