## 原因回顾
- `LeafAutoConfig` 使用了错误的条件：`@ConditionalOnProperty(prefix = "zhangzc.leaf.segment", name = "enable", ...)`，而实际配置是 `zhangzc.leaf.segmentEnable`/`segment-enable`，导致 `DataSource` 和 `SegmentService` 都不创建。
- 控制器 `com.zhangzc.sharethingadminimpl.controller.test` 依赖 `SegmentService`（share-thing-admin-impl/src/main/java/com/zhangzc/sharethingadminimpl/controller/test.java:18），因此启动阶段直接失败。

## 变更内容
- 修改自动配置条件，使其与现有 `application.yml` 匹配：
  - 在 `LeafAutoConfig`（share-things-framework/leaf-spring-boot-starter/leaf-spring-boot-starter-server/src/main/java/com/zhangzc/leaf/server/config/LeafAutoConfig.java:24, 37）将
    - `@ConditionalOnProperty(prefix = "zhangzc.leaf.segment", name = "enable", havingValue = "true")`
    - 改为 `@ConditionalOnProperty(prefix = "zhangzc.leaf", name = "segment-enable", havingValue = "true")`
- 可选优化：移除 `SegmentService` 上的 `@Component`（share-things-framework/leaf-spring-boot-starter/leaf-spring-boot-starter-server/src/main/java/com/zhangzc/leaf/server/service/SegmentService.java:18），完全由自动配置的 `@Bean` 在条件满足时创建，避免在未启用分段时强制要求数据源。

## 风险与兼容性
- 与现有 `application.yml`（share-thing-admin-impl/src/main/resources/application.yml:24-31）一致，不需要改动配置文件。
- `SnowflakeService` 的条件已正确（LeafAutoConfig.java:47-53），不受影响。
- 仅影响 `segment-enable=true` 时的 Bean 创建流程；其余场景行为不变。

## 验证步骤
- 清理并启动 `ShareThingAdminImplApplication`：确认日志出现数据源初始化和 `Segment Service Init Successfully`。
- 访问 `GET /test/test2`：应打印生成的 ID 并返回 `R.ok("test2")`。
- 如失败，检查数据库连接信息（`jdbcUrl`、`jdbc-username`、`jdbc-password`）是否可用。