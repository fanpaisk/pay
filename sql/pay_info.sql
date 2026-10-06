-- 支付信息表
-- 列定义与 src/main/resources/mappers/PayInfoMapper.xml 的 resultMap 一一对应
-- 注意：本表与商城订单服务（mall）同库，默认库名 mall，表名 mall_pay_info

CREATE TABLE IF NOT EXISTS `mall_pay_info` (
  `id`              int(11)       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         int(11)                DEFAULT NULL COMMENT '下单用户 id',
  `order_no`        bigint(20)    NOT NULL COMMENT '订单号（对应商城主订单表 order_no）',
  `pay_platform`    int(1)                 DEFAULT NULL COMMENT '支付平台：1-支付宝，2-微信',
  `platform_number` varchar(255)           DEFAULT NULL COMMENT '第三方平台支付流水号',
  `platform_status` varchar(20)            DEFAULT NULL COMMENT '支付状态（best-pay-sdk 的 OrderStatusEnum.name，如 NOTPAY / SUCCESS / REFUND_SUCCESS）',
  `pay_amount`      decimal(20,2)          DEFAULT NULL COMMENT '支付金额',
  `create_time`     datetime               DEFAULT NULL COMMENT '创建时间',
  `update_time`     datetime               DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付信息表';

-- 说明：
--   1. order_no 上建的是普通索引而非唯一索引 —— 当前实现对同一订单重复调用 /pay/create 会再次 INSERT，
--      若加唯一约束会直接报重复键。生产环境更合理的做法是「一个订单一条支付记录 + update 而不是 insert」，
--      并把 order_no 设为唯一索引，见 README「已知限制」。
--   2. 幂等依赖 platform_status：回调重复到达时，只有非 SUCCESS 状态才会被更新。
