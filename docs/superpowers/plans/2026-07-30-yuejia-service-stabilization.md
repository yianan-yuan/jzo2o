# 悦家服务双端稳定化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复小程序关键路径并恢复服务人员 App 首页的正常结构，同时统一为悦家服务品牌。

**Architecture:** 小程序以 URL 参数建立首页到服务分类页的确定性数据流；布局问题仅在所属页面或组件修复。服务人员 App 移除 `App.vue` 的宽泛选择器，保留原有组件边界。UI 回归由静态契约测试守护，再使用项目构建进行验证。

**Tech Stack:** Vue 3、UniApp、SCSS、Node.js 内置 `node:test`。

## Global Constraints

- 只修改 `D:\develop\code\jzo2o2.0\.publish-jzo2o` 中的 GitHub 管理副本。
- 用户品牌文案为“悦家服务”；服务人员端为“悦家服务·服务端”。
- 不修改业务接口、订单状态枚举、路由已有路径或原始 Gitee 副本。
- 禁止在服务人员 `App.vue` 使用 `.item`、`.navFrame`、`.box` 等泛化视觉覆盖。

---

### Task 1: 建立小程序导航与布局回归契约

**Files:**
- Create: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/package.json`

**Interfaces:**
- Consumes: 首页、服务分类页、我的页面和订单页的 Vue/SCSS 源码。
- Produces: `npm test` 可运行的静态页面契约检查。

- [ ] **Step 1: Write the failing test**

```js
assert.match(home, /url:\s*`\/pages\/service\/index\?serveTypeId=\$\{val\}`/);
assert.match(service, /requestedServeTypeId/);
assert.match(home, /mode="aspectFill"[\s\S]*class="cardImg"/);
assert.match(profile, /class="profileCopy"/);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: FAIL because the current code uses cached `activeId`, `scaleToFill`, and no `profileCopy` wrapper.

- [ ] **Step 3: Write minimal implementation**

Implement the production changes described in Tasks 2–4 without weakening the assertions.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: PASS.

### Task 2: 修复分类跳转与推荐图片

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/pages/service/index.vue`

**Interfaces:**
- Consumes: `serveTypeId` URL query value and existing `getServeCategory`/`getServeList` APIs.
- Produces: 分类页在接口完成后应用指定分类，海报使用保比例裁剪。

- [ ] **Step 1: Write the failing test**

Use Task 1 assertions for explicit query navigation, deferred selected-category application and `aspectFill`.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: FAIL on all three missing source contracts.

- [ ] **Step 3: Write minimal implementation**

Pass `serveTypeId` in `uni.reLaunch`; read it in `onLoad`; after category response choose that ID if present, otherwise first item; call `getServeListData` once. Replace the card image mode with `aspectFill` and retain a fixed image frame.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: PASS.

### Task 3: 修复个人页并改造订单页

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/my/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/my/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/subPages/order/index.vue`

**Interfaces:**
- Consumes: `handleOrder`、Vuex 的 `orderStatus`/`backLink` 和订单列表组件。
- Produces: 可换行的个人信息组，以及不改变业务流程的暖橙订单列表层级。

- [ ] **Step 1: Write the failing test**

Add assertions for `profileCopy`, absence of absolute-positioned `profileHint`, and order-page warm page class.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: FAIL because profile information currently has no vertical wrapper and the order page lacks the new visual class.

- [ ] **Step 3: Write minimal implementation**

Wrap nickname and helper copy in `.profileCopy`; lay it out normally in a vertical flex group. Add scoped order-page classes around existing elements; do not alter API calls, store mutations, or order actions.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: PASS.

### Task 4: 生成 AI 助手并完成品牌替换

**Files:**
- Create: `project-xzb-xcx-uniapp-java/static/ai/yuejia-assistant.png`
- Modify: `project-xzb-xcx-uniapp-java/components/aiBall/AiBall.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/ai-chat/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: both projects’ `pages.json`, and the worker `manifest.json`

**Interfaces:**
- Consumes: existing `/pages/ai-chat/index` route.
- Produces: local visual asset and unified displayed brand copy.

- [ ] **Step 1: Write the failing test**

Add assertions that the AI ball points to the local asset and opens `/pages/ai-chat/index`, and that both manifests/pages configuration contain the new brand.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: FAIL because the current assistant uses an emoji and old brand copy remains.

- [ ] **Step 3: Write minimal implementation**

Generate a single friendly cartoon assistant asset, copy it into `static/ai`, replace the emoji in the AI ball, preserve drag behavior, and replace all user-facing product names.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- yuejia-ui-contract.test.mjs`

Expected: PASS.

### Task 5: 恢复服务人员 App 首页并验证

**Files:**
- Create: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/App.vue`

**Interfaces:**
- Consumes: 首页 `uni-home-nav` 的 `.item` 和 `.navFrame`。
- Produces: 不污染组件选择器的基础全局样式。

- [ ] **Step 1: Write the failing test**

```js
assert.doesNotMatch(app, /\.navFrame,[\s\S]*min-height:\s*100vh/);
assert.doesNotMatch(app, /\.item,[\s\S]*\.boxBg/);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: FAIL because `App.vue` currently matches navigation and item elements globally.

- [ ] **Step 3: Write minimal implementation**

Delete the broad visual selector blocks in `App.vue`, retain only `page` background plus the existing imports, and leave page/component styles responsible for their own layout.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: PASS.

### Task 6: 全量验证与提交

**Files:**
- Verify: both project source trees and generated asset.

- [ ] **Step 1: Run static contracts**

Run: `npm test -- yuejia-ui-contract.test.mjs` and `node --test tests/yuejia-worker-contract.test.mjs`.

- [ ] **Step 2: Run HBuilderX-compatible dependency check**

Run: `npm install --package-lock=false --ignore-scripts` only if the relevant `node_modules` directory is absent; otherwise use existing dependencies without reinstalling.

- [ ] **Step 3: Inspect Git scope**

Run: `git status --short` and ensure only `.publish-jzo2o` changes are present.

- [ ] **Step 4: Commit and push**

Commit with `fix: stabilize yuejia service mobile clients`, then push `main` to GitHub.

