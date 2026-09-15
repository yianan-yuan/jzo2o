# Worker Banner and Search Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the worker App home banner with a warm service-workspace message and make customer mini-program search usable and visible.

**Architecture:** The worker banner becomes a local component template/CSS composition, keeping existing navigation actions and APIs untouched. The search page separates keyword input from request dispatch, refreshes city context immediately before querying, and gives the result scroll area an explicit viewport height.

**Tech Stack:** Vue 3 Composition API, UniApp, SCSS, Node built-in test runner, HBuilderX/Vite UniApp build.

## Global Constraints

- Modify only `D:/develop/code/jzo2o2.0/.publish-jzo2o` on user-authorized `main`.
- Do not change backend API endpoints, service result fields, navigation targets, or existing worker quick-setting handlers.
- Search sends requests only for non-empty keywords with a current city code.
- New behavior must have a failing source-contract test before production code.

---

### Task 1: Replace the worker home banner

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/components/uni-home-nav/index.vue`

**Consumes:** Existing worker header shortcut routes and warm style variables.

**Produces:** A local workbench banner with “今日专注服务” and “开启接单，附近订单将优先推送给你”.

- [ ] **Step 1: Write the failing contract test**

Add a test that reads `components/uni-home-nav/index.vue` and requires `class="workbenchBanner"`, both approved text strings, and no `img_baanner@2x.png` reference.

- [ ] **Step 2: Verify the test fails**

Run: `node --test tests/yuejia-worker-contract.test.mjs` from `project-xzb-app-uniapp-java`.

Expected: it fails because the component still uses the legacy banner image.

- [ ] **Step 3: Implement the local banner**

Replace `<view class="bg"></view>` with a `workbenchBanner` element containing an icon tile, an eyebrow “服务工作台”, the approved title and description, plus CSS-only decorative circles. Keep `baseSetting`, location handling, and every route callback unchanged.

- [ ] **Step 4: Verify and commit**

Run `node --test tests/yuejia-worker-contract.test.mjs`, then:

```powershell
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-app-uniapp-java/components/uni-home-nav/index.vue
git commit -m "feat: replace worker home banner"
```

### Task 2: Repair customer search flow and result viewport

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/pages/search/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/search/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/pages/search/components/List.vue`

**Consumes:** `getServeSearch({ cityCode, serveTypeId, keyword })` and city storage.

**Produces:** Confirm-driven search with city validation and a visible scrollable result region.

- [ ] **Step 1: Write the failing contract test**

Require a `refreshSearchCity` helper, a `keyword = searchData.value.keyword.trim()` guard in `handleSearch`, a `resultList` class in `List.vue`, and a `height: calc(100vh - 260rpx)` style rule in search SCSS. Assert that `handleInput` does not call `getNewData`.

- [ ] **Step 2: Verify the test fails**

Run: `node --test tests/yuejia-ui-contract.test.mjs` from `project-xzb-xcx-uniapp-java`.

Expected: it fails because requests currently fire while typing and results have no viewport height.

- [ ] **Step 3: Implement the repair**

Use `refreshSearchCity()` before a query; return false after showing “请先选择服务城市” and opening `/pages/city/index` when city code is absent. Make `handleInput` update only icon/history state. Make `handleSearch` trim input, clear results for empty values, validate city, set `isHistory`, then call `getNewData`. Use `Array.isArray(uni.getStorageSync('historyData'))` for initial history. Add `resultList` to the result `scroll-view` and style the outer search layout and result height without a `min-height: 100vh` conflict.

- [ ] **Step 4: Verify and commit**

Run `node --test tests/yuejia-ui-contract.test.mjs`, then:

```powershell
git add project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs project-xzb-xcx-uniapp-java/pages/search/index.vue project-xzb-xcx-uniapp-java/pages/search/index.scss project-xzb-xcx-uniapp-java/pages/search/components/List.vue
git commit -m "fix: restore customer service search"
```

### Task 3: Final verification

- [ ] **Step 1:** Run both Node contracts from their respective project directories.
- [ ] **Step 2:** Build the worker App with the configured HBuilderX `uni.js build -p h5` command.
- [ ] **Step 3:** Verify manually: worker home banner has no legacy housecleaning image; customer search box appears under navigation and displays a query result.

## Plan self-review

- Spec coverage: Task 1 covers the approved static workbench banner; Task 2 covers layout, query timing, city validation and history safety; Task 3 covers automated and build verification.
- Placeholder scan: no undefined API, route or component names are used.
- Type consistency: `refreshSearchCity` is defined and consumed within Task 2; all result handling remains array-based.
