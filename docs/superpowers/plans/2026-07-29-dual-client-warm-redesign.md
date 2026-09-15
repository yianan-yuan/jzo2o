# Dual Client Warm Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the service worker App to the GitHub monorepo and restyle every business page in both mobile clients with the approved warm-orange visual system while preserving all business behaviour.

**Architecture:** Keep each client as an independent UniApp project under the existing GitHub monorepo. Each client exposes its palette and layout primitives from `styles/theme.scss`; existing pages consume those tokens through their current scoped SCSS files and shared navigation/list/footer components. The worker App is copied into the monorepo as source only, preserving its separate Gitee repository on disk.

**Tech Stack:** Vue 3 Composition API, UniApp, SCSS, Node built-in test runner, Vite/HBuilderX build tooling, Git.

## Global Constraints

- Use `D:\develop\code\jzo2o2.0\.publish-jzo2o` on branch `main`, as explicitly requested by the user.
- Do not modify the Gitee worktree at `D:\develop\code\jzo2o2.0\project-xzb-app-uniapp-java` or its `.git` directory.
- Do not change API modules, route paths, Vuex state names, request payloads, payment flow, or order-status logic.
- Keep `node_modules/`, `unpackage/`, IDE files, local private configuration, environment files, and caches out of GitHub.
- Use design tokens exactly as specified in `docs/superpowers/specs/2026-07-29-dual-client-warm-redesign-design.md`.
- Every source change follows red-green-refactor: add or update the relevant contract test, run it to observe failure, make the smallest change, then run it again.

---

## File Structure

| Path | Responsibility |
|---|---|
| `project-xzb-xcx-uniapp-java/styles/theme.scss` | Customer mini-program palette, spacing, radius, shadow and status tokens |
| `project-xzb-xcx-uniapp-java/components/` | Customer navigation, footer, search, tab, empty and action primitives |
| `project-xzb-xcx-uniapp-java/pages/` and `subPages/` | Customer-facing screens, restyled without changing script behaviour |
| `project-xzb-app-uniapp-java/styles/theme.scss` | Worker App palette and high-contrast operational variants |
| `project-xzb-app-uniapp-java/components/` | Worker navigation, footer, tab, list, empty and order primitives |
| `project-xzb-app-uniapp-java/pages/` | Worker workflow screens, restyled without changing script behaviour |
| `*/tests/theme-contract.test.*` | Source-level tests asserting each client contains the approved token set and page coverage markers |

### Task 1: Import the service worker App into the GitHub monorepo

**Files:**
- Create: `project-xzb-app-uniapp-java/` from `D:\develop\code\jzo2o2.0\project-xzb-app-uniapp-java`
- Modify: `.gitignore`
- Create: `project-xzb-app-uniapp-java/.gitignore`

**Interfaces:**
- Consumes: the existing worker App source directory and the root GitHub ignore rules.
- Produces: a regular source directory tracked by the GitHub monorepo, with no nested `.git` directory or generated/private files.

- [ ] **Step 1: Write the failing repository-boundary check**

Create `project-xzb-app-uniapp-java/tests/repository-boundary.test.mjs` with:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

