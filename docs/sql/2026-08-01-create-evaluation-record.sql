-- 请在运行 customer 服务实际连接的 jzo2o-customer-all 库中执行本脚本。
-- 该库由 jzo2o-customer/src/main/resources/bootstrap.yml 的 mysql.db-name 配置决定。
USE `jzo2o-customer-all`;

CREATE TABLE IF NOT EXISTS `evaluation_record` (
  `id` bigint NOT NULL,
  `orders_id` bigint NOT NULL,
  `consumer_id` bigint NOT NULL,
  `serve_provider_id` bigint NOT NULL,
  `evaluation_type` tinyint NOT NULL COMMENT '1好评，2差评',
  `visible_status` tinyint NOT NULL DEFAULT 1 COMMENT '1可见，0隐藏',
  `content` varchar(500) NOT NULL,
  `picture_array` text NULL,
  `orders_no` varchar(64) NULL,
  `serve_item_name` varchar(128) NULL,
  `serve_item_img` varchar(512) NULL,
  `serve_address` varchar(512) NULL,
  `serve_start_time` datetime NULL,
  `consumer_name` varchar(64) NULL,
  `consumer_phone` varchar(32) NULL,
  `serve_provider_name` varchar(64) NULL,
  `is_anonymous` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_record_orders_id` (`orders_id`),
  KEY `idx_evaluation_record_consumer` (`consumer_id`, `visible_status`, `create_time`),
  KEY `idx_evaluation_record_provider` (`serve_provider_id`, `visible_status`, `evaluation_type`, `create_time`),
  KEY `idx_evaluation_record_operation` (`visible_status`, `evaluation_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单统一评价记录';
