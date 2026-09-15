# 统一评价体系 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 将评价改为订单级好评或差评，保存必填文字和可选图片；小程序、服务人员 App、管理端和好评率使用同一份本地数据。

**Architecture:** 在 jzo2o-customer 新增本地 evaluation_record 表与统一评价服务，替代运行时对旧独立评价系统的 HTTP 调用。三个角色通过受限 REST API 查询同一记录；前端不保留星级、综合得分或独立系统跳转。

**Tech Stack:** Spring Boot、MyBatis-Plus、MySQL、Vue 3、uni-app、TDesign、JUnit 5、Node test runner。

## Global Constraints

- 类型仅 GOOD 或 BAD；没有中评与星级评分。
- 提交时类型必选，trim 后文字为 1 至 500 字，图片可选且最多 6 张。
- 好评率只统计可见记录：GOOD / (GOOD + BAD) * 100；分母为 0 时返回 hasEvaluation=false。
- 管理端只允许隐藏和恢复，禁止物理删除；隐藏记录不对客户和服务人员公开，也不计入好评率。
- 不再跳转、取 token 或请求旧独立评价系统；历史导入只能是一次性迁移工具，不能成为查询依赖。
- 修改只在现有 main，不建分支、不推送。

---

### Task 1: 建立本地评价数据模型和迁移脚本

**Files:**
- Create: docs/sql/2026-08-01-create-evaluation-record.sql
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/domain/EvaluationRecord.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/mapper/EvaluationRecordMapper.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/enums/EvaluationTypeEnum.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/enums/EvaluationVisibilityEnum.java
- Test: jzo2o-customer/src/test/java/com/jzo2o/customer/service/UnifiedEvaluationPersistenceTest.java

**Interfaces:**
- Produces EvaluationRecord fields: ordersId, consumerId, serveProviderId, evaluationType, visibleStatus, content, pictureArray, and order/service snapshots.
- Produces GOOD/BAD and VISIBLE/HIDDEN enums for every later query.

- [ ] **Step 1: Write the failing persistence test**

~~~java
@Test
void evaluationRecordStoresOneOrderAndOptionalImages() {
    EvaluationRecord record = record(101L, EvaluationTypeEnum.GOOD, "[\"https://cdn/a.png\"]");
    mapper.insert(record);

    assertEquals(1, mapper.selectCount(Wrappers.<EvaluationRecord>lambdaQuery()
        .eq(EvaluationRecord::getOrdersId, 101L)));
}
~~~

- [ ] **Step 2: Run it and verify it fails because the record model/table does not exist**

Run: mvn -pl jzo2o-customer -Dtest=UnifiedEvaluationPersistenceTest test

Expected: compilation or database-mapping failure naming EvaluationRecord or evaluation_record.

- [ ] **Step 3: Add schema and MyBatis-Plus record**

The SQL creates evaluation_record with a unique orders_id key, indexes for consumer, service personnel, type, visibility, and create time. It stores order/service snapshots: ordersNo, serveItemName, serveItemImg, serveAddress, serveStartTime, consumerName, consumerPhone, and serveProviderName. Store image URLs as JSON text and default visibleStatus to VISIBLE.

~~~java
@TableName("evaluation_record")
public class EvaluationRecord extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long ordersId;
    private Long consumerId;
    private Long serveProviderId;
    private Integer evaluationType;
    private Integer visibleStatus;
    private String content;
    private String pictureArray;
}
~~~

- [ ] **Step 4: Apply SQL locally, rerun the test, and commit**

Run: mvn -pl jzo2o-customer -Dtest=UnifiedEvaluationPersistenceTest test

Expected: PASS; the database rejects a second record for the same ordersId.

Commit: feat: add local evaluation record model

### Task 2: 用本地统一服务替换旧评价 HTTP 依赖

**Files:**
- Modify: jzo2o-api/src/main/java/com/jzo2o/api/customer/dto/request/EvaluationSubmitReqDTO.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/dto/request/EvaluationPageReqDTO.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/dto/request/EvaluationVisibilityReqDTO.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/dto/response/EvaluationDetailResDTO.java
- Create: jzo2o-customer/src/main/java/com/jzo2o/customer/model/dto/response/EvaluationRateResDTO.java
- Modify: jzo2o-customer/src/main/java/com/jzo2o/customer/service/EvaluationService.java
- Modify: jzo2o-customer/src/main/java/com/jzo2o/customer/service/impl/EvaluationServiceImpl.java
- Modify: consumer, worker, operation, and inner EvaluationController classes
- Test: jzo2o-customer/src/test/java/com/jzo2o/customer/service/UnifiedEvaluationServiceTest.java