test('worker App is source-only and excludes local artifacts', () => {
  const ignore = readFileSync('../.gitignore', 'utf8');
  assert.equal(existsSync('package.json'), true);
  assert.match(ignore, /node_modules/);
  assert.match(ignore, /unpackage/);
  assert.match(ignore, /project\.private\.config\.json/);
  assert.equal(existsSync('.git'), false);
});
```

- [ ] **Step 2: Run the boundary check and verify it fails**

Run from `project-xzb-app-uniapp-java` after creating only the test:

```powershell
node --test tests/repository-boundary.test.mjs
```

Expected: the test fails because the tracked source directory or its ignore file is absent.

- [ ] **Step 3: Copy the source snapshot without nested Git or generated files**

Use PowerShell from the monorepo root:

```powershell
robocopy 'D:\develop\code\jzo2o2.0\project-xzb-app-uniapp-java' 'D:\develop\code\jzo2o2.0\.publish-jzo2o\project-xzb-app-uniapp-java' /E /XD .git node_modules unpackage .idea .hbuilderx .vite /XF project.private.config.json *.log
if ($LASTEXITCODE -gt 7) { exit $LASTEXITCODE }
```

Create `project-xzb-app-uniapp-java/.gitignore` containing the root exclusions for `node_modules/`, `unpackage/`, `.idea/`, `.hbuilderx/`, `.vite/`, `project.private.config.json`, `.env*`, and `*.log`. Do not copy the original nested `.git` directory.

- [ ] **Step 4: Run the boundary check and Git ignore inspection**

```powershell
Set-Location project-xzb-app-uniapp-java
node --test tests/repository-boundary.test.mjs
git -C .. check-ignore -v project-xzb-app-uniapp-java/node_modules/example.js
git -C .. check-ignore -v project-xzb-app-uniapp-java/unpackage/dist/app-plus/app.js
```

Expected: test passes; both artifact paths are reported as ignored.

- [ ] **Step 5: Commit the source import**

```powershell
git add .gitignore project-xzb-app-uniapp-java
git commit -m "feat: manage worker app in monorepo"
```

### Task 2: Establish token contracts for both clients

**Files:**
- Create: `project-xzb-xcx-uniapp-java/tests/theme-contract.test.js`
- Modify: `project-xzb-xcx-uniapp-java/styles/theme.scss`
- Modify: `project-xzb-xcx-uniapp-java/App.vue`
- Create: `project-xzb-app-uniapp-java/tests/theme-contract.test.mjs`
- Modify: `project-xzb-app-uniapp-java/package.json`
- Modify: `project-xzb-app-uniapp-java/styles/theme.scss`
- Modify: `project-xzb-app-uniapp-java/App.vue`

**Interfaces:**
- Consumes: current SCSS variables and global page rules.
- Produces: `--warm-page-bg`, `--warm-surface`, `--warm-primary`, `--warm-primary-soft`, `--warm-text`, `--warm-text-muted`, `--warm-line`, `--state-success`, `--state-warning`, and `--state-danger` in both client themes.

- [ ] **Step 1: Write failing theme-contract tests**

Use this test body in each client, changing only the file extension/import syntax appropriate to its package type:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const theme = readFileSync(new URL('../styles/theme.scss', import.meta.url), 'utf8');
const app = readFileSync(new URL('../App.vue', import.meta.url), 'utf8');

test('warm visual tokens are available globally', () => {
  for (const token of ['--warm-page-bg', '--warm-surface', '--warm-primary', '--warm-primary-soft', '--warm-text', '--warm-text-muted', '--warm-line', '--state-success', '--state-warning', '--state-danger']) {
    assert.match(theme, new RegExp(token));
  }
  assert.match(app, /var\(--warm-page-bg\)/);
});
```

- [ ] **Step 2: Run the tests and verify they fail**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
Set-Location ..\project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
```

Expected: each test fails because the warm token names do not exist.

- [ ] **Step 3: Add the minimum shared visual foundation**

Define the ten produced custom properties in each `styles/theme.scss` using the approved palette. Add radius, spacing and shadow properties named `--warm-radius-sm`, `--warm-radius-md`, `--warm-radius-lg`, `--warm-space`, and `--warm-shadow`. In each `App.vue`, set the root `page` background to `var(--warm-page-bg)` and retain existing UniApp SCSS imports and lifecycle code unchanged.

For the worker App, use the same warm palette but preserve high-contrast `--state-*` colors for order labels and operational controls.

- [ ] **Step 4: Run both contracts and formatting checks**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
Set-Location ..\project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
git -C .. diff --check
```

Expected: all token tests pass and `git diff --check` prints no whitespace errors.

- [ ] **Step 5: Commit the visual foundation**

```powershell
git add project-xzb-xcx-uniapp-java/styles/theme.scss project-xzb-xcx-uniapp-java/App.vue project-xzb-xcx-uniapp-java/tests/theme-contract.test.js project-xzb-app-uniapp-java/styles/theme.scss project-xzb-app-uniapp-java/App.vue project-xzb-app-uniapp-java/package.json project-xzb-app-uniapp-java/tests/theme-contract.test.mjs
git commit -m "feat: add shared warm visual tokens"
```

### Task 3: Restyle shared navigation, controls and feedback components

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/components/Navbar/index.vue`, `components/Foot/index.vue`, `components/uni-nav/index.vue`, `components/uni-tab/index.vue`, `components/uni-search/index.vue`, `components/BtnFooter/index.vue`, `components/EmptyPage/index.vue`, `components/NetFail/index.vue`
- Modify: `project-xzb-app-uniapp-java/components/uni-nav/index.vue`, `components/uni-footer/index.vue`, `components/uni-tab/index.vue`, `components/uni-search/index.vue`, `components/uni-home-nav/index.vue`, `components/uni-empty-page/index.vue`, `components/empty/index.vue`, `components/uni-expressage-foot/index.vue`
- Modify: both `tests/theme-contract.test.*`

**Interfaces:**
- Consumes: warm tokens from Task 2 and existing component props/events.
- Produces: unchanged prop/event APIs with warm navigation, selected states, cards, search fields, tabs, empty states and fixed bottom actions.

- [ ] **Step 1: Extend each test with a failing component token assertion**

Add this helper and test to each contract file, adapting the component path list for its client:

```js
const componentText = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');

