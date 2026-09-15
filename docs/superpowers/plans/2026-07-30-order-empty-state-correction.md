# 订单空状态与状态栏修正 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让两端订单页使用正确且不遮挡的空状态，并修复用户小程序订单状态栏的横向溢出。

**Architecture:** 服务人员端继续使用共享 `Empty` 组件，但为历史订单增加独立 `history` 变体，并由页面将业务上下文明确传入。小程序 `EmptyPage` 为订单列表增加 `order` 变体，用 CSS 图形取代旧图片；订单栏用左右固定定位保持在内容安全区内。

**Tech Stack:** Vue 3、uni-app、SCSS、Node.js 内置 `node:test` 契约测试。

## Global Constraints

- 仅修改 `.publish-jzo2o` 中的现有 GitHub 仓库和 `main` 分支。
- 保持悦家服务现有暖橙色变量，不添加图片或第三方依赖。
- 历史订单不得复用抢单大厅的图案或“保持在线”文案。
- 小程序订单空状态不得渲染 `static/zwnr2x.png`。
- 小程序固定状态栏左右各保留 24rpx，五个状态项等宽且不溢出。

---

### Task 1: 为服务人员端订单空状态建立清晰边界

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/components/empty/index.vue`
- Modify: `project-xzb-app-uniapp-java/pages/pickup/index.scss`
- Modify: `project-xzb-app-uniapp-java/pages/history/index.vue`
- Modify: `project-xzb-app-uniapp-java/pages/history/index.scss`
- Modify: `project-xzb-app-uniapp-java/components/uni-tab/index.vue`

**Interfaces:**
- Consumes: `Empty` props `variant`, `emptyLabel`, and worker page `v-else` branches.
- Produces: `variant="history"` for the history page; `worker-order` remains the pickup-page variant; shared tab container never exceeds its viewport.

- [ ] **Step 1: Write the failing worker contract test**

Append a test that verifies the history page selects its own variant, the shared empty component has a history message and history visual, pickup preserves an 88rpx top separation for its no-data list, the legacy history `.content` top override is absent, and the shared tab container uses `box-sizing: border-box`:

```js
test('worker order and history empty states keep independent meaning and clear the fixed tabs', () => {
  const pickupStyle = read('pages/pickup/index.scss');
  const history = read('pages/history/index.vue');
  const historyStyle = read('pages/history/index.scss');
  const empty = read('components/empty/index.vue');
  const tabs = read('components/uni-tab/index.vue');

  assert.match(history, /<Empty v-else variant="history"><\/Empty>/);
  assert.match(empty, /variant === 'history'/);
  assert.match(empty, /暂无历史订单，完成服务后会显示在这里/);
  assert.match(empty, /class="historyEmptyVisual"/);
  assert.match(pickupStyle, /\.noData\s*\{[\s\S]*margin-top:\s*88rpx/);
  assert.doesNotMatch(historyStyle, /::v-deep \.empty[\s\S]*\.content[\s\S]*top:\s*220rpx/);
  assert.match(tabs, /\.tabScroll\s*\{[\s\S]*box-sizing:\s*border-box/);
});
```

- [ ] **Step 2: Run the worker test to verify it fails**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: FAIL because `history` is not yet passed to `Empty`, and no `historyEmptyVisual` exists.

- [ ] **Step 3: Implement the smallest semantic and layout correction**

In `pages/history/index.vue`, replace the default empty component call with:

```vue
<Empty v-else variant="history"></Empty>
```

In `components/empty/index.vue`, add a `variant === 'history'` branch before the default worker radar. It must render `historyEmptyVisual` as a CSS-drawn archive/card icon and use this exact copy:

```vue
<view class="content">暂无历史订单，完成服务后会显示在这里</view>
```

Keep the `worker-order` branch on the existing radar and `暂无{{ emptyLabel }}订单` copy. Remove the legacy `::v-deep .empty` image/content position overrides from `pages/history/index.scss`. In `pages/pickup/index.scss`, replace the `.noData` margin reset with `margin-top: 88rpx` and a viewport-safe minimum height. In `components/uni-tab/index.vue`, add `box-sizing: border-box` to `.tabScroll`.

- [ ] **Step 4: Run the worker contract test to verify it passes**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: all worker contract tests PASS.

- [ ] **Step 5: Commit the worker correction**

```bash
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-app-uniapp-java/components/empty/index.vue project-xzb-app-uniapp-java/pages/pickup/index.scss project-xzb-app-uniapp-java/pages/history/index.vue project-xzb-app-uniapp-java/pages/history/index.scss project-xzb-app-uniapp-java/components/uni-tab/index.vue
git commit -m "fix: separate worker order empty states"
```

### Task 2: 替换小程序订单猴子图并固定五项状态栏

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/components/EmptyPage/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/subPages/order/components/list.vue`
- Modify: `project-xzb-xcx-uniapp-java/subPages/order/index.vue`

**Interfaces:**
- Consumes: `EmptyPage` prop `emptyInfo` from the customer order list.
- Produces: optional `variant="order"` that renders CSS `orderEmptyVisual`; fixed `.tabBox` positioned with `left` and `right` rather than width plus horizontal margin.

- [ ] **Step 1: Write the failing mini-program contract test**

Append a test that verifies the order list requests the order empty state, that state has its own visual, and the fixed status bar uses the exact safe insets:

```js
test('customer orders use a non-monkey empty visual and a five-item safe tab rail', () => {
  const empty = read('components/EmptyPage/index.vue');
  const list = read('subPages/order/components/list.vue');
  const order = read('subPages/order/index.vue');

  assert.match(list, /<EmptyPage[^>]*variant="order"/);
  assert.match(empty, /v-if="variant === 'order'"/);
  assert.match(empty, /class="orderEmptyVisual"/);
  assert.match(order, /left:\s*24rpx/);
  assert.match(order, /right:\s*24rpx/);
  assert.match(order, /width:\s*auto\s*!important/);
  assert.doesNotMatch(order, /width:\s*calc\(100% - 48rpx\)/);
});
```

- [ ] **Step 2: Run the mini-program test to verify it fails**

Run: `node --test tests/yuejia-ui-contract.test.mjs`

Expected: FAIL because the order list has no `variant="order"` and its status bar still uses `calc(100% - 48rpx)`.

- [ ] **Step 3: Implement the minimal order-specific visual and positioning**

Add `variant` to `EmptyPage` with default `default`. In its template, render an `orderEmptyVisual` branch when `variant === 'order'`; draw a warm document/archive card, small check badge, and soft halo using SCSS. Keep the old image only for the default variant so unrelated pages retain their current behavior.

In `subPages/order/components/list.vue`, pass `variant="order"` to its no-data `EmptyPage` invocation. In `subPages/order/index.vue`, replace the width-plus-horizontal-margin fixed-bar rules with:

```scss
left: 24rpx;
right: 24rpx;
width: auto !important;
margin: 16rpx 0 0;
box-sizing: border-box;
```

Retain the existing `flex: 1` and `min-width: 0` rules for every status item.

- [ ] **Step 4: Run the mini-program contract test to verify it passes**

Run: `node --test tests/yuejia-ui-contract.test.mjs`

Expected: all mini-program contract tests PASS.

- [ ] **Step 5: Commit the mini-program correction**

```bash
git add project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs project-xzb-xcx-uniapp-java/components/EmptyPage/index.vue project-xzb-xcx-uniapp-java/subPages/order/components/list.vue project-xzb-xcx-uniapp-java/subPages/order/index.vue
git commit -m "fix: correct customer order empty state"
```

### Task 3: 端到端静态验证

**Files:**
- Verify only: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Verify only: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:**
- Consumes: completed worker and mini-program corrections from Tasks 1 and 2.
- Produces: clean static checks and a repository without whitespace errors.

- [ ] **Step 1: Run both contract suites**

```bash
node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs
node --test project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs
```

Expected: both suites PASS with no failing tests.

- [ ] **Step 2: Check patch whitespace and review changed files**

```bash
git diff --check HEAD~2..HEAD
git status --short
```

Expected: `git diff --check` has no output; status is clean after the two task commits.

- [ ] **Step 3: Do not push automatically**

Leave the verified commits on `main` for the user to push, consistent with the repository workflow.
