# 管理端、服务人员与好评率派单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将管理端改为个人服务人员模式，并让服务人员详情、真实服务记录和区域派单统一使用好评率数据。

**Architecture:** 客户服务提供按服务人员批量计算好评率的内部 API；订单调度服务在候选人检索后批量填充该数据，再用确定性的好评率、接单量、距离和 ID 规则选人。订单管理服务返回真实完成服务记录，管理端以现有 API 模块消费这些内部接口并移除企业/用户分析 UI。

**Tech Stack:** Java 11、Spring Boot、OpenFeign、MyBatis-Plus、JUnit 5、Vue 3、TypeScript、TDesign、Vite。

## Global Constraints

- 不删除 `ConfigRegion` 的历史企业字段或数据库列；当前个人模式页面与请求不再读写它们。
- 仅统计 `evaluation_record` 中 `visible_status = 1` 的好评和差评；好评率为 `good / (good + bad) * 100`，保留一位小数。
- 没有有效评价的候选人低于已有评价的候选人；相同好评率按接单数升序、距离升序、服务人员 ID 升序。
- 调度评价查询失败不能阻断派单，必须降级为“暂无评价”并继续后续排序。
- 服务数据只返回 `SERVE_FINISHED` 的真实订单服务单；不得根据前端占位数据补造记录。
- 所有新增行为先写失败测试并确认失败，再写最小实现。

---

## 文件结构

| 文件 | 职责 |
| --- | --- |
| `jzo2o-api/.../EvaluationApi.java`、`.../ServeProviderGoodRateResDTO.java` | 调度和客户服务之间的批量好评率契约。 |
| `jzo2o-customer/.../InnerEvaluationController.java`、`UnifiedEvaluationService*.java` | 聚合可展示评价并实现批量接口。 |
| `jzo2o-orders/.../ServeProviderDTO.java`、`GoodRateDispatchRule.java`、`GoodRateDispatchStrategyImpl.java`、`OrdersDispatchServiceImpl.java` | 候选人数据、好评率排序与降级派单。 |
| `jzo2o-orders-manager/.../OrdersServeManagerServiceImpl.java` | 服务人员真实完成服务记录分页。 |
| `project-xzb-pc-admin-vue3-java/src/pages/dashboard/base/*` | 快捷入口和工作台分析区域。 |
| `project-xzb-pc-admin-vue3-java/src/pages/personnel/information/*`、`src/api/evaluation.ts` | 服务人员好评率和服务数据展示。 |
| `project-xzb-pc-admin-vue3-java/src/pages/service/region/*` | 个人服务人员调度配置。 |

### Task 1: 批量好评率内部契约

**Files:**
- Create: `jzo2o-api/src/main/java/com/jzo2o/api/customer/dto/response/ServeProviderGoodRateResDTO.java`
- Modify: `jzo2o-api/src/main/java/com/jzo2o/api/customer/EvaluationApi.java`
- Modify: `jzo2o-customer/src/main/java/com/jzo2o/customer/service/UnifiedEvaluationService.java`
- Modify: `jzo2o-customer/src/main/java/com/jzo2o/customer/service/impl/UnifiedEvaluationServiceImpl.java`
- Modify: `jzo2o-customer/src/main/java/com/jzo2o/customer/controller/inner/InnerEvaluationController.java`
- Test: `jzo2o-customer/src/test/java/com/jzo2o/customer/service/impl/UnifiedEvaluationRateBatchTest.java`