**Interfaces:**
- Consumer: POST /consumer/evaluation, GET /consumer/evaluation/page, GET /consumer/evaluation/{id}.
- Worker: GET /worker/evaluation/page, GET /worker/evaluation/summary, GET /worker/evaluation/{id}.
- Operation: GET /operation/evaluation/page, GET /operation/evaluation/{id}, PUT /operation/evaluation/{id}/visibility.
- Inner score endpoint remains for dispatch compatibility but returns the native good-rate percentage, not a star score.

- [ ] **Step 1: Write failing service tests**

~~~java
@Test
void submitRejectsBlankContentAndDoesNotEvaluateOrder() {
    assertThrows(IllegalArgumentException.class,
        () -> service.submit(request(400L, GOOD, "   ", List.of())));
    verify(ordersApi, never()).evaluate(400L);
}

@Test
void workerRateUsesOnlyVisibleGoodAndBadRecords() {
    seed(GOOD, VISIBLE); seed(BAD, VISIBLE); seed(GOOD, HIDDEN);
    EvaluationRateResDTO rate = service.workerRate(88L);
    assertEquals("50.0%", rate.getDisplayRate());
    assertTrue(rate.getHasEvaluation());
}

@Test
void consumerCannotReadAnotherConsumersEvaluation() {
    assertThrows(ForbiddenOperationException.class, () -> service.consumerDetail(900L, 10L));
}
~~~

- [ ] **Step 2: Run the test and verify it fails against the legacy implementation**

Run: mvn -pl jzo2o-customer -Dtest=UnifiedEvaluationServiceTest test

Expected: FAIL because unified fields, local queries, and rate calculation do not exist.

- [ ] **Step 3: Implement transactional local submit**

Replace dual service/provider score inputs with ordersId, evaluationType, content, pictureArray, and isAnonymous. Validate type, content, images, current customer ownership, status 400, and absence of an ordersId record. Resolve the actual service provider through OrdersServeApi, persist one EvaluationRecord with snapshots, then call OrdersApi.evaluate. Do not catch and suppress a failed status transition.

- [ ] **Step 4: Implement role-restricted page/detail/visibility methods**

Consumer predicates include current consumer id and VISIBLE. Worker predicates include current provider id and VISIBLE. Operation may query HIDDEN. Filters are ordersId, consumerPhone, serveProviderName, evaluationType, visibleStatus, minCreateTime, and maxCreateTime. Visibility changes one status only and never delete rows.

~~~java
public EvaluationRateResDTO workerRate(Long workerId) {
    long good = recordMapper.selectCount(visibleType(workerId, GOOD));
    long bad = recordMapper.selectCount(visibleType(workerId, BAD));
    return EvaluationRateResDTO.of(good, bad);
}
~~~

EvaluationRateResDTO.of returns hasEvaluation=false when good plus bad equals zero; otherwise it uses HALF_UP rounding to one decimal place.

- [ ] **Step 5: Remove runtime legacy coupling**

Remove token/configuration endpoints and EvaluationHttpClient/EvaluationProperties usage from EvaluationServiceImpl. Replace queryServeProviderScoreByOrdersId with native good-rate data so dispatch compiles. A one-time importer is only enabled explicitly: map old scoreLevel >= 3 to GOOD and lower levels to BAD, skip duplicate ordersId, and never call it during normal requests.

- [ ] **Step 6: Run backend tests and commit**

Run: mvn -pl jzo2o-customer -Dtest=UnifiedEvaluationPersistenceTest,UnifiedEvaluationServiceTest test

Expected: PASS; no test starts or calls a legacy evaluation URL.

Commit: feat: replace legacy evaluation with local workflow

### Task 3: 重做小程序提交、我的评价列表和详情

**Files:**
- Modify: project-xzb-xcx-uniapp-java/subPages/order/components/evaluate.vue
- Modify: project-xzb-xcx-uniapp-java/pages/api/order.js
- Modify: project-xzb-xcx-uniapp-java/pages/commit/index.vue
- Modify: project-xzb-xcx-uniapp-java/pages/commit/index.scss
- Create: project-xzb-xcx-uniapp-java/pages/commit/detail.vue
- Modify: project-xzb-xcx-uniapp-java/pages.json
- Modify: project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js

