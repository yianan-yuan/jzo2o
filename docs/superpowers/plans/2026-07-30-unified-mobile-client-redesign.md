# Unified Mobile Client Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the customer mini-program's established warm theme while fixing its navigation and AI experience, then apply the same visual language across the service-worker App business flows.

**Architecture:** The mini-program keeps its existing custom footer but expands its route configuration to four equal items and normalizes category identifiers at the service-page boundary. The worker App receives shared warm design tokens and page-local style layers; API calls, Vuex state, route query parameters, and order actions remain unchanged.

**Tech Stack:** Vue 3 Composition API, UniApp, SCSS, Node built-in test runner, HBuilderX/Vite UniApp build.

## Global Constraints

- Change files only under `D:/develop/code/jzo2o2.0/.publish-jzo2o`; do not touch the root Gitee copies.
- Work directly on the user-authorized `main` branch.
- Preserve existing API endpoints, payload fields, order status values, login rules, and route parameter names.
- The customer mini-program remains in its existing warm orange theme; the worker App adopts the same tokens.
- Do not add broad visual selectors such as `.item`, `.box`, or `.navFrame` to `project-xzb-app-uniapp-java/App.vue`.
- Each behavioral production change is preceded by a failing Node source-contract test.

---

## File map

| Area | Files | Responsibility |
| --- | --- | --- |
| Mini navigation and services | `project-xzb-xcx-uniapp-java/pages/index/index.vue`, `pages/service/index.vue`, `components/Foot/index.vue`, `pages/ai-chat/index.vue` | Correct service selection, full recommendation images, four-item navigation, AI interface. |
| Mini regression contract | `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs` | Assert the route, ID comparison, image mode, footer and chat structure. |
| Worker shared shell | `project-xzb-app-uniapp-java/styles/theme.scss`, `components/uni-nav/index.vue`, `components/uni-home-nav/index.vue`, `components/uni-footer/index.vue`, `components/uni-tab/index.vue` | Common tokens, headers, tab bar and filters. |
| Worker dispatch flows | `pages/index/**`, `pages/pickup/**`, `pages/history/**`, `pages/orderInfo/**`, `pages/serveRecord/**`, `pages/cancel/**` | Home, current/historical order lists, details and actions. |
| Worker profile/settings flows | `pages/my/**`, `pages/delivery/**`, `pages/setting/**`, `pages/serviceSkill/**`, `pages/getOrder/**`, `pages/serviceRange/**`, `pages/city/**`, `pages/evaluate/**`, `pages/account/**`, `pages/auth/**`, `pages/authFail/**` | Profile, messages, work configuration, forms and certification. |
| Worker regression contract | `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs` | Guard the no-global-override rule and require shared warm classes on each page family. |

## Task 1: Lock the mini-program navigation and selection contract

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/service/index.vue`

**Consumes:** Home category objects with `serveTypeId`; service category objects with `serveTypeId` from `getServeCategory`.

**Produces:** Query-driven service selection that compares normalized IDs and only falls back to the first category when no requested category exists.

- [ ] **Step 1: Write the failing source-contract tests**

Replace the old strict-equality assertion with assertions that `pages/service/index.vue` declares `const normalizeServeTypeId = (value) => String(value ?? '')`, derives `requestedServeTypeId` from that helper, and finds a category with `normalizeServeTypeId(item.serveTypeId) === requestedServeTypeId.value`. Assert that the home route still includes `?serveTypeId=${val}`.

- [ ] **Step 2: Run the mini regression contract and verify red**

Run: `node --test tests/yuejia-ui-contract.test.mjs` from `project-xzb-xcx-uniapp-java`.

Expected: the new normalization assertions fail because the current source calls `Number()` and compares raw values with `===`.

- [ ] **Step 3: Implement the minimal ID normalization**

In `pages/service/index.vue`, add:

```js
const normalizeServeTypeId = (value) => String(value ?? '');
```

Set `requestedServeTypeId.value = normalizeServeTypeId(options?.serveTypeId)`. Find `selectedType` with normalized values. Use the selected category's original `serveTypeId` for `activeId` and `getServeList`, so backend request types do not change. In `getServeListData`, derive the active category with the same normalization before reading `serveTypeImg`.

- [ ] **Step 4: Run the mini contract and verify green**

Run: `node --test tests/yuejia-ui-contract.test.mjs`.

Expected: all tests pass.

- [ ] **Step 5: Commit the navigation fix**

```powershell
git add project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs project-xzb-xcx-uniapp-java/pages/index/index.vue project-xzb-xcx-uniapp-java/pages/service/index.vue
git commit -m "fix: preserve selected service category"
```

## Task 2: Make recommendations complete and replace the floating AI entry

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/components/Foot/index.vue`

