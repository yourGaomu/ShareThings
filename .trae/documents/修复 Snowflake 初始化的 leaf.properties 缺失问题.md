## 问题原因
- 栈异常显示 `PropertyFactory` 在静态初始化时抛出 `NullPointerException`：`inStream parameter is null`。
- 根因：`PropertyFactory` 从类路径读取 `leaf.properties`（share-things-framework/leaf-spring-boot-starter/leaf-spring-boot-starter-core/src/main/java/com/zhangzc/leaf/core/common/PropertyFactory.java:14），当前项目中不存在该文件，导致 `getResourceAsStream("leaf.properties")` 返回 `null`。
- 该文件提供 `leaf.name`，被 `SnowflakeZookeeperHolder` 用于构建 ZK 路径与本地缓存文件（share-things-framework/leaf-spring-boot-starter/leaf-spring-boot-starter-core/src/main/java/com/zhangzc/leaf/core/snowflake/SnowflakeZookeeperHolder.java:33-35）。

## 修复方案
1. 添加配置文件（推荐）
- 在运行的应用模块加入 `src/main/resources/leaf.properties`（或在 core/starter 模块加入，均可被类路径加载）：
- 内容示例：
  - `leaf.name=share-things-leaf`
- 该值会用于 ZK 目录 `/snowflake/{leaf.name}` 与本地临时目录 `java.io.tmpdir/{leaf.name}/leafconf/{port}/workerID.properties`。

2. 代码健壮性优化（可选增强）
- 修改 `PropertyFactory`：当资源不存在时，记录警告并提供默认值，避免 NPE。
  - 逻辑：
    - 若 `getResourceAsStream("leaf.properties") == null`，则创建空 `Properties` 并 `prop.setProperty("leaf.name", "leaf")`。
- 保证即使缺少文件，Snowflake 仍可启动（使用默认 `leaf.name`）。

3. 启用/禁用雪花模式的运维开关
- 若短期内无需雪花模式，设置 `zhangzc.leaf.snowflake-enable: false`（share-things-admin/share-thing-admin-impl/src/main/resources/application.yml:29），应用即可在仅分段模式下成功启动。

## 验证步骤
- 添加 `leaf.properties` 后重启应用：
  - 期望日志包含 `Snowflake Service Init Successfully` 或至少不再出现 `PropertyFactory`/`SnowflakeZookeeperHolder` 的初始化异常。
- 若禁用雪花模式，确认应用在分段模式下可用，`GET /test/test2` 返回正常 ID。