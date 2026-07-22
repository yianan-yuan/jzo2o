# Task 9 报告：服务卡片、SSE 协议和受控编排

## 状态

DONE

实现严格限制在简报列出的 11 个生产文件和 2 个测试文件；另按任务要求新增本报告。未修改 Task 1–8 文件、POM 或配置文件。

## TDD 记录

### 第一轮：缺类型与核心路径

先创建 `AssistantOrchestratorTest`、`AiAssistantControllerTest` 的 4 个核心用例，再运行：

```powershell
$env:JAVA_HOME='D:\develop\Java\jdk-11'
& 'D:\develop\IntelliJIDEA2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -s '.superpowers\sdd\maven-settings.xml' -f 'jzo2o-aigc\pom.xml' `
  '-DskipTests=false' '-Dmaven.test.skip=false' `
  '-Dtest=AssistantOrchestratorTest,AiAssistantControllerTest' test
```

RED：`testCompile` 按预期报告 12 个 Task 9 类型缺失，包括 orchestrator、catalog、reply、SSE sink、controller 与 DTO；不是测试拼写或环境失败。

实现最小骨架、正常推荐和澄清分支后，GREEN：4/4 PASS。

### 第二轮：编排、reference 与降级

补充无匹配、未知单位、reference、模型降级、目录异常、快捷问题和 prompt 隔离用例后，RED 为 18 次执行中的 7 failures / 5 errors，准确暴露当前缺失分支。实现后 GREEN：18/18 PASS。

### 第三轮：SSE 与控制器生命周期

先补精确 SSE 数据、单终止、I/O 失败、95 秒 emitter、30/90 秒 timer、首 delta、timeout/disconnect/completion、lease 重试和 future 赋值竞态用例。RED 首先因 `SseEmitterEventSink` 缺失而在 `testCompile` 失败；实现后 GREEN：16/16 PASS。所有 timer 测试使用 direct/queued executor 和手动 scheduler，无 `sleep` 或真实等待。

随后针对 Bean Validation provider 缺失，增加“非法 DTO 必须在 owned load/guard 之前返回 400”的用例。RED 为未抛异常；加入控制器等价前置校验后 GREEN。

## 实现摘要

### 编排与权威事实

- 固定事件顺序：
  - 澄清：`UNDERSTANDING -> GENERATING -> delta -> done:CLARIFYING`
  - 推荐：`UNDERSTANDING -> SEARCHING_SERVICES -> GENERATING -> delta -> recommendations -> done:RECOMMENDING`
  - 无匹配：`UNDERSTANDING -> SEARCHING_SERVICES -> GENERATING -> delta -> done:NO_MATCH`
- 卡片名称、图片、价格、单位和 ID 只来自 `ServeAggregationResDTO`；模型仅提供选择 ID 和理由。
- 单位只接受 `1小时、2天、3次、4台、5个、6㎡、7米`。未知单位记录目录数据异常并丢卡，不猜测默认单位。
- session 更新 city、profile、stage、推荐 ID 和必要 user/assistant turns，并在成功终止前保存。
- 无候选或无有效卡片输出固定安全文本并进入 `NO_MATCH`。

### reference 分支

- 以 `referencedRecommendationIndex` 从 session 的 `lastRecommendedServeIds` 按 1-based 取 ID。
- 通过 `ServiceCatalogService.findById` / `ServeApi.findById` 获取最新详情，不执行关键词 search。
- 复核非空、`saleStatus=2`、当前 city 和合法单位；失效时输出澄清文本并 `done:NO_MATCH`。

### 模型不可用降级

- 任一模型阶段抛 `MODEL_UNAVAILABLE` 后只进入一次无模型降级。
- 原消息先 `trim`、连续空白归一，再以 `(?<!\d)\d{6,}(?!\d)` 将独立 6 位以上数字串替换为 `***`。
- 只调用一次 `catalog.search(cityCode, normalizedMessage, 3)`；有效结果使用固定理由和模板 delta，空结果发送 `AIGC_MODEL_UNAVAILABLE`。
- catalog/Feign 异常发送 `AIGC_SERVICE_CATALOG_UNAVAILABLE`；`MODEL_OUTPUT_INVALID` 等稳定错误直接发送 error。
- 快捷问题完全由服务端固定生成，最多 3 条，不额外调用模型。