**Consumes:** Existing `/static/sy*.png`, `/static/fw*.png`, `/static/wd*.png`, and existing AI cartoon asset.

**Produces:** A four-item, 25% width custom footer and uncropped recommendation banners.

- [ ] **Step 1: Write failing source-contract tests**

Add assertions that the home page has no `<AiBall` or `import AiBall`, its recommendation image uses `mode="widthFix"`, and the footer defines exactly four entries including `{ pagePath: '/pages/ai-chat/index', text: 'AI助手' }`. Add an assertion for a `.tabbar-item { width: 25%; }` rule and a route to `/pages/ai-chat/index`.

- [ ] **Step 2: Run the mini contract and verify red**

Run: `node --test tests/yuejia-ui-contract.test.mjs`.

Expected: it fails because the source still uses `aspectFill`, mounts `AiBall`, and has three entries.

- [ ] **Step 3: Implement the minimal layout changes**

Remove `AiBall` from the home template and imports. Change the recommended `<image>` to `mode="widthFix"`; in `index.scss`, remove the fixed `height` from `.cardImg`, retain `width: 100%`, set `display: block`, and keep its card radius. Add a fourth footer item using the existing local cartoon image or a small CSS avatar, route it with `uni.redirectTo`, and give each `.tabbar-item` `width: 25%` / `flex: 0 0 25%`. Keep the existing login behavior for service navigation; AI remains available but the chat page enforces login when sending.

- [ ] **Step 4: Run the mini contract and verify green**

Run: `node --test tests/yuejia-ui-contract.test.mjs`.

Expected: all tests pass.

- [ ] **Step 5: Commit the footer and image update**

```powershell
git add project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs project-xzb-xcx-uniapp-java/pages/index/index.vue project-xzb-xcx-uniapp-java/pages/index/index.scss project-xzb-xcx-uniapp-java/components/Foot/index.vue
git commit -m "feat: add AI tab and preserve service banners"
```

## Task 3: Rebuild the AI conversation presentation in the existing theme

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/pages/ai-chat/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/components/Foot/index.vue`

**Consumes:** Existing `createAiSession`, `streamAiMessage`, `chat-message-state.js`, AI cartoon asset and NavBar.

**Produces:** A warm branded AI chat screen without changes to streaming/session interfaces.

- [ ] **Step 1: Write failing source-contract tests**

Add assertions that the chat has `class="chatWelcomeCard"`, references `/static/ai/yuejia-assistant.png`, renders a `quickQuestions` collection, and includes `<UniFooter :pagePath="'/pages/ai-chat/index'"`. Assert the source still imports and calls `streamAiMessage`.

- [ ] **Step 2: Run the mini contract and verify red**

Run: `node --test tests/yuejia-ui-contract.test.mjs`.

Expected: the welcome-card, quick-question and footer assertions fail.

- [ ] **Step 3: Implement the warm chat composition**

Define `const quickQuestions = ['想预约日常保洁', '空调维修怎么收费？', '帮我推荐合适服务'];`. Show a top welcome card before messages, render quick-question chips only when the chat has no user message, and call `askSuggestedQuestion(question)` from those chips. Replace the emoji AI avatar with the local cartoon image. Use warm CSS variables for the header background, message bubbles, recommendation cards, input surface, and primary button. Preserve `ensureSession`, login/city guards, callbacks, abort-on-unload and recommendation navigation exactly as they are. Add `UniFooter` with the AI active route and add message bottom padding equal to input bar plus footer height.

- [ ] **Step 4: Run the mini contract and verify green**

Run: `node --test tests/yuejia-ui-contract.test.mjs`.

Expected: all tests pass while the streaming API assertions remain present.

- [ ] **Step 5: Commit the chat redesign**

```powershell
git add project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs project-xzb-xcx-uniapp-java/pages/ai-chat/index.vue project-xzb-xcx-uniapp-java/components/Foot/index.vue
git commit -m "feat: redesign customer AI conversation"
```

## Task 4: Build the service-worker shared warm shell

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/styles/theme.scss`
- Modify: `project-xzb-app-uniapp-java/components/uni-nav/index.vue`
- Modify: `project-xzb-app-uniapp-java/components/uni-home-nav/index.vue`
- Modify: `project-xzb-app-uniapp-java/components/uni-footer/index.vue`
- Modify: `project-xzb-app-uniapp-java/components/uni-tab/index.vue`

**Consumes:** Current warm CSS variables and existing worker route paths.

**Produces:** Shared header, header shortcut, footer and tab styling without global `App.vue` overrides.

- [ ] **Step 1: Write failing worker contract tests**