**Interfaces:**
- Consumes: `EvaluationRecord`, `EvaluationTypeEnum`, `EvaluationVisibilityEnum`。
- Produces: `EvaluationApi.queryServeProviderGoodRates(List<Long> serveProviderIds): Map<Long, ServeProviderGoodRateResDTO>`，其中 DTO 含 `boolean hasEvaluation` 与 `Double goodRate`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void should_return_visible_good_rate_for_each_requested_worker() {
    Map<Long, ServeProviderGoodRateResDTO> rates = service.rateForWorkers(List.of(11L, 22L));
    assertThat(rates.get(11L).getGoodRate()).isEqualTo(66.7D);
    assertThat(rates.get(11L).isHasEvaluation()).isTrue();
    assertThat(rates.get(22L).isHasEvaluation()).isFalse();
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `mvn -pl jzo2o-customer -am -Dtest=UnifiedEvaluationRateBatchTest test`

Expected: FAIL，原因是 `rateForWorkers` 和 API DTO 尚不存在。

- [ ] **Step 3: 写最小实现**

```java
Map<Long, ServeProviderGoodRateResDTO> rateForWorkers(List<Long> workerIds) {
    List<EvaluationRecord> records = evaluationRecordMapper.selectList(
        Wrappers.<EvaluationRecord>lambdaQuery()
            .in(EvaluationRecord::getServeProviderId, workerIds)
            .eq(EvaluationRecord::getVisibleStatus, VISIBLE));
    // 对每个请求 ID 聚合 GOOD/BAD，缺失记录返回 hasEvaluation=false。
}

@GetMapping("/good-rates")
public Map<Long, ServeProviderGoodRateResDTO> goodRates(
        @RequestParam List<Long> serveProviderIds) {
    return unifiedEvaluationService.rateForWorkers(serveProviderIds);
}
```

- [ ] **Step 4: 运行测试并确认通过**

Run: `mvn -pl jzo2o-customer -am -Dtest=UnifiedEvaluationRateBatchTest,EvaluationRateResDTOTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add jzo2o-api/src/main/java/com/jzo2o/api/customer \
  jzo2o-customer/src/main/java/com/jzo2o/customer
git commit -m "feat: expose batch provider good rates"
```

### Task 2: 好评率优先派单与稳定降级

**Files:**
- Create: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/rules/impl/GoodRateDispatchRule.java`
- Create: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/strategys/impl/GoodRateDispatchStrategyImpl.java`
- Modify: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/model/dto/ServeProviderDTO.java`
- Modify: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/enums/DispatchStrategyEnum.java`
- Modify: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/strategys/impl/AbstractDispatchStrategyImpl.java`
- Modify: `jzo2o-orders/jzo2o-orders-dispatch/src/main/java/com/jzo2o/orders/dispatch/service/impl/OrdersDispatchServiceImpl.java`
- Test: `jzo2o-orders/jzo2o-orders-dispatch/src/test/java/com/jzo2o/orders/dispatch/rules/GoodRateDispatchRuleTest.java`
- Test: `jzo2o-orders/jzo2o-orders-dispatch/src/test/java/com/jzo2o/orders/dispatch/strategys/GoodRateDispatchStrategyTest.java`

**Interfaces:**
- Consumes: `EvaluationApi.queryServeProviderGoodRates`, `ServeProviderDTO.id`, `acceptanceNum`, `acceptanceDistance`。
- Produces: `ServeProviderDTO.hasEvaluation`、`goodRate` 和 type `2` 对应的 `GOOD_RATE` 策略；选择结果不再随机。

- [ ] **Step 1: 写失败排序测试**

```java
@Test
void should_rank_rated_workers_by_rate_then_acceptance_distance_and_id() {
    List<ServeProviderDTO> actual = new GoodRateDispatchRule(null).doFilter(List.of(
        worker(30L, true, 90D, 1, 5),
        worker(20L, true, 90D, 1, 3),
        worker(10L, false, null, 0, 1)));
    assertThat(actual).extracting(ServeProviderDTO::getId).containsExactly(20L);
}

@Test
void should_put_unrated_workers_after_rated_workers() {
    assertThat(strategy.getPrecedenceServeProvider(List.of(
        worker(10L, false, null, 0, 1), worker(20L, true, 0D, 5, 10))).getId())
        .isEqualTo(20L);
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `mvn -pl jzo2o-orders/jzo2o-orders-dispatch -am -Dtest=GoodRateDispatchRuleTest,GoodRateDispatchStrategyTest test`

Expected: FAIL，原因是好评率字段和规则不存在。

- [ ] **Step 3: 写最小实现**

```java
Comparator<ServeProviderDTO> order = Comparator
    .comparing(ServeProviderDTO::getHasEvaluation, Comparator.reverseOrder())
    .thenComparing(ServeProviderDTO::getGoodRate,
        Comparator.nullsLast(Comparator.reverseOrder()))
    .thenComparing(ServeProviderDTO::getAcceptanceNum,
        Comparator.nullsFirst(Comparator.naturalOrder()))
    .thenComparing(ServeProviderDTO::getAcceptanceDistance,
        Comparator.nullsLast(Comparator.naturalOrder()))
    .thenComparing(ServeProviderDTO::getId);

// OrdersDispatchServiceImpl.dispatch():
// 读取一批候选人 ID，一次调用 EvaluationApi，填充 hasEvaluation/goodRate；
// 调用失败时统一填 hasEvaluation=false，并继续策略。
```

保留数据库配置值 `2`，仅将枚举名称与前端文案从评分改为好评率。对于好评率策略，ES 查询必须带距离排序并回填 `acceptanceDistance`，随后使用本地规则完成最终排序；`AbstractDispatchStrategyImpl` 在规则已经给出确定顺序时返回首项，不再随机选择。

- [ ] **Step 4: 运行测试并确认通过**

Run: `mvn -pl jzo2o-orders/jzo2o-orders-dispatch -am -Dtest=GoodRateDispatchRuleTest,GoodRateDispatchStrategyTest,IDispatchStrategyManagerTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add jzo2o-orders/jzo2o-orders-dispatch
git commit -m "feat: prioritize dispatch by provider good rate"
```

### Task 3: 服务人员真实服务记录

**Files:**
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersServeManagerServiceImpl.java`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/personnel/information/informationDetail.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/personnel/information/constants.ts`
- Test: `jzo2o-orders/jzo2o-orders-manager/src/test/java/com/jzo2o/orders/manager/service/OrdersServeManagerServiceImplTest.java`

**Interfaces:**
- Consumes: `OrdersServePageQueryByServeProviderReqDTO(serveProviderId, userType, pageNo, pageSize)`。
- Produces: `PageResult<ServeProviderServeResDTO>`，其中包含真实的订单号、服务名称、金额、结束时间和服务前后图片。

- [ ] **Step 1: 写失败测试**

```java
@Test
void should_include_a_recently_finished_service_for_worker() {
    PageResult<ServeProviderServeResDTO> page = service.pageQueryByServeProvider(queryFor(9001L));
    assertThat(page.getList()).extracting(ServeProviderServeResDTO::getId)
        .containsExactly(ordersServe.getId());
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `mvn -pl jzo2o-orders/jzo2o-orders-manager -am -Dtest=OrdersServeManagerServiceImplTest test`

Expected: FAIL，服务结束不足一小时的记录被现有 `realServeEndTime < now - 1h` 条件过滤。

- [ ] **Step 3: 写最小实现**

```java
LambdaQueryWrapper<OrdersServe> query = Wrappers.<OrdersServe>lambdaQuery()
    .eq(OrdersServe::getServeProviderId, req.getServeProviderId())
    .eq(OrdersServe::getServeStatus, ServeStatusEnum.SERVE_FINISHED.getStatus());
// 删除 realServeEndTime 的一小时过滤；保留分页排序。
```

管理端请求 `userType` 使用服务人员实际类型常量，并在表格中将评价列改为好评/差评文案，图片字段用现有图片预览组件渲染。空页只显示“暂无服务记录”。

- [ ] **Step 4: 运行测试并确认通过**

Run: `mvn -pl jzo2o-orders/jzo2o-orders-manager -am -Dtest=OrdersServeManagerServiceImplTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add jzo2o-orders/jzo2o-orders-manager project-xzb-pc-admin-vue3-java/src/pages/personnel/information
git commit -m "fix: show real worker service records"
```

### Task 4: 服务人员详情好评率

**Files:**
- Modify: `jzo2o-customer/src/main/java/com/jzo2o/customer/controller/operation/EvaluationController.java`
- Modify: `project-xzb-pc-admin-vue3-java/src/api/evaluation.ts`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/personnel/information/informationDetail.vue`
- Test: `jzo2o-customer/src/test/java/com/jzo2o/customer/controller/operation/EvaluationControllerTest.java`
- Test: `project-xzb-pc-admin-vue3-java/src/pages/personnel/information/good-rate.test.ts`

**Interfaces:**
- Consumes: `GET /customer/operation/evaluation/summary?serveProviderId={id}`。
- Produces: `EvaluationRateResDTO(hasEvaluation, goodRate, displayRate)`，供详情页展示。

- [ ] **Step 1: 写失败测试**

```java
@Test
void should_return_rate_for_requested_worker_to_operation() throws Exception {
    mockMvc.perform(get("/operation/evaluation/summary").param("serveProviderId", "9001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goodRate").value(100.0));
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `mvn -pl jzo2o-customer -am -Dtest=EvaluationControllerTest test`

Expected: FAIL，运营端尚无可指定服务人员的汇总接口。

- [ ] **Step 3: 写最小实现**

```java
@GetMapping("/summary")
public EvaluationRateResDTO summary(@RequestParam Long serveProviderId) {
    return unifiedEvaluationService.rateForWorker(serveProviderId);
}
```

详情页在加载基本信息后请求该接口；把“综合评分”改为“好评率”。`hasEvaluation=true` 显示 `displayRate` 和“基于 N 条评价”的统计；否则显示“暂无评价”，不显示伪造的百分比。

- [ ] **Step 4: 运行测试并确认通过**

Run: `mvn -pl jzo2o-customer -am -Dtest=EvaluationControllerTest,UnifiedEvaluationRateBatchTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add jzo2o-customer project-xzb-pc-admin-vue3-java/src/api/evaluation.ts \
  project-xzb-pc-admin-vue3-java/src/pages/personnel/information/informationDetail.vue
git commit -m "feat: display provider good rate in admin"
```

### Task 5: 工作台入口与个人化区域配置

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/dashboard/base/components/TopPanel.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/dashboard/base/index.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/service/region/model.ts`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/service/region/setBusiness.vue`
- Test: `project-xzb-pc-admin-vue3-java/src/pages/dashboard/base/dashboard-content.test.ts`
- Test: `project-xzb-pc-admin-vue3-java/src/pages/service/region/region-config.test.ts`

**Interfaces:**
- Consumes: 现有 `/evaluation/index` 路由与区域配置 API。
- Produces: 不含 `institutionReceiveOrderMax`、`institutionServeRadius` 的区域保存载荷，值 `2` 文案为“好评率优先”。

- [ ] **Step 1: 写失败测试**

```ts
it('uses evaluation management instead of enterprise authentication', () => {
  expect(quickEntries.map((item) => item.path)).toContain('/evaluation/index')
  expect(quickEntries.map((item) => item.title)).not.toContain('企业认证')
})

it('serializes only personal dispatch settings', () => {
  expect(buildRegionPayload(form)).not.toHaveProperty('institutionReceiveOrderMax')
  expect(buildRegionPayload(form)).not.toHaveProperty('institutionServeRadius')
  expect(dispatchLabel(2)).toBe('好评率优先')
})
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `npm test -- dashboard-content.test.ts region-config.test.ts`

Expected: FAIL，旧入口、用户分析组件和企业字段仍在页面/序列化结果中。

- [ ] **Step 3: 写最小实现**

```ts
const listData = [
  // 保留订单、服务人员认证、客户、服务项、区域入口
  { title: '评价管理', path: '/evaluation/index', url: evaluationSvg }
]

const buildRegionPayload = ({ staffReceiveOrderMax, staffServeRadius,
  seizeTimeoutInterval, dispatchPerRoundInterval, dispatchStrategy, cityCode }) => ({
  staffReceiveOrderMax, staffServeRadius, seizeTimeoutInterval,
  dispatchPerRoundInterval, dispatchStrategy, cityCode
})
```

从工作台删除 `OutputOverview` 导入、用户分析标题、图表组件和相关数据变量；区域页面删除企业表单、校验和赋值逻辑，同时将帮助说明改为好评率统计口径和无评价候选人规则。

- [ ] **Step 4: 运行测试并确认通过**

Run: `npm test -- dashboard-content.test.ts region-config.test.ts && npm run build`

Expected: PASS，管理端可构建。

- [ ] **Step 5: 提交本任务**

```bash
git add project-xzb-pc-admin-vue3-java/src/pages/dashboard \
  project-xzb-pc-admin-vue3-java/src/pages/service/region
git commit -m "feat: simplify admin dashboard and region dispatch settings"
```

### Task 6: 端到端回归与文档

**Files:**
- Modify: `docs/superpowers/specs/2026-08-02-admin-worker-good-rate-dispatch-design.md`（仅在实现偏离已批准设计时同步调整）
- Test: `project-xzb-pc-admin-vue3-java/package.json` 的现有测试命令与三个 Maven 模块测试。

**Interfaces:**
- Consumes: 前五个任务的 API、页面和排序规则。
- Produces: 可复现的验证记录；不新增兼容性旁路。

- [ ] **Step 1: 写失败回归用例**

```java
@Test
void should_choose_rated_worker_before_unrated_worker_when_good_rate_strategy_is_selected() {
    assertThat(dispatch(candidate(1L, false), candidate(2L, true))).isEqualTo(2L);
}
```

- [ ] **Step 2: 运行用例并确认失败**

Run: `mvn -pl jzo2o-orders/jzo2o-orders-dispatch -am -Dtest=GoodRateDispatchStrategyTest test`

Expected: FAIL，直到 Task 2 的真实批量好评率填充与规则接通。

- [ ] **Step 3: 最小整合实现**

```text
启动 jzo2o-customer、jzo2o-orders-manager、jzo2o-orders-dispatch 与管理端；
创建一名有好评和一名无评价的可接单服务人员；完成一张服务单并提交服务前后图片；
在管理端验证服务记录、好评率、区域配置和工作台入口。
```

- [ ] **Step 4: 运行完整验证并确认通过**

Run: `mvn -pl jzo2o-customer,jzo2o-orders/jzo2o-orders-manager,jzo2o-orders/jzo2o-orders-dispatch -am test`

Run: `npm test && npm run build`（目录：`project-xzb-pc-admin-vue3-java`）

Expected: 全部 PASS；手工页面检查无企业字段、无用户分析、无综合评分。

- [ ] **Step 5: 提交本任务**

```bash
git add docs/superpowers/specs/2026-08-02-admin-worker-good-rate-dispatch-design.md
git commit -m "test: verify good-rate dispatch workflow"
```