test('shared controls consume the warm token layer', () => {
  for (const path of ['../components/uni-nav/index.vue', '../components/uni-tab/index.vue']) {
    assert.match(componentText(path), /var\(--warm-primary\)/);
  }
});
```

- [ ] **Step 2: Run tests and verify component assertions fail**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
Set-Location ..\project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
```

Expected: assertions fail because the old hard-coded brand colors remain.

- [ ] **Step 3: Implement component-only style changes**

Replace hard-coded page chrome colors with `--warm-*` variables. Use 32rpx cards, 24rpx controls, shallow `--warm-shadow`, visible selected tab indicators and `--state-*` labels. Keep component names, properties, emits, click handlers and navigation URLs unchanged. Ensure fixed footers add safe bottom padding so list content remains reachable.

- [ ] **Step 4: Re-run contracts and inspect both component trees**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
Set-Location ..\project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
git -C .. diff --check
```

Expected: all tests pass and the diff contains style/template changes only in shared components.

- [ ] **Step 5: Commit shared component work**

```powershell
git add project-xzb-xcx-uniapp-java/components project-xzb-xcx-uniapp-java/tests/theme-contract.test.js project-xzb-app-uniapp-java/components project-xzb-app-uniapp-java/tests/theme-contract.test.mjs
git commit -m "feat: restyle shared mobile components"
```

### Task 4: Restyle customer discovery and account pages

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`, `pages/index/index.scss`, `pages/service/index.vue`, `pages/service/components/airMaintenance.vue`, `pages/search/index.vue`, `pages/search/index.scss`, `pages/search/components/List.vue`, `pages/city/index.vue`, `pages/city/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/pages/login/index.vue`, `pages/login/index.scss`, `pages/ai-chat/index.vue`, `pages/message/index.vue`, `pages/my/index.vue`, `pages/my/index.scss`, `pages/my/components/FastMenu.vue`, `pages/my/components/MyMenu.vue`, `pages/my/components/list.vue`
- Modify: `project-xzb-xcx-uniapp-java/tests/theme-contract.test.js`

**Interfaces:**
- Consumes: existing data refs, API calls, navigation methods and components.
- Produces: customer discovery/account pages with warm hero blocks, card groups, readable service grids and consistent profile menus.

- [ ] **Step 1: Add failing coverage assertions for all discovery/account entries**

Add a `customerPages` array containing the exact page paths above and test each source contains `var(--warm-`:

```js
test('customer discovery and account pages use the warm layer', () => {
  for (const path of customerPages) {
    assert.match(componentText(`../${path}`), /var\(--warm-/);
  }
});
```

- [ ] **Step 2: Run the customer contract and verify it fails**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
```

Expected: each unmodified page is reported as missing a warm token.

- [ ] **Step 3: Apply the discovery/account layout system**

Restyle the homepage hero, city/search bar, category grid, recommendation cards and service categories. Restyle service detail, search results, city selector, login, AI chat, messages and profile/menu groups. Reuse existing images, loops, events and API data; add only wrappers or classes required for spacing and hierarchy.

- [ ] **Step 4: Verify the contract and run a mini-program build**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
npx vite build
```

Expected: tests pass and the build exits with code 0.

- [ ] **Step 5: Commit customer discovery/account work**

```powershell
git add project-xzb-xcx-uniapp-java/pages project-xzb-xcx-uniapp-java/tests/theme-contract.test.js
git commit -m "feat: restyle customer discovery pages"
```