### SSE 协议和单终止

- 事件名严格为 `status`、`delta`、`recommendations`、`done`、`error`。
- 数据形状：
  - status：`{stage}`
  - delta：`{text}`
  - recommendations：卡片数组
  - done：`{stage,suggestedQuestions}`，问题截断到 3 条
  - error：`{code,message,retryable}`
- `SseEmitterEventSink` 使用同步发送门和原子 terminal 门；每条流最多一个 done 或 error，终止后忽略所有事件。
- 首个有效非空 delta 回调一次；发送 I/O 失败调用 `completeWithError` 并执行终止清理。

### 控制器与 timer

- user ID 仅取自 `UserInfoHandler.currentUserInfo().getId()`；空用户抛 `UNAUTHORIZED`。
- 顺序固定为 auth -> DTO 前置校验 -> `loadOwned` -> `guard.acquire` -> 创建 `SseEmitter(95_000)`。
- controller 将已加载 `AigcSession` 交给 orchestrator，避免异步重复加载；同时保留 `run(userId, sessionId, ...)` 测试/调用入口。
- 专用 executor 与 scheduler；30 秒首 token timer 和 90 秒总 timer 均由配置值调度。
- 首个有效 delta 只取消 30 秒 timer；90 秒 timer 保留到 terminal。
- timer 触发时先取消 token，再发送 `AIGC_REQUEST_TIMEOUT`。
- done/error、异步异常、emitter timeout、连接 error、completion 和发送失败均清理两个 future 并关闭 lease。
- 生命周期协调器覆盖“future 返回前已 terminal”的竞态；lease close 失败不破坏 SSE，也不标记关闭，后续 cleanup 会重试。

## Bean Validation 运行时核验

依赖树显示当前模块只有：

```text
javax.validation:validation-api:2.0.1.Final
```

没有 Hibernate Validator provider；直接调用 `Validation.buildDefaultValidatorFactory()` 会抛 `NoProviderFoundException`。因此测试以反射锁定 DTO 上的 `@NotBlank`、`@Size(max=1000)`、`@Pattern("[0-9]{3,6}")` 和 controller 参数 `@Valid`，没有修改 POM。

为确保当前接口在实际运行时仍满足流前校验，controller 同时执行与注解等价的 message/city 检查，非法请求以 HTTP 400 `ResponseStatusException` 结束，且测试验证不会访问 session、guard 或 orchestrator。关注点：未来其他接口若只依赖 `@Valid`，仍应在平台依赖层补充 provider；本 Task 9 接口已由等价校验覆盖。

## 测试覆盖

- 简报两条核心场景与完整事件顺序。
- 权威卡片所有字段、单位映射、未知单位丢弃。
- 空 catalog、空有效卡片、固定 NO_MATCH 文本与问题。
- reference 有效、缺失、下架、跨城、未知单位以及不调用 keyword search。
- 模型不可用归一/数字脱敏、空结果、reply 阶段降级、固定理由/模板。
- catalog 异常、其他稳定模型错误直传。
- ReplyGeneration 独立 system/user 消息与 JSON 事实隔离。
- status/delta/recommendations/done/error 数据形状、单 done/error、空 delta、I/O 失败。
- auth/归属/guard 顺序、DTO 注解与等价 runtime 校验、create 不启动模型链。
- 95 秒 emitter、30/90 秒 timer、首 delta、terminal/timeout/disconnect/completion 清理、lease 重试和赋值竞态。

## 最终验证

```powershell
$env:JAVA_HOME='D:\develop\Java\jdk-11'
& 'D:\develop\IntelliJIDEA2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' `
  -s '.superpowers\sdd\maven-settings.xml' -f 'jzo2o-aigc\pom.xml' `
  '-DskipTests=false' '-Dmaven.test.skip=false' test
```

结果：`BUILD SUCCESS`；98 tests run，0 failures，0 errors，0 skipped；输出干净。
