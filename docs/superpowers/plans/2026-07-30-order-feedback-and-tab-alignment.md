# Order Feedback and Tab Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair order feedback and order-navigation layout in the worker App and customer mini-program.

**Architecture:** Keep current order APIs. Replace only legacy result-card visuals, use a distinct worker-order empty-state variant, and scope the customer tab alignment to its order page. The shared Navbar derives history placement from the WeChat capsule boundary.

**Tech Stack:** Vue 3, uni-app, SCSS, Node.js contract tests.

## Global Constraints

- Keep existing order endpoints, payloads, status values, and navigation unchanged.
- Use `--warm-*` tokens for worker UI additions.
- Do not push commits.

---

### Task 1: Worker rob-order feedback

**Files:**
- Modify: `project-xzb-app-uniapp-java/pages/index/components/homeList.vue:35-43`
- Modify: `project-xzb-app-uniapp-java/pages/index/index.scss:263-318`
- Test: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

**Interfaces:** Retain `isRob`, `msg`, `alertDialog`, and `handleClose`; display a semantic result mark, title, message, and confirmation action.

- [ ] **Step 1: Write the failing test**

```js
test('rob-order feedback uses a warm semantic mark, not legacy face assets', () => {
  const list = read('pages/index/components/homeList.vue');
  const style = read('pages/index/index.scss');
  assert.match(list, /class="resultMark"/);
  assert.doesNotMatch(list, /class="img"/);
  assert.doesNotMatch(style, /img_success@2x\.png|img_fail@2x\.png/);
});
```

- [ ] **Step 2: Verify RED**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

Expected: failure because the legacy image dialog is still present.

- [ ] **Step 3: Implement the minimum card**

```vue
<view class="resultMark" :class="isRob ? 'isSuccess' : 'isFailure'">
  <text>{{ isRob ? '✓' : '!' }}</text>
</view>
<view class="resultTitle">{{ isRob ? '接单成功' : '接单未成功' }}</view>
<view class="resultMessage">{{ isRob ? '已加入您的服务安排' : msg || '订单已被其他服务人员接取' }}</view>
```

Use `--warm-primary` and `--warm-primary-soft`; retain the existing `@click="handleClose"` confirmation behavior.

- [ ] **Step 4: Verify GREEN**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

Expected: zero failures.

- [ ] **Step 5: Commit**

Run: `git add project-xzb-app-uniapp-java/pages/index/components/homeList.vue project-xzb-app-uniapp-java/pages/index/index.scss project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs; git commit -m "fix: restyle worker rob-order feedback"`

### Task 2: Worker order empty state

**Files:**
- Modify: `project-xzb-app-uniapp-java/pages/pickup/index.vue:12-28`
- Modify: `project-xzb-app-uniapp-java/components/empty/index.vue:1-38`
- Test: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

**Interfaces:** Pickup passes `variant="worker-order"` and `emptyLabel`; Empty renders `暂无{{ emptyLabel }}订单`. Existing rob-order and evaluation variants stay unchanged.

- [ ] **Step 1: Write the failing test**

```js
test('worker order empty state describes the selected category', () => {
  const pickup = read('pages/pickup/index.vue');
  const empty = read('components/empty/index.vue');
  assert.match(pickup, /variant="worker-order"/);
  assert.match(pickup, /:emptyLabel="tabBars\[users\.tabIndex\]\.label"/);
  assert.match(empty, /variant === 'worker-order'/);
});
```

- [ ] **Step 2: Verify RED**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

Expected: failure because pickup uses the generic rob-order copy.

- [ ] **Step 3: Implement the minimum variant**

```vue
<Empty v-else variant="worker-order" :emptyLabel="tabBars[users.tabIndex].label" />
```

```vue
<template v-else-if="variant === 'worker-order'">
  <view class="workerEmptyRadar">
    <view class="radarRing ringOuter"></view>
    <view class="radarRing ringMiddle"></view>
    <view class="radarRing ringInner"></view>
    <view class="radarSignal"></view>
    <view class="toolbox"><text>✓</text></view>
  </view>
  <view class="content">暂无{{ emptyLabel }}订单</view>
</template>
```

Keep the existing pickup header and scroll-container geometry unchanged; only replace the empty-state meaning.

- [ ] **Step 4: Verify GREEN**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

Expected: zero failures.

- [ ] **Step 5: Commit**

Run: `git add project-xzb-app-uniapp-java/pages/pickup/index.vue project-xzb-app-uniapp-java/components/empty/index.vue project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs; git commit -m "fix: clarify worker order empty state"`

### Task 3: Customer order tabs and capsule-safe history control

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/subPages/order/index.vue:225-253`
- Modify: `project-xzb-xcx-uniapp-java/components/Navbar/index.vue:3-75`
- Modify: `project-xzb-xcx-uniapp-java/components/Navbar/index.scss:27-40`
- Test: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:** Order-page tab items use equal flex widths. Navbar uses `historyRight`, calculated with `res.screenWidth - menuButton.left + 8`.

- [ ] **Step 1: Write the failing test**

```js
test('customer order tabs are equal-width and history avoids the capsule', () => {
  const order = read('subPages/order/index.vue');
  const nav = read('components/Navbar/index.vue');
  assert.match(order, /\.orderPageWarm :deep\(\.itemTab \.tabItem\)[\s\S]*flex:\s*1/);
  assert.match(nav, /:style="\{ right: historyRight \+ 'px' \}"/);
  assert.match(nav, /historyRight\.value\s*=\s*res\.screenWidth - menuButton\.left \+ 8/);
});
```

- [ ] **Step 2: Verify RED**

Run: `node --test project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

Expected: failure because general tab margins and a hard-coded history offset remain.

- [ ] **Step 3: Implement the scoped layout**

```scss
.orderPageWarm :deep(.itemTab .tabItem) {
  flex: 1;
  min-width: 0;
  margin: 0;
}
```

```vue
<view class="history" :style="{ right: historyRight + 'px' }" ...>
```

```js
const menuButton = uni.getMenuButtonBoundingClientRect();
historyRight.value = res.screenWidth - menuButton.left + 8;
```

Remove only the fixed SCSS `right` value from the history rule.

- [ ] **Step 4: Verify GREEN**

Run: `node --test project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

Expected: zero failures.

- [ ] **Step 5: Commit**

Run: `git add project-xzb-xcx-uniapp-java/subPages/order/index.vue project-xzb-xcx-uniapp-java/components/Navbar/index.vue project-xzb-xcx-uniapp-java/components/Navbar/index.scss project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs; git commit -m "fix: align customer order navigation"`

### Task 4: Final verification

**Files:** all implementation files from Tasks 1-3.

- [ ] **Step 1: Run full checks**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs; node --test project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs; git diff --check; git status --short --branch`

Expected: both suites have zero failures, diff check has no output, and local commits are not pushed.