### Task 5: Restyle customer transaction and after-sales pages

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/address/index.vue`, `pages/address/index.scss`, `pages/coupon/index.vue`, `pages/coupon/index.scss`, `pages/coupon/coupon.vue`, `pages/coupon/components/list.vue`, `pages/commit/index.vue`, `pages/commit/index.scss`, `pages/pay/index.vue`, `pages/pay/index.scss`, `pages/pay/components/paySuccessful.vue`
- Modify: `project-xzb-xcx-uniapp-java/subPages/address-info/index.vue`, `subPages/address-info/index.scss`, `subPages/address-info/components/selectArea.vue`, `subPages/address-info/components/selectArea.scss`
- Modify: `project-xzb-xcx-uniapp-java/subPages/order/index.vue`, `subPages/order/index.scss`, `subPages/order/details.vue`, `subPages/order/cancel.vue`, `subPages/order/cancelRule.vue`, `subPages/order/components/list.vue`, `subPages/order/components/evaluate.vue`, `subPages/order/components/emoji.vue`
- Modify: `project-xzb-xcx-uniapp-java/subPages/history/index.vue`, `subPages/history/index.scss`, `subPages/history/details.vue`, `project-xzb-xcx-uniapp-java/tests/theme-contract.test.js`

**Interfaces:**
- Consumes: transaction/order component props, route query parameters and existing order/payment handlers.
- Produces: card-based transaction views, semantic status labels and unobscured fixed checkout/order actions.

- [ ] **Step 1: Add failing coverage assertions for all transaction paths**

Append every exact path in this task to `customerPages`; keep the existing loop test so it fails when a page lacks a `var(--warm-` style reference.

- [ ] **Step 2: Run the customer contract and verify it fails**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
```

Expected: newly listed transaction pages fail until restyled.

- [ ] **Step 3: Apply transaction hierarchy without changing flow code**

Use grouped surface cards for addresses, coupons, order summaries and service records. Apply `--state-success`, `--state-warning` and `--state-danger` to status labels only; keep all status value comparisons unchanged. Make pay/confirm/cancel affordances visually distinct, preserve bottom action handlers and ensure scrollable content has bottom padding equal to the footer height.

- [ ] **Step 4: Verify contract and build**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
npx vite build
```

Expected: all customer page coverage assertions and the build pass.

- [ ] **Step 5: Commit customer transaction work**

```powershell
git add project-xzb-xcx-uniapp-java/pages project-xzb-xcx-uniapp-java/subPages project-xzb-xcx-uniapp-java/tests/theme-contract.test.js
git commit -m "feat: restyle customer transaction pages"
```

### Task 6: Restyle service worker workday and setup pages

**Files:**
- Modify: `project-xzb-app-uniapp-java/pages/start/index.vue`, `pages/start/index.scss`, `pages/login/user.vue`, `pages/login/index.scss`, `pages/index/index.vue`, `pages/index/index.scss`, `pages/index/components/homeFilter.vue`, `pages/index/components/homeList.vue`
- Modify: `project-xzb-app-uniapp-java/pages/getOrder/index.vue`, `pages/getOrder/index.scss`, `pages/serviceSkill/index.vue`, `pages/serviceSkill/index.scss`, `pages/serviceRange/index.vue`, `pages/serviceRange/index.scss`, `pages/city/index.vue`, `pages/city/index.scss`, `pages/setting/index.vue`, `pages/setting/index.scss`
- Modify: `project-xzb-app-uniapp-java/tests/theme-contract.test.mjs`

**Interfaces:**
- Consumes: existing worker login, order list, filter, skill, range and setting data flows.
- Produces: high-scanability worker workbench, operational filters and setup forms using warm branding and semantic status colors.

- [ ] **Step 1: Add failing worker-page coverage assertions**

Create a `workerPages` array with all exact `.vue` paths listed in this task and apply the same `var(--warm-` assertion loop used by the worker contract.

- [ ] **Step 2: Run the worker contract and verify it fails**

```powershell
Set-Location project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
```

Expected: listed pages fail before their styles consume the warm layer.

- [ ] **Step 3: Apply the operational worker layout**

Make the workbench lead with status counts and actionable order cards. Make time, distance, customer address and action state visually scannable. Restyle login/start, skills, coverage range, city and settings as grouped forms. Preserve current methods, Vuex commits, API calls and component event signatures.

- [ ] **Step 4: Verify worker contract and build**

```powershell
Set-Location project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
npx vite build
```

Expected: worker page coverage passes and the build exits with code 0.

- [ ] **Step 5: Commit worker workday/setup work**

```powershell
git add project-xzb-app-uniapp-java/pages project-xzb-app-uniapp-java/tests/theme-contract.test.mjs
git commit -m "feat: restyle worker workbench pages"
```

### Task 7: Restyle service worker order, record and profile pages

**Files:**
- Modify: `project-xzb-app-uniapp-java/pages/pickup/index.vue`, `pages/pickup/index.scss`, `pages/pickup/components/homeList.vue`, `pages/orderInfo/index.vue`, `pages/orderInfo/index.scss`, `pages/cancel/index.vue`, `pages/serveRecord/index.vue`, `pages/serveRecord/index.scss`, `pages/history/index.vue`, `pages/history/index.scss`, `pages/history/components/homeList.vue`
- Modify: `project-xzb-app-uniapp-java/pages/delivery/index.vue`, `pages/delivery/index.scss`, `pages/evaluate/index.vue`, `pages/evaluate/index.scss`, `pages/evaluate/components/homeList.vue`, `pages/my/index.vue`, `pages/my/index.scss`, `pages/my/commponents/BaseInfo.vue`, `pages/my/commponents/Evaluate.vue`, `pages/my/commponents/HistoryScope.vue`
- Modify: `project-xzb-app-uniapp-java/pages/account/index.vue`, `pages/account/index.scss`, `pages/account/components/selectArea.vue`, `pages/account/components/selectArea.scss`, `pages/auth/index.vue`, `pages/auth/index.scss`, `pages/authFail/index.vue`, `pages/authFail/index.scss`, `project-xzb-app-uniapp-java/tests/theme-contract.test.mjs`

**Interfaces:**
- Consumes: existing order status, service record, message, evaluation, account and authentication flows.
- Produces: status-aware worker cards, details and profile forms with clear fixed action zones.

- [ ] **Step 1: Add the remaining worker files to the failing coverage array**

Append every exact `.vue` path listed in this task to `workerPages`; retain a single loop assertion to report each page that is missing the warm token reference.

- [ ] **Step 2: Run the worker contract and verify it fails**

```powershell
Set-Location project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
```

Expected: unmodified order, history, profile and authentication pages fail.

- [ ] **Step 3: Apply order and profile visual hierarchy**

Use white order cards on warm backgrounds, status chips using `--state-*`, concise metadata rows, and fixed primary action buttons that do not cover list content. Restyle messages, evaluations, user summary, account fields, authentication forms and failure feedback with the same spacing/radius rules. Keep all network and navigation code untouched.

- [ ] **Step 4: Verify worker contract and build**

```powershell
Set-Location project-xzb-app-uniapp-java
node --test tests/theme-contract.test.mjs
npx vite build
```

Expected: complete worker page coverage passes and the build exits with code 0.

- [ ] **Step 5: Commit worker order/profile work**

```powershell
git add project-xzb-app-uniapp-java/pages project-xzb-app-uniapp-java/tests/theme-contract.test.mjs
git commit -m "feat: restyle worker order and profile pages"
```

### Task 8: Complete visual, route and repository verification

**Files:**
- Modify only files required to resolve verification failures from Tasks 1-7.
- Test: `project-xzb-xcx-uniapp-java/tests/theme-contract.test.js`, `project-xzb-app-uniapp-java/tests/theme-contract.test.mjs`

**Interfaces:**
- Consumes: both finished source trees and their existing UniApp routes.
- Produces: buildable clients with all business page routes preserved and a clean repository boundary.

- [ ] **Step 1: Add a failing route-preservation assertion before any route repair**

If verification finds a missing page, add its expected path to the appropriate test and assert it is present in `pages.json`:

```js
assert.match(readFileSync(new URL('../pages.json', import.meta.url), 'utf8'), /pages\/index\/index/);
```

- [ ] **Step 2: Run complete automated checks**

```powershell
Set-Location project-xzb-xcx-uniapp-java
npm test
npx vite build
Set-Location ..\project-xzb-app-uniapp-java
node --test tests/repository-boundary.test.mjs tests/theme-contract.test.mjs
npx vite build
Set-Location ..
git diff --check
git status --short
```

Expected: all tests/builds exit with code 0, no whitespace errors are reported, and only intended source/docs changes appear.

- [ ] **Step 3: Perform visual and route smoke checks**

Open each client in its standard UniApp preview target. Navigate through every route declared in its `pages.json`; inspect the homepage/workbench, service or order list, order detail, profile, a form, and an empty state at a common mobile viewport. Confirm no horizontal overflow, no obscured fixed action, and no broken route. Record each result in the final delivery note.

- [ ] **Step 4: Commit final verification repairs**

```powershell
git add project-xzb-xcx-uniapp-java project-xzb-app-uniapp-java
git commit -m "test: verify dual client visual redesign"
```

## Plan Self-Review

- Scope coverage: Tasks 4-5 cover every customer page and subpackage from the specification; Tasks 6-7 cover every worker business page; Task 3 covers shared chrome; Task 1 covers GitHub management.
- No-placeholder check: search this file for unfinished-plan markers before execution and replace any occurrence with executable detail.
- Interface consistency: all tasks consume the `--warm-*` token names produced in Task 2, and all coverage tests call the same source-level token assertion.