Add tests requiring `--warm-page-bg`, `--warm-primary`, `--warm-surface`, and `--warm-shadow` in `styles/theme.scss`; require `var(--warm-primary)` in `uni-home-nav`, `uni-footer` and `uni-tab`; retain the assertions forbidding broad root selectors.

- [ ] **Step 2: Run the worker contract and verify red**

Run: `node --test tests/yuejia-worker-contract.test.mjs` from `project-xzb-app-uniapp-java`.

Expected: it fails on the component warm-token assertions.

- [ ] **Step 3: Implement shared-shell styling**

Keep all route definitions and event handlers. In the three shared components, replace fixed red/gray background, borders and active colors with the named warm tokens. Give the home header a short warm gradient, white shortcut chips, readable text, and a banner that no longer relies on global layout rules. Give the footer a white surface, top border, safe-area padding and warm active label/icon treatment. Use only component-scoped selectors.

- [ ] **Step 4: Run worker contract and verify green**

Run: `node --test tests/yuejia-worker-contract.test.mjs`.

Expected: all tests pass.

- [ ] **Step 5: Commit shared worker shell**

```powershell
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-app-uniapp-java/styles/theme.scss project-xzb-app-uniapp-java/components/uni-nav/index.vue project-xzb-app-uniapp-java/components/uni-home-nav/index.vue project-xzb-app-uniapp-java/components/uni-footer/index.vue project-xzb-app-uniapp-java/components/uni-tab/index.vue
git commit -m "feat: unify worker app shell styling"
```

## Task 5: Redesign worker dispatch, order and detail flows

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/pages/index/index.vue`, `pages/index/index.scss`, `pages/index/components/homeFilter.vue`, `pages/index/components/homeList.vue`
- Modify: `project-xzb-app-uniapp-java/pages/pickup/index.vue`, `pages/pickup/index.scss`, `pages/pickup/components/homeList.vue`
- Modify: `project-xzb-app-uniapp-java/pages/history/index.vue`, `pages/history/index.scss`, `pages/history/components/homeList.vue`
- Modify: `project-xzb-app-uniapp-java/pages/orderInfo/index.vue`, `pages/orderInfo/index.scss`, `pages/serveRecord/index.vue`, `pages/serveRecord/index.scss`, `pages/cancel/index.vue`

**Consumes:** Existing order API calls, `getRobOrder`, `getOrder`, existing status filter data and navigation handlers.

**Produces:** Warm dispatch workspace cards, order filters, status labels and details without data-flow changes.

- [ ] **Step 1: Write failing worker contract tests**

Require root markers `workerHomeWarm`, `workerOrderWarm`, and `workerDetailWarm` in the home, pickup/history and order-info files respectively. Require a scoped style reference to `var(--warm-surface)` in each page family. Assert the existing `getRobOrder`, `getOrder`, `getHistoryOrder`, and `getOrderInfo` imports remain.

- [ ] **Step 2: Run worker contract and verify red**

Run: `node --test tests/yuejia-worker-contract.test.mjs`.

Expected: root marker/style assertions fail.

- [ ] **Step 3: Implement the dispatch workspace family**

Add the root markers to each listed page. Update only templates/classes and local SCSS: use warm page backgrounds, rounded white order cards, soft status tags, grouped time/address/price blocks, warm primary action buttons, and enough bottom padding for the footer. Retain all `v-if`, `v-for`, API calls, order status expressions, emitted events, popup logic, `uni.navigateTo` and `uni.redirectTo` code. Do not change response-property names.

- [ ] **Step 4: Run worker contract and H5 build**

Run:

```powershell
node --test tests/yuejia-worker-contract.test.mjs
$env:HX_APP_ROOT = 'D:\develop\HBuilderX'
$env:UNI_INPUT_DIR = (Get-Location).Path
$env:NODE_PATH = 'D:\develop\HBuilderX\plugins\uniapp-cli-vite\node_modules'
& C:\Users\14602\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe D:\develop\HBuilderX\plugins\uniapp-cli-vite\node_modules\@dcloudio\vite-plugin-uni\bin\uni.js build -p h5 --outDir D:\develop\code\jzo2o2.0\.publish-jzo2o\project-xzb-app-uniapp-java\unpackage\dist\codex-h5-verify
```

Expected: contract passes and H5 build exits 0.

- [ ] **Step 5: Commit dispatch and order redesign**

```powershell
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-app-uniapp-java/pages/index project-xzb-app-uniapp-java/pages/pickup project-xzb-app-uniapp-java/pages/history project-xzb-app-uniapp-java/pages/orderInfo project-xzb-app-uniapp-java/pages/serveRecord project-xzb-app-uniapp-java/pages/cancel
git commit -m "feat: restyle worker dispatch and order flows"
```

## Task 6: Redesign worker profile, message and work-configuration flows

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/pages/my/index.vue`, `pages/my/index.scss`, `pages/my/commponents/HistoryScope.vue`
- Modify: `project-xzb-app-uniapp-java/pages/delivery/index.vue`, `pages/delivery/index.scss`
- Modify: `project-xzb-app-uniapp-java/pages/setting/index.vue`, `pages/setting/index.scss`, `pages/serviceSkill/index.vue`, `pages/serviceSkill/index.scss`, `pages/getOrder/index.vue`, `pages/getOrder/index.scss`, `pages/serviceRange/index.vue`, `pages/serviceRange/index.scss`, `pages/city/index.vue`, `pages/city/index.scss`
- Modify: `project-xzb-app-uniapp-java/pages/evaluate/index.vue`, `pages/evaluate/index.scss`, `pages/account/index.vue`, `pages/account/index.scss`, `pages/auth/index.vue`, `pages/auth/index.scss`, `pages/authFail/index.vue`, `pages/authFail/index.scss`

