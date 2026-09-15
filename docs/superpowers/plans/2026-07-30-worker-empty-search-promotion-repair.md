# Worker Empty State, Search Layout, and Promotion Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the worker App monkey empty state, restore the mini-program search layout, and add a generated 1-yuan cleaning promotion banner.

**Architecture:** The worker empty state stays CSS-only and retains the `canPickUp` business branch. The search correction is local to its page and overrides the shared warm selector that mistakenly assigns viewport height to the search box. A text-free generated background is paired with Vue text so Chinese copy and the ¥1 price are exact.

**Tech Stack:** Vue 3 Composition API, UniApp, SCSS, Node static-contract tests, built-in ImageGen.

## Global Constraints

- Work only in `D:\develop\code\jzo2o2.0\.publish-jzo2o` on `main`.
- Preserve service APIs, search confirmation behavior, city validation, and service-detail routes.
- Do not overwrite `static/banner.png`; add a sibling static asset.
- Render the exact offer wording in Vue rather than in generated artwork.

---

### Task 1: Define failing UI contracts

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Modify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:**
- Consumes: existing `read()` source helpers.
- Produces: checks for `workerEmptyRadar`, automatic search-box height, generated promotion asset, and exact Vue copy.

- [ ] **Step 1: Add a failing worker-empty assertion**

```js
assert.match(empty, /class="workerEmptyRadar"/);
assert.match(empty, /保持在线，机会很快到来/);
assert.doesNotMatch(empty, /static\/new\/empty\.png/);
```

- [ ] **Step 2: Add two failing customer assertions in separate tests**

```js
test('search box uses automatic height below navigation', () => {
  const searchStyle = read('pages/search/index.scss');
  assert.match(searchStyle, /\.searchBox\s*\{[\s\S]*min-height:\s*auto/);
});

test('home promotion uses the one-yuan cleaning asset and copy', () => {
  const home = read('pages/index/index.vue');
  assert.match(home, /banner-cleaning-one-yuan\.png/);
  assert.match(home, /日常保洁限时优惠价/);
  assert.match(home, /¥1/);
});
```

- [ ] **Step 3: Run tests and verify red**

```powershell
# Run from project-xzb-app-uniapp-java
node --test tests/yuejia-worker-contract.test.mjs
# Run from project-xzb-xcx-uniapp-java
node --test tests/yuejia-ui-contract.test.mjs
```

Expected: each new contract fails before implementation.

- [ ] **Step 4: Commit**

```powershell
git add project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs
git commit -m "test: define empty search and promotion contracts"
```

### Task 2: Replace the worker App empty illustration

**Files:**
- Modify: `project-xzb-app-uniapp-java/components/empty/index.vue`
- Test: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

**Interfaces:**
- Consumes: `canPickUp: Boolean`.
- Produces: a `workerEmptyRadar` local illustration and no reference to the monkey asset.

- [ ] **Step 1: Implement the radar and toolbox composition**

```vue
<view class="workerEmptyRadar">
  <view class="radarRing ringOuter"></view>
  <view class="radarRing ringInner"></view>
  <view class="toolbox"><text>✓</text></view>
</view>
<view v-if="canPickUp" class="content">暂时没有符合条件的订单，保持在线，机会很快到来</view>
```

Use local SCSS pseudo-elements for radar arcs. Remove the `.image` background URL; keep the not-ready business message.

- [ ] **Step 2: Verify green and commit**

```powershell
node --test tests/yuejia-worker-contract.test.mjs
git add project-xzb-app-uniapp-java/components/empty/index.vue
git commit -m "feat: replace worker empty state"
```

Expected: worker test passes.

### Task 3: Generate and wire the 1-yuan cleaning promotion

