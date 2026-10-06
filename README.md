# pay —— 商城支付服务（微信 Native 扫码支付）

商城项目的**独立支付模块**，单独部署为一个 Spring Boot 应用，与商城主服务（`mall`）通过 **RabbitMQ 异步解耦**：
支付结果不直接回写订单，而是发消息通知 `mall`，由 `mall` 侧的 `PayMsgListener` 更新订单状态。

## 技术栈

| 项 | 值 |
|---|---|
| 框架 | Spring Boot 2.1.7 / Java 8 |
| 持久层 | MyBatis + MySQL |
| 消息 | RabbitMQ（`spring-boot-starter-amqp`） |
| 支付 | `cn.springboot:best-pay-sdk:1.3.0`（微信 Native 扫码） |
| 模板 | Freemarker（渲染支付二维码页面） |

## 支付链路

```
mall 下单 ──POST /pay/create(orderId, amount)──▶ pay
                                                │ 1. 写入 mall_pay_info（状态 NOTPAY）
                                                │ 2. best-pay-sdk 统一下单 → 二维码链接
                                                │ 3. Freemarker 渲染 createForWxNative.ftl
   ┌────────────────────────────────────────────┘
   │  用户扫码支付
   ▼
微信 ──POST /pay/notify（异步回调）──▶ pay
                                       │ 1. bestPayService.asyncNotify 验签
                                       │ 2. 按 orderNo 查询本地支付记录
                                       │ 3. 金额校验（BigDecimal#compareTo 与回调金额比对）
                                       │ 4. 幂等：platform_status 非 SUCCESS 才更新
                                       │ 5. 更新支付记录 → 发 MQ（队列 payNotify）
                                       └─▶ mall：@RabbitListener(queues="payNotify") → orderService.paid(orderNo)
```

回调处理顺序（**验签 → 查单 → 金额校验 → 幂等判断 → 更新 → 发消息 → 返回微信成功**）是本模块的核心设计点：
把验签放在最前、金额校验放在状态变更之前，任何一步失败都不会污染支付状态。

## 目录结构

```
src/main/java/com/imooc/pay/
├── PayApplication.java              # 启动类
├── config/BestPayConfig.java        # BestPayService / WxPayConfig Bean 装配
├── config/WxAccountConfig.java      # @ConfigurationProperties(prefix="wx") 读取商户配置
├── controller/PayController.java    # /pay/create、/pay/notify
├── dao/PayInfoMapper.java           # MyBatis Mapper 接口
├── pojo/PayInfo.java                # 支付记录实体
└── service/impl/PayServiceImpl.java # 下单、回调处理、金额校验与幂等
src/main/resources/
├── application.yml                  # 全部敏感项走环境变量占位符
├── mappers/PayInfoMapper.xml        # SQL 映射
└── templates/createForWxNative.ftl  # 扫码支付页
sql/pay_info.sql                     # 建表脚本
```

## 快速开始

1. **建库建表**：在 MySQL 中执行 `sql/pay_info.sql`（表位于与商城同库的数据库中，默认库名 `mall`）。
2. **准备中间件**：需要可用的 MySQL 与 RabbitMQ。注意 `mall` 侧监听的 `payNotify` 队列**需要预先存在**，否则消费者启动会失败。
3. **配置**：敏感项全部通过环境变量注入（见下表）。也可以把真实值写进 `src/main/resources/application-local.yml`
   （该文件已被 `.gitignore` 排除），并以 `--spring.profiles.active=local` 启动。
4. **启动**：`mvn spring-boot:run`（默认端口 8083）。

### 配置项

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | JDBC 连接串 | `jdbc:mysql://localhost:3306/mall?...` |
| `DB_USER` / `DB_PASSWORD` | 数据库账号 | `root` / 空 |
| `RABBITMQ_HOST` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ 连接 | `127.0.0.1` / `guest` / `guest` |
| `WX_APPID` / `WX_MCHID` / `WX_MCHKEY` | 微信商户配置 | 空（必填，否则下单会失败） |
| `WX_NOTIFY_URL` / `WX_RETURN_URL` | 微信回调地址 / 返回地址 | 本地地址 |

## 已知限制与可改进点

诚实记录当前实现的边界（也是这个模块最值得讨论的地方）：

1. **`/pay/create` 未做鉴权**，且 `amount` 由调用方传入；服务端没有与商城主订单的应付金额做比对。生产环境应由服务端按 `orderNo` 反查订单金额，忽略外部传入值。
2. **回调消息无法判别真伪业务**：微信回调本身有验签，但若有人直接构造 `/pay/notify` 请求，本服务会写入支付成功记录并通知订单服务。
3. **幂等仅依赖 `platform_status`**：能挡住微信的重复通知，但没有独立的幂等表/去重键，也没有对 MQ 消费失败的重试与死信处理。
4. **`payNotify` 队列未在代码中声明**（没有 `Queue`/`Exchange` Bean），依赖 `mall` 侧或运维预先创建。
5. **无自动化测试断言**：`src/test` 下的测试类依赖真实 MySQL/RabbitMQ，且以日志打印为主，尚不能作为回归保障。
6. 商城侧扣减库存**未使用行锁或乐观锁**，并发下单存在超卖风险（属订单服务问题，一并记录）。

## 声明

初始代码骨架来自慕课网《SpringBoot 商城》课程，本仓库为其**补全与二次开发版本**；
脱敏改造（敏感配置全部改为环境变量注入、补充建表脚本与文档）与上述改进点整理为二次开发内容。