**Interfaces:** consumes consumer submit/page/detail endpoints; produces binary submission plus 全部/好评/差评 list and read-only detail.

- [ ] **Step 1: Write failing mini-program contracts**

~~~js
test('evaluation form has one binary choice, required content, and optional images', () => {
  const source = componentText('../subPages/order/components/evaluate.vue');
  assert.match(source, /好评/);
  assert.match(source, /差评/);
  assert.doesNotMatch(source, /服务评价|师傅评价|uni-rate/);
  assert.doesNotMatch(source, /请上传评价图片/);
});
~~~

Also assert exactly three tabs, request parameter evaluationType, a detail route, and no delete-evaluation action.

- [ ] **Step 2: Run the contract and confirm RED**

Run: node --test tests/warm-theme-contract.test.js

Expected: FAIL because current page has two star-rating sections and requires images.

- [ ] **Step 3: Implement one evaluation form**

Render order/service summary, mutually-exclusive good/bad chips, 500-character textarea, and optional six-image uploader. Disable submit until a type and nonblank text exist. Reuse uploadFile behavior, but send only ordersId, evaluationType, content, pictureArray, isAnonymous. On failure retain selected type/text/files and show API error text.

- [ ] **Step 4: Implement customer history and detail**

Remove stars and the delete popup. Normalize the request wrapper response once. Page through /customer/consumer/evaluation/page with evaluationType. Cards show type badge, service, text excerpt, time, and thumbnails. Tap navigates to /pages/commit/detail?id=<id>; detail loads /customer/consumer/evaluation/{id} and previews images.

- [ ] **Step 5: Verify and commit**

Run: npm test

Expected: all mini-program tests pass.

Commit: feat: rebuild miniapp evaluations

### Task 4: 重做服务人员 App 的好评率和评价查看

**Files:**
- Modify: project-xzb-app-uniapp-java/utils/commonData.js
- Modify: project-xzb-app-uniapp-java/pages/api/order.js
- Modify: project-xzb-app-uniapp-java/pages/my/commponents/Evaluate.vue
- Modify: project-xzb-app-uniapp-java/pages/evaluate/index.vue
- Modify: project-xzb-app-uniapp-java/pages/evaluate/components/homeList.vue
- Create: project-xzb-app-uniapp-java/pages/evaluate/detail.vue
- Modify: project-xzb-app-uniapp-java/pages.json
- Modify: project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs

**Interfaces:** consumes worker page/detail/summary endpoints; produces profile good rate and binary list/detail.

- [ ] **Step 1: Write failing worker App contracts**

~~~js
test('worker profile uses native good-rate and removes composite score', () => {
  const source = componentText('../pages/my/commponents/Evaluate.vue');
  assert.match(source, /好评率/);
  assert.doesNotMatch(source, /综合评分|baseData\\.score/);
  assert.match(source, /getEvaluationSummary/);
});
~~~

Also require three tabs only, evaluationType instead of scoreLevel, no uni-rate, and a detail route.

- [ ] **Step 2: Run the contract and confirm RED**

Run: node --test tests/yuejia-worker-contract.test.mjs

Expected: FAIL because profile currently renders baseData.score and includes middle review.

- [ ] **Step 3: Implement good-rate profile card**

Add getEvaluationSummary for /customer/worker/evaluation/summary. Fetch in Evaluate.vue and render displayRate only when hasEvaluation is true; otherwise render 暂无评价. Remove the composite-score block; never calculate rate from locally loaded rows.

- [ ] **Step 4: Implement binary received-evaluation list and detail**

Replace evaluateData with all/good/bad, send evaluationType, render a type badge/text/images/order snapshot, and remove reply-input/emoji/star UI. Detail fetches /customer/worker/evaluation/{id}; images use uni.previewImage.

- [ ] **Step 5: Verify and commit**

Run: node --test tests/yuejia-worker-contract.test.mjs

Expected: PASS.

Commit: feat: show worker evaluation good rate

### Task 5: 恢复并重建管理端评价管理

