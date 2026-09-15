# 管理端订单操作与筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成管理端订单列表的稳定加载、组合筛选、详情查看和运营端真实退款发起。

**Architecture:** 前端将所有列表请求收敛到一个由显式事件调用的 `fetchData`，并在发送请求前移除空条件。运营端控制器补齐详情和取消接口，取消接口只适配登录管理员身份后委托给既有 `IOrdersManagerService.cancel` 与策略/退款任务链路；详情接口组装现有详情页面所需的聚合模型。

**Tech Stack:** Vue 3、TypeScript、TDesign Vue Next、Axios、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Node test runner。

## Global Constraints

- 退款必须复用 `IOrdersManagerService.cancel(OrderCancelDTO)`，不得伪造退款成功状态。
- 只有既有运营端取消策略允许的订单状态可以发起退款；服务端是最终授权与状态校验点。
- 空筛选条件不得转换为 SQL 的等值条件。
- 保持现有 `OperationOrdersDetailResDTO` 与 `/order/orderList/orderDetail/:id` 前端详情页兼容。
- 每个任务先运行失败测试，再写最小实现，并在任务完成后提交。

---

### Task 1: 固定列表加载和筛选回归约束

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/index.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/SearchForm.vue`

**Interfaces:**
- Consumes: `getOrderList(query)`。
- Produces: `fetchData(query)` 仅由路由进入、搜索、重置、排序和分页调用；`normalizeOrderQuery(query)` 返回不含空字符串的请求参数。

- [ ] **Step 1: 写出失败的前端回归测试**

在 `order-list-contract.test.mjs` 添加断言，要求列表页不再导入/调用 `watchEffect`，定义 `normalizeOrderQuery`，并且搜索条件包含六个字段：

```js
assert.doesNotMatch(page, /watchEffect\s*\(/);
assert.match(page, /const normalizeOrderQuery\s*=\s*\(query\)/);
assert.match(page, /return Object\.fromEntries\(/);
assert.match(searchForm, /ordersStatus/);
assert.match(searchForm, /payStatus/);
assert.match(searchForm, /refundStatus/);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: FAIL，提示缺少 `normalizeOrderQuery` 或仍存在 `watchEffect`。

- [ ] **Step 3: 实现单一加载入口和参数清理**

在列表页用路由路径 `watch(..., { immediate: true })` 代替 `onMounted` 和 `watchEffect` 的双重加载；只在 `route.path === '/order/orderList'` 时调用一次 `fetchData`。新增：

```ts
const normalizeOrderQuery = (query) =>
  Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null)
  )

const fetchData = async (query) => {
  dataLoading.value = true
  try {
    const res = await getOrderList(normalizeOrderQuery(query))
    if (res.code !== 200) throw new Error(res.msg || res.message || '订单列表加载失败')
    listData.value = res.data.list || []
    pagination.value.total = Number(res.data.total || 0)
  } catch (error) {
    listData.value = []
    pagination.value.total = 0
    MessagePlugin.error(error.message || '订单列表加载失败')
  } finally {
    dataLoading.value = false
  }
}
```

在搜索与重置时克隆完整的默认查询对象、同步设置 `pageNo = 1`，并由搜索表单的重置事件调用父级重置处理。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: PASS，所有断言通过。

- [ ] **Step 5: 提交列表加载与筛选修复**

```bash
git add project-xzb-pc-admin-vue3-java/src/pages/order/orderList/index.vue project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/SearchForm.vue project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs
git commit -m "fix: stabilize admin order filters"
```

### Task 2: 补齐运营端退款与详情 API

**Files:**
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/test/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersControllerTest.java`
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersController.java`
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/IOrdersManagerService.java`
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java`

**Interfaces:**
- Consumes: `OrderCancelReqDTO { id, cancelReason }`、`UserContext.currentUser()`、`Orders`、`OrdersServe`、订单快照。
- Produces: `GET /operation/orders/aggregation/{id}` 返回 `OperationOrdersDetailResDTO`；`PUT /operation/orders/cancel` 调用 `IOrdersManagerService.cancel(OrderCancelDTO)`。

- [ ] **Step 1: 写出失败的控制器测试**

扩展 `OperationOrdersControllerTest`，模拟当前管理员和订单服务，验证详情代理与取消 DTO 的关键字段：

```java
@Test
void cancelUsesTheLoggedInAdministratorAndDelegatesToTheExistingCancelService() {
    OrderCancelReqDTO request = new OrderCancelReqDTO();
    request.setId(1L);
    request.setCancelReason("用户申请退款");

    controller.cancel(request);

    ArgumentCaptor<OrderCancelDTO> captor = ArgumentCaptor.forClass(OrderCancelDTO.class);
    verify(ordersManagerService).cancel(captor.capture());
    assertEquals(1L, captor.getValue().getId());
    assertEquals("用户申请退款", captor.getValue().getCancelReason());
}
```

同时添加静态契约断言，要求控制器声明 `@GetMapping("/aggregation/{id}")`、`@PutMapping("/cancel")`。

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`

Expected: FAIL，提示详情/取消路由缺失。若本机 Maven 已可用，再运行：

```bash
mvn -pl jzo2o-orders/jzo2o-orders-manager -am -Dtest=OperationOrdersControllerTest test
```

Expected: FAIL，提示控制器方法缺失。

- [ ] **Step 3: 实现运营端详情与取消入口**

在 `OperationOrdersController` 按消费者控制器的已有模式实现：

```java
@GetMapping("/aggregation/{id}")
public OperationOrdersDetailResDTO aggregation(@PathVariable Long id) {
    return ordersManagerService.operationDetail(id);
}

@PutMapping("/cancel")
public void cancel(@RequestBody @Validated OrderCancelReqDTO request) {
    OrderCancelDTO cancel = BeanUtil.toBean(request, OrderCancelDTO.class);
    CurrentUserInfo current = UserContext.currentUser();
    cancel.setCurrentUserId(current.getId());
    cancel.setCurrentUserName(current.getName());
    cancel.setCurrentUserType(current.getUserType());
    ordersManagerService.cancel(cancel);
}
```

在服务接口中声明 `OperationOrdersDetailResDTO operationDetail(Long id)`。实现从 `orders` 读取订单主数据、从订单状态机快照取得状态流转与支付/退款流水字段、按订单 ID 查询 `OrdersServe` 以补充服务图文记录；无服务单时 `serveInfo` 为 `null`。用 `OrdersCanceled` 补充取消人、时间和原因。订单不存在抛出既有 `CommonException("订单不存在")`。不重写 `cancel` 或退款策略，只调用既有策略链路。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`

Expected: PASS。若 Maven 可用：

```bash
mvn -pl jzo2o-orders/jzo2o-orders-manager -am -Dtest=OperationOrdersControllerTest test
```

Expected: PASS。

- [ ] **Step 5: 提交运营端订单接口**

```bash
git add jzo2o-orders/jzo2o-orders-manager/src/test/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersControllerTest.java jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/controller/operation/OperationOrdersController.java jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/IOrdersManagerService.java jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs
git commit -m "feat: add admin order detail and refund endpoints"
```

### Task 3: 完成订单详情跳转与退款交互

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/TableList.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/index.vue`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/DialogForm.vue`
- Modify: `project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`

**Interfaces:**
- Consumes: `refundOrder({ id, cancelReason })`、`GET /operation/orders/aggregation/{id}` 和列表行的 `ordersStatus`、`payStatus`、`refundStatus`。
- Produces: 详情跳转；合法行可退款；退款原因为必填；确认后刷新当前查询页。

- [ ] **Step 1: 写出失败的界面契约测试**

在静态测试中要求操作列有明确退款资格函数、详情跳转与成功刷新：

```js
assert.match(table, /const canRefund\s*=\s*\(row\)/);
assert.match(table, /router\.push\('\/order\/orderList\/orderDetail\/'.*val\.id/);
assert.match(page, /await refundOrder\(/);
assert.match(page, /fetchData\(requestData\.value\)/);
assert.match(dialog, /required:\s*true/);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: FAIL，提示退款资格函数或完成回刷逻辑缺失。

- [ ] **Step 3: 实现安全的退款和查看交互**

在 `TableList.vue` 新增：

```ts
const canRefund = (row) =>
  row.payStatus === 4 &&
  (row.refundStatus === null || row.refundStatus === undefined) &&
  [100, 200, 300, 500].includes(row.ordersStatus)
```

对不符合条件的退款按钮使用禁用样式和 `aria-disabled`，并在点击保护中直接返回。保留 `handleDetail` 到现有详情路径。

在父页面提交退款前使用 TDesign 确认对话框显示订单号和原因；确认后调用 `refundOrder({ id: refundId.value, cancelReason: form.description })`。以统一响应结构 `res.code` 判定成功；成功后关闭退款原因窗口、显示“已发起退款，请等待支付渠道处理”并刷新当前查询；失败时显示 `res.msg || res.message`，不清除输入内容。

修正 `DialogForm.vue` 的表单状态：每次打开时初始化空 `description`，只在成功提交后关闭，取消时清空。保留现有必填与文本长度规则。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: PASS。

- [ ] **Step 5: 提交管理端操作交互**

```bash
git add project-xzb-pc-admin-vue3-java/src/pages/order/orderList/index.vue project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/TableList.vue project-xzb-pc-admin-vue3-java/src/pages/order/orderList/components/DialogForm.vue project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs
git commit -m "feat: complete admin order actions"
```

### Task 4: 全量验证与运行交接

**Files:**
- Verify only: `project-xzb-pc-admin-vue3-java`
- Verify only: `jzo2o-orders/jzo2o-orders-manager`

**Interfaces:**
- Consumes: 前三项完成的前端、订单服务和真实支付退款任务。
- Produces: 构建证据、接口验证结果及管理员手动验证清单。

- [ ] **Step 1: 运行前端回归和构建**

```bash
node --test tests/order-list-contract.test.mjs
npm run build -- --mode mock
```

Expected: Node 测试零失败，`vue-tsc` 与 Vite 构建退出码为 0。

- [ ] **Step 2: 验证订单服务**

```bash
mvn -pl jzo2o-orders/jzo2o-orders-manager -am -Dtest=OperationOrdersControllerTest test
```

Expected: `OperationOrdersControllerTest` 通过。若当前终端未安装 Maven，在 IDEA Maven 面板运行该测试并记录结果。

- [ ] **Step 3: 手动验收**

1. 重启 `jzo2o-orders-manager`，刷新管理端订单列表，不应出现误报。
2. 分别验证订单号、客户电话、日期、订单状态、支付状态、退款状态筛选，以及多条件组合；点击重置后应回到全部订单第一页。
3. 点击查看，应完整渲染订单详情；对无服务记录的订单不应报错。
4. 对已支付、未退款且策略允许的测试订单填写原因并确认，列表应刷新为退款处理中；再次点击退款应被禁止。
5. 对未支付、已退款或不允许状态的订单验证退款按钮不可用。

- [ ] **Step 4: 提交验证后的必要修正**

若上述验证仅发现与本计划直接相关的错误，先补充失败测试、完成最小修复，再使用：

```bash
git add <changed-files>
git commit -m "fix: verify admin order operations"
```
