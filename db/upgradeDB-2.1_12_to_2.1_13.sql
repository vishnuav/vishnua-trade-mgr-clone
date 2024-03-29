ALTER TABLE `tradeprod`.`contract` 
CHANGE COLUMN `tradingHours` `tradingHours` VARCHAR(100) NULL DEFAULT NULL ;

ALTER TABLE `tradeprod`.`contract` 
CHANGE COLUMN `localSymbol` `localSymbol` VARCHAR(20) NULL DEFAULT NULL ,
CHANGE COLUMN `symbol` `symbol` VARCHAR(20) NOT NULL ;

ALTER TABLE `tradeprod`.`tradeorder` 
ADD COLUMN `trailStopPrice` DECIMAL(10,2) NULL AFTER `transmit`,
ADD COLUMN `trailingPercent` DECIMAL(10,2) NULL AFTER `trailStopPrice`;