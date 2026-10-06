-- =============================================================================
-- 支付信息表 mall_pay_info
--
-- 【重要】本脚本取自**真实运行库**的 `SHOW CREATE TABLE`（读取日期 2026-10-06），
--   因此它是"如实还原"而不是"我设计的"—— 表结构里几处不那么理想的地方
--   （见文末【设计提示】）也一并保留，没有擅自"优化"，以免与实际部署不一致。
--
--   该表由支付服务（pay）写入，与商城主服务（mall）**共用同一个数据库**；
--   mall 侧不直接读写它（无对应 Mapper），只通过 MQ 消息感知支付结果。
--
--   列定义与两端代码的对应关系：
--     · src/main/java/com/imooc/pay/pojo/PayInfo.java        —— pay 侧实体（写入）
--     · src/main/resources/mappers/PayInfoMapper.xml         —— pay 侧 SQL 映射
--     · mall 侧 com/imooc/mall/listenVo/PayInfo.java         —— 消费 MQ 消息用的 DTO
-- =============================================================================

CREATE TABLE IF NOT EXISTS `mall_pay_info` (
  `id`              int(11)       NOT NULL AUTO_INCREMENT,
  `user_id`         int(11)                DEFAULT NULL COMMENT '用户id',
  `order_no`        bigint(20)    NOT NULL COMMENT '订单号',
  `pay_platform`    int(10)                DEFAULT NULL COMMENT '支付平台:1-支付宝,2-微信',
  `platform_number` varchar(200)           DEFAULT NULL COMMENT '支付流水号',
  `platform_status` varchar(20)            DEFAULT NULL COMMENT '支付状态',
  `pay_amount`      decimal(20,2) NOT NULL COMMENT '支付金额',
  `create_time`     datetime               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`,`order_no`) USING BTREE,
  UNIQUE KEY `uqe_order_no` (`order_no`),
  UNIQUE KEY `uqe_platform_number` (`platform_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='支付信息表';

-- =============================================================================
-- 【设计提示 / 已知限制】—— 与代码行为直接相关，面试或接手时都会被问到
--
-- 1) `uqe_order_no` 是**唯一约束**，而 `PayServiceImpl.create()` 每次调用都会
--    `insertSelective` 插入一条新记录。因此**对同一个订单重复发起支付会抛出
--    唯一键冲突**（Duplicate entry）。生产做法应是「一个订单一条支付记录」：
--    先按 order_no 查，存在则 UPDATE（或 `insert ... on duplicate key update`）。
--
-- 2) 主键是 `(id, order_no)` 复合主键，而 `id` 本身已是自增 —— 与 `uqe_order_no`
--    在语义上冗余。本脚本按真实库保留原样，未做简化。
--
-- 3) `uqe_platform_number`（支付流水号唯一）：MySQL 的唯一索引允许多个 NULL，
--    所以尚未支付的记录（platform_number 为 NULL）不受影响；但要注意第三方返回
--    空字符串（''）而非 NULL 时会互相冲突。
--
-- 4) 字符集沿用真实库的 `utf8`(utf8mb3)。新部署建议改用 `utf8mb4`（超集，可存
--    完整 emoji 等 4 字节字符），本仓库的 mall 建表脚本用的是 utf8mb4。
--
-- 5) 幂等依赖 `platform_status`：回调重复到达时，只有非 SUCCESS 状态才会被更新
--    （见 PayServiceImpl.asyncNotify）。这是当前实现里唯一的幂等手段。
-- =============================================================================