**Files:**
- Create: `project-xzb-xcx-uniapp-java/static/banner-cleaning-one-yuan.png`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.scss`
- Test: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:**
- Consumes: one text-free 16:5 ImageGen background.
- Produces: `.promotionBanner` with `日常保洁限时优惠价`, `¥1`, and `专业保洁上门服务，限时优惠`.

- [ ] **Step 1: Generate a single background with ImageGen**

```text
Use case: ads-marketing
Asset type: Chinese mobile mini-program horizontal promotion banner background
Primary request: professional home cleaner in a neat apron preparing to clean a bright modern living room
Scene/backdrop: sunlit tidy apartment, sofa, soft beige and warm peach decor
Composition/framing: wide 16:5 banner, cleaner on right third, clean negative space on left
Lighting/mood: soft warm daylight, trustworthy and premium
Color palette: warm peach, coral, cream
Text: no text anywhere in the image
Constraints: no logos, no watermark, no price tags, no distorted hands or cleaning tools
```

Inspect the image and copy the selected output to the static path above.

- [ ] **Step 2: Add the exact Vue overlay**

```vue
<view class="promotionBanner">
  <image src="/static/banner-cleaning-one-yuan.png" mode="aspectFill" />
  <view class="promotionCopy">
    <view class="promotionLabel">限时福利</view>
    <view class="promotionTitle">日常保洁限时优惠价</view>
    <view class="promotionPrice">¥1</view>
    <view class="promotionHint">专业保洁上门服务，限时优惠</view>
  </view>
</view>
```

Style `.promotionBanner` to 206rpx, clip its existing rounded corners, and provide a left cream gradient behind the copy.

- [ ] **Step 3: Verify green and commit**

```powershell
node --test --test-name-pattern="home promotion uses" tests/yuejia-ui-contract.test.mjs
git add project-xzb-xcx-uniapp-java/static/banner-cleaning-one-yuan.png project-xzb-xcx-uniapp-java/pages/index/index.vue project-xzb-xcx-uniapp-java/pages/index/index.scss
git commit -m "feat: add one-yuan cleaning promotion"
```

Expected: customer test passes, including the exact promotion assertions.

### Task 4: Correct the scoped search-box height

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/search/index.scss`
- Test: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:**
- Consumes: current `searchBase`, `.main`, `.searchBox`, and List `resultList` classes.
- Produces: a normal-flow search row directly under NavBar while preserving the list scroll viewport.

- [ ] **Step 1: Override only the accidental shared height**

```scss
.searchBox {
  min-height: auto;
  background: transparent;
}

.searchBase > .main {
  min-height: 0;
}
```

- [ ] **Step 2: Verify green and commit**

```powershell
node --test --test-name-pattern="search box uses" tests/yuejia-ui-contract.test.mjs
git add project-xzb-xcx-uniapp-java/pages/search/index.scss
git commit -m "fix: restore search bar layout"
```

Expected: the search-box assertion and existing confirmed-search contract pass.

### Task 5: Full regression verification

**Files:**
- Verify: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`
- Verify: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

- [ ] **Step 1: Run the static contract suites**

```powershell
# Run from project-xzb-app-uniapp-java
node --test tests/yuejia-worker-contract.test.mjs
# Run from project-xzb-xcx-uniapp-java
node --test tests/yuejia-ui-contract.test.mjs
```

- [ ] **Step 2: Build the worker App H5 target**

Run from `project-xzb-app-uniapp-java`:

```powershell
$env:HX_APP_ROOT='D:\develop\HBuilderX'
$env:UNI_INPUT_DIR=(Get-Location).Path
$env:NODE_PATH='D:\develop\HBuilderX\plugins\uniapp-cli-vite\node_modules'
& 'C:\Users\14602\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'D:\develop\HBuilderX\plugins\uniapp-cli-vite\node_modules\@dcloudio\vite-plugin-uni\bin\uni.js' build -p h5 --outDir 'D:\develop\code\jzo2o2.0\.publish-jzo2o\project-xzb-app-uniapp-java\unpackage\dist\codex-h5-verify'
```

Expected: `DONE Build complete.`

- [ ] **Step 3: Inspect final changes**

```powershell
git diff --check
git status --short --branch
```

Expected: no whitespace errors and only intended commits ahead of `origin/main`.
