# 管理端服务记录详情修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理端订单详情能读取服务人员已提交的服务记录。

**Architecture:** 服务单与订单共用 ID；订单聚合详情使用该 ID 读取 `OrdersServe`，再映射到 `OperationOrdersDetailResDTO.ServeInfo`。不变更 API、前端请求或数据库结构。

**Tech Stack:** Java、Spring Boot、MyBatis-Plus、Node.js 静态契约测试。

## Global Constraints

- 仅修改订单管理详情的服务单读取条件。
- 保持 `ServeInfo` 的字段映射和现有接口路径不变。
- 不创建分支、不推送远程仓库。

---

### Task 1: 修复管理端详情的服务单关联

**Files:**
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java:132-134`
- Test: `project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`

**Interfaces:**
- Consumes: `operationDetail(Long id)` 的订单 ID。
- Produces: 填充 `OperationOrdersDetailResDTO.ServeInfo` 的 `OrdersServe` 实体。

- [x] **Step 1: 写入失败的回归测试**

在 `order-list-contract.test.mjs` 中新增断言，要求 `operationDetail` 使用：

```js
assert.match(implementation, /OrdersServe ordersServe = ordersServeManagerService\.queryById\(id\)/);
assert.doesNotMatch(implementation, /OrdersServe::getOrdersId, id/);
```

- [x] **Step 2: 运行测试确认失败**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: 新断言失败，因为当前实现使用 `OrdersServe::getOrdersId`。

- [x] **Step 3: 写入最小实现**

将聚合详情中的查询替换为：

```java
OrdersServe ordersServe = ordersServeManagerService.queryById(id);
```

保留后续 `ServeInfo` 的图片、说明和时间字段映射。

- [x] **Step 4: 运行测试确认通过**

Run: `node --test tests/order-list-contract.test.mjs`

Expected: 所有子测试通过。

- [x] **Step 5: 构建管理端并提交**

Run: `npm run build -- --mode mock`

Expected: `vue-tsc --noEmit` 与 `vite build` 均以退出码 0 完成。

Commit:

```bash
git add jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs docs/superpowers/specs/2026-07-30-admin-service-record-detail-design.md docs/superpowers/plans/2026-07-30-admin-service-record-detail.md
git commit -m "fix: load service records in admin detail"
```
