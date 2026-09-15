# 管理端订单筛选与空状态修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复电话筛选和空服务记录，并替换管理端空状态视觉与菜单。

**Architecture:** 订单筛选仅调整运营端订单查询条件；详情页只在有效服务时间存在时渲染服务记录；菜单显示由路由配置控制，空状态图由公用组件统一替换。

**Tech Stack:** Vue 3、TypeScript、TDesign、Spring Boot、MyBatis-Plus。

## Global Constraints

- 不删除评价管理、企业管理页面，仅从菜单路由中注释隐藏。
- 电话查询支持去首尾空格后的完整号码和局部号码。
- 空状态插画不得包含猴子、动物或文字。

---

### Task 1: 订单电话查询与服务记录防护

**Files:**
- Modify: `jzo2o-orders/jzo2o-orders-manager/src/main/java/com/jzo2o/orders/manager/service/impl/OrdersManagerServiceImpl.java`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/order/orderList/orderDetail.vue`
- Test: `project-xzb-pc-admin-vue3-java/tests/order-list-contract.test.mjs`

- [ ] **Step 1: 写失败断言**

断言后端使用 `StrUtil.trim` 和 `like`，服务记录依赖 `realServeStartTime`，且结束时间使用空值兜底。

- [ ] **Step 2: 运行断言并确认失败**

运行：`node --test tests/order-list-contract.test.mjs`

- [ ] **Step 3: 实现最小修复**

后端声明 `String contactsPhone = StrUtil.trim(...)`，使用 `like(StrUtil.isNotBlank(...), Orders::getContactsPhone, contactsPhone)`；详情页用开始时间作为卡片条件，并对结束时间使用 `-`。

- [ ] **Step 4: 运行断言并确认通过**

运行：`node --test tests/order-list-contract.test.mjs`

### Task 2: 菜单与空状态视觉

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/src/router/modules/components.ts`
- Modify: `project-xzb-pc-admin-vue3-java/src/components/noData/index.vue`
- Create: `project-xzb-pc-admin-vue3-java/src/assets/default/service-empty.png`

- [ ] **Step 1: 注释不需要的顶级路由**

将 `/reply` 与 `/institution` 顶级路由对象包裹为注释，保留所有页面代码。

- [ ] **Step 2: 接入新空状态插画**

将公用空状态图片源替换为 `service-empty.png`。

- [ ] **Step 3: 构建管理端**

运行：`npm run build -- --mode mock`