**Files:**
- Modify: project-xzb-pc-admin-vue3-java/src/router/modules/components.ts
- Modify: project-xzb-pc-admin-vue3-java/src/layouts/simpleComponents/MenuContent.vue
- Delete: project-xzb-pc-admin-vue3-java/src/config/configuration.ts only if no other import remains
- Create: project-xzb-pc-admin-vue3-java/src/api/evaluation.ts
- Create: project-xzb-pc-admin-vue3-java/src/pages/evaluation/index.vue
- Create: project-xzb-pc-admin-vue3-java/src/pages/evaluation/components/SearchForm.vue
- Create: project-xzb-pc-admin-vue3-java/src/pages/evaluation/components/EvaluationTable.vue
- Create: project-xzb-pc-admin-vue3-java/src/pages/evaluation/components/EvaluationDetailDrawer.vue
- Modify: project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs
- Create: project-xzb-pc-admin-vue3-java/tests/evaluation-management-contract.test.mjs

**Interfaces:** consumes operation page/detail/visibility endpoints; produces in-app /evaluation/index management.

- [ ] **Step 1: Write failing admin contracts**

~~~js
test('evaluation management is an in-app route without legacy token navigation', () => {
  const router = fileText('../src/router/modules/components.ts');
  const menu = fileText('../src/layouts/simpleComponents/MenuContent.vue');
  assert.match(router, /path: '\\/evaluation'/);
  assert.match(router, /title: '评价管理'/);
  assert.doesNotMatch(menu, /handleToReply|evaluationBaseUrl|serviceToken/);
});
~~~

Add assertions for filters, drawer, and hide/restore action text; prohibit a delete action.

- [ ] **Step 2: Run contracts and confirm RED**

Run: node --test tests/order-list-contract.test.mjs tests/evaluation-management-contract.test.mjs

Expected: FAIL because the route is commented and MenuContent opens a legacy external URL.

- [ ] **Step 3: Restore native navigation**

Do not re-enable the old /reply pages. Add fresh /evaluation route to pages/evaluation/index.vue. Remove evaluationBaseUrl, evaluationBackUrl, evaluationUrlPrefix, serviceToken, handleToReply, and /reply special case from MenuContent.vue.

- [ ] **Step 4: Build management UI**

evaluation.ts exposes getEvaluationPage, getEvaluationDetail, updateEvaluationVisibility. Filter form sends only nonempty order number, customer phone, worker, type, visible status, and date range. Table renders order/service/customer/worker/type/time/status. Drawer shows full text, images, and immutable snapshots. Confirm before status change, then refresh the current page. Do not offer edit or delete.

- [ ] **Step 5: Verify and commit**

Run: node --test tests/order-list-contract.test.mjs tests/evaluation-management-contract.test.mjs

Run: npm run build

Expected: contracts pass and bundle has no legacy evaluation-system import.

Commit: feat: add native evaluation management

### Task 6: 集成验证和迁移交付

**Files:**
- Modify: docs/sql/2026-08-01-create-evaluation-record.sql
- Modify: docs/superpowers/specs/2026-08-01-unified-evaluation-design.md
- Test: tests from Tasks 1 through 5

- [ ] **Step 1: Document historical-data limits and importer**

Old evaluations are not stored locally in this repository; current service explicitly skips external persistence. If the old endpoint/database remains reachable, run an idempotent importer before retirement, map scoreLevel >= 3 to GOOD and all lower levels to BAD, and skip duplicate ordersId. If it is unavailable, state that older records cannot be reconstructed and new records start after migration.

- [ ] **Step 2: Run the verification suite**

Run: mvn -pl jzo2o-customer -Dtest=UnifiedEvaluationPersistenceTest,UnifiedEvaluationServiceTest test

Run: npm test from project-xzb-xcx-uniapp-java

Run: node --test tests/yuejia-worker-contract.test.mjs from project-xzb-app-uniapp-java

Run: node --test tests/order-list-contract.test.mjs tests/evaluation-management-contract.test.mjs and npm run build from project-xzb-pc-admin-vue3-java

Expected: all introduced tests pass; report unrelated pre-existing failures separately.

- [ ] **Step 3: Manually test each role**

1. Complete an order and submit good review with text only.
2. Confirm it appears in customer list/detail and worker list/detail; rate changes.
3. Submit bad review with images and confirm display.
4. Hide good review in admin; verify lists hide it and rate recalculates.
5. Restore it; verify visibility and rate return.
6. Confirm no client opens port 8083/evaluationadmin or requests an evaluation token.

- [ ] **Step 4: Commit delivery documentation**

Commit: docs: add unified evaluation deployment notes