**Consumes:** Existing profile/user API calls, setting routes, certification form fields and status logic.

**Produces:** A consistent worker profile, message, configuration and certification visual system.

- [ ] **Step 1: Write failing worker contract tests**

Require `workerProfileWarm` in `pages/my/index.vue`, `workerSettingsWarm` in `pages/setting/index.vue`, `workerFormWarm` in each of account/auth/authFail, and `var(--warm-page-bg)` within the relevant local styles. Assert the existing `getUserInfo`, certification API imports and setting navigation route strings remain.

- [ ] **Step 2: Run worker contract and verify red**

Run: `node --test tests/yuejia-worker-contract.test.mjs`.

Expected: the new page-marker assertions fail.

- [ ] **Step 3: Implement profile and configuration styling**

Wrap each page with the specified marker class. Apply white rounded profile header/cards, grouped list rows, warm state chips, form labels, input surfaces, empty states, and clear primary/secondary actions through their local SCSS. Keep every `@click`, `v-model`, `switch`, emitted event, validation branch and navigation target unchanged. For message, city, skill and range lists, style existing data rows instead of replacing their data structures.

- [ ] **Step 4: Run worker contract and H5 build**

Run the same worker contract and H5 build commands from Task 5.

Expected: both exit successfully.

- [ ] **Step 5: Commit worker profile/settings redesign**

```powershell
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-app-uniapp-java/pages/my project-xzb-app-uniapp-java/pages/delivery project-xzb-app-uniapp-java/pages/setting project-xzb-app-uniapp-java/pages/serviceSkill project-xzb-app-uniapp-java/pages/getOrder project-xzb-app-uniapp-java/pages/serviceRange project-xzb-app-uniapp-java/pages/city project-xzb-app-uniapp-java/pages/evaluate project-xzb-app-uniapp-java/pages/account project-xzb-app-uniapp-java/pages/auth project-xzb-app-uniapp-java/pages/authFail
git commit -m "feat: restyle worker profile and settings flows"
```

## Task 7: Final regression and visual verification

**Files:**
- Modify only if a regression is found: files named in Tasks 1–6.
- Do not commit generated output under `unpackage/dist/codex-h5-verify`.

- [ ] **Step 1: Run both automated contracts**

Run:

```powershell
Set-Location project-xzb-xcx-uniapp-java; node --test tests/yuejia-ui-contract.test.mjs
Set-Location ..\project-xzb-app-uniapp-java; node --test tests/yuejia-worker-contract.test.mjs
```

Expected: both test suites pass.

- [ ] **Step 2: Build the worker App**

Run the H5 build command from Task 5.

Expected: exit code 0. Record non-fatal historical asset warnings separately if emitted.

- [ ] **Step 3: Manually inspect required navigation paths in HBuilderX**

Verify in the mini-program: home Daily Repair → All Services Daily Repair; each of the four footer items; complete recommendation banners; AI entry/chat. Verify in the worker App: home, current order, order detail, history order, profile, settings, skills/range, and account/auth. For every page confirm that footer/input/action areas do not obscure content.

- [ ] **Step 4: Commit only regression fixes if present**

```powershell
git status --short
git add <only the files changed to fix a verified regression>
git commit -m "fix: complete unified mobile visual regression"
```

## Plan self-review

- Spec coverage: Tasks 1–3 cover every customer requirement; Tasks 4–6 cover the worker shell and every registered business page family; Task 7 covers builds and manual flows.
- Placeholder scan: no task relies on unspecified routes, APIs, or a later undefined helper. Existing handlers and interface names are explicitly preserved.
- Type consistency: `normalizeServeTypeId` is defined and consumed in Task 1; page marker names are introduced in the same task that tests them; all source-contract commands use the existing test files.
