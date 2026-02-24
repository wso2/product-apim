DELIMITER $$

DROP PROCEDURE IF EXISTS create_index_if_not_exists $$
CREATE PROCEDURE create_index_if_not_exists(
	in theTable varchar(128),
    in theIndexName varchar(128),
    in column_1 varchar(128),
    in column_2 varchar(128))
BEGIN
 IF((SELECT COUNT(*) AS index_exists 
		FROM information_schema.statistics 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND table_name = theTable 
        AND index_name = theIndexName) = 0) THEN
   SET @s = CONCAT('CREATE INDEX `' , theIndexName , '` ON `' , theTable, '` (`', column_1, '`, `', column_2, '`)');
   PREPARE stmt FROM @s;
   EXECUTE stmt;
 END IF;
END $$

DELIMITER ;

CALL create_index_if_not_exists("REG_RESOURCE_TAG", "REG_RESOURCE_TAG_IND_BY_REG_TAG_ID", "REG_TAG_ID", "REG_TENANT_ID");
CALL create_index_if_not_exists("REG_RESOURCE_PROPERTY", "REG_RESOURCE_PROPERTY_IND_BY_REG_PROP_ID", "REG_TENANT_ID", "REG_PROPERTY_ID");

DROP PROCEDURE IF EXISTS create_index_if_not_exists;