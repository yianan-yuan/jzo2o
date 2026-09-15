# 移动端页面重做纠偏 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复服务人员 App 的 HBuilderX 编译路径，并把小程序改为逐页实现的暖橙安心视觉。

**Architecture:** 服务人员 App 保持源码项目形态，不在项目内安装 UniApp 构建器。小程序保留 `styles/theme.scss` 令牌，但仅在各页面和共享组件中显式使用；`App.vue` 只保留默认页面底色。

**Tech Stack:** Vue 3、UniApp、SCSS、Node built-in test、HBuilderX。

## Global Constraints

- 不修改 API、路由、`pages.json` 或业务数据流。
- 不提交 `node_modules`、`unpackage`、IDE 配置或私有运行配置。
- 不使用带 `!important` 的通用业务页面、卡片、按钮或标签全局选择器。
- 保留暖橙令牌与通用导航/标签组件；页面色值从 `theme.scss` 读取。

---

### Task 1: 固化服务人员 App 的 HBuilderX 构建边界

**Files:**
- Modify: `project-xzb-app-uniapp-java/tests/repository-boundary.test.mjs`
- Verify: `project-xzb-app-uniapp-java/.gitignore`

**Interfaces:**
- Produces: `worker App stays free of local build runtime` 契约。

- [ ] **Step 1: 写失败测试**

```js
const packageJson = JSON.parse(readFileSync('package.json', 'utf8'));
assert.equal(existsSync('node_modules'), false);
assert.equal(packageJson.devDependencies?.vite, undefined);
assert.equal(packageJson.devDependencies?.['@dcloudio/vite-plugin-uni'], undefined);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/repository-boundary.test.mjs`

Expected: 本地依赖或错误构建依赖存在时 FAIL。

- [ ] **Step 3: 清除仅用于排查的本地 node_modules，保留业务 package.json**

```powershell
Test-Path node_modules
Get-Content package.json
```

- [ ] **Step 4: 验证并提交**

Run: `node --test tests/repository-boundary.test.mjs`

Expected: PASS；HBuilderX 启动时不再解析项目本地旧版 Vite 插件。

```powershell
git add project-xzb-app-uniapp-java/tests/repository-boundary.test.mjs
git commit -m "fix: restore worker app hbuilder build boundary"
```

### Task 2: 移除小程序全局覆盖层

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/App.vue`
- Modify: `project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js`

**Interfaces:**
- Consumes: `--warm-page-bg`。
- Produces: 只设置 `page` 默认底色的全局 SCSS。

- [ ] **Step 1: 写失败测试**

```js
assert.doesNotMatch(app, /\.myPage,[\s\S]*background:\s*var\(--warm-page-bg\)\s*!important/);
assert.doesNotMatch(app, /\.card,[\s\S]*box-shadow:\s*var\(--warm-shadow\)/);
assert.match(app, /page\s*\{[\s\S]*background-color:\s*var\(--warm-page-bg\)/);
```

- [ ] **Step 2: 运行失败测试**

Run: `npm test -- --test-name-pattern="global"`

Expected: 当前 `.myPage`、`.card`、`.btn` 覆盖层导致 FAIL。

- [ ] **Step 3: 删除业务类名全局覆盖并保留最小全局样式**

```scss
page {
  background-color: var(--warm-page-bg);
}
```

- [ ] **Step 4: 验证并提交**

Run: `npm test`

Expected: PASS。

```powershell
git add project-xzb-xcx-uniapp-java/App.vue project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js
git commit -m "fix: remove global mobile page overrides"
```

### Task 3: 重做小程序首页

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/index/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js`

**Interfaces:**
- Consumes: `city`、`menuData`、`hotData`、`toCity`、`handleSearch`、`toService`。
- Produces: `homeHero`、`homeSearch`、`serviceGrid`、`recommendCard`。

- [ ] **Step 1: 写失败测试**

```js
const home = componentText('../pages/index/index.vue');
assert.match(home, /class="homeHero"/);
assert.match(home, /class="serviceGrid"/);
assert.match(home, /class="recommendCard"/);
```

- [ ] **Step 2: 运行失败测试**

Run: `npm test -- --test-name-pattern="home"`

Expected: FAIL，因为旧首页只包含 `homeBox`、`menu`、`recommend`。

- [ ] **Step 3: 保留原有事件和数据绑定，加入专用布局类**

```vue
<view class="homeHero">
  <view class="homeHeroTitle">云洁到家</view>
  <view class="homeSearch" @click="handleSearch">...</view>
</view>
<view class="serviceGrid">...</view>
<view class="recommendCard">...</view>
```

- [ ] **Step 4: 实现页面局部 SCSS**

```scss
.homeHero { background: linear-gradient(145deg, #f78d67, var(--warm-primary)); }
.serviceGrid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20rpx; }
.recommendCard { background: var(--warm-surface); border-radius: var(--warm-radius-lg); box-shadow: var(--warm-shadow); }
```

- [ ] **Step 5: 验证并提交**

Run: `npm test`

Expected: PASS；微信开发者工具中搜索、分类、推荐服务和底部导航清晰可见。

```powershell
git add project-xzb-xcx-uniapp-java/pages/index project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js
git commit -m "feat: rebuild mini-program home page"
```

### Task 4: 重做小程序“我的”页

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/my/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/my/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js`

**Interfaces:**
- Consumes: `nickName`、`token`、`handleClick`、`handleOrder`、`handleAddress`、`handleCoupon`、`handleBill`、`handlePhone`。
- Produces: `profileHero`、`orderPanel`、`accountPanel`、`supportPanel`。

- [ ] **Step 1: 写失败测试**

```js
const profile = componentText('../pages/my/index.vue');
assert.match(profile, /class="profileHero"/);
assert.match(profile, /class="orderPanel"/);
assert.match(profile, /class="supportPanel"/);
```

- [ ] **Step 2: 运行失败测试**

Run: `npm test -- --test-name-pattern="profile"`

Expected: FAIL，因为旧页面没有局部头部和面板类。

- [ ] **Step 3: 给既有头像、订单、功能菜单和客服电话增加语义化容器**

```vue
<view class="profileHero"><view class="head" @click="handleClick">...</view></view>
<view class="box orderPanel">...</view>
<view class="box accountPanel">...</view>
<view class="phoneMenu supportPanel">...</view>
```

- [ ] **Step 4: 恢复背景图层并实现页面局部白卡**

```scss
.myPage { background-color: var(--warm-page-bg); background-image: url('data:image/png;base64,...'); }
.profileHero { background: linear-gradient(145deg, #f78d67, var(--warm-primary)); border-radius: 0 0 44rpx 44rpx; }
.orderPanel, .accountPanel, .supportPanel { background: var(--warm-surface); border-radius: var(--warm-radius-lg); box-shadow: var(--warm-shadow); }
```

- [ ] **Step 5: 验证并提交**

Run: `npm test`

Expected: PASS；头像、昵称、订单卡片与入口文字可见。

```powershell
git add project-xzb-xcx-uniapp-java/pages/my project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js
git commit -m "feat: rebuild mini-program profile page"
```

### Task 5: 逐页接入其他业务流

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/service/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/service/components/airMaintenance.vue`
- Modify: `project-xzb-xcx-uniapp-java/pages/{search,address,city,coupon,pay,commit,login}/index.scss`
- Modify: `project-xzb-xcx-uniapp-java/pages/message/index.vue`
- Modify: `project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js`

**Interfaces:**
- Consumes: `styles/theme.scss`。
- Produces: 每页自己的主题样式，不依赖 `App.vue` 的业务类名覆盖。

- [ ] **Step 1: 写失败测试，列出必须接入令牌的样式文件**

```js
for (const path of ['../pages/search/index.scss', '../pages/address/index.scss', '../pages/city/index.scss', '../pages/coupon/index.scss', '../pages/pay/index.scss']) {
  assert.match(componentText(path), /var\(--warm-(page-bg|surface|primary|line)\)/);
}
```

- [ ] **Step 2: 运行失败测试**

Run: `npm test -- --test-name-pattern="business"`

Expected: FAIL，因为旧页面仍使用固定灰白或红色值。

- [ ] **Step 3: 按页面职责替换背景、卡片、边框和主操作色**

```scss
.pageRoot { background: var(--warm-page-bg); }
.sectionCard { background: var(--warm-surface); border: 1rpx solid var(--warm-line); border-radius: var(--warm-radius-lg); }
.primaryAction { background: var(--warm-primary); color: #fff; }
```

- [ ] **Step 4: 验证并提交**

Run: `npm test`

Expected: PASS；服务、详情、搜索、地址、城市、优惠券、支付、评价、消息和登录不依赖全局覆盖。

```powershell
git add project-xzb-xcx-uniapp-java/pages project-xzb-xcx-uniapp-java/tests/warm-theme-contract.test.js
git commit -m "feat: apply page-level warm styling to mini-program flows"
```

### Task 6: 回归验证和交付

**Files:**
- Verify: `project-xzb-xcx-uniapp-java/tests/`
- Verify: `project-xzb-app-uniapp-java/tests/`

- [ ] **Step 1: 运行所有 Node 测试**

```powershell
Set-Location project-xzb-xcx-uniapp-java; npm test
Set-Location ..\project-xzb-app-uniapp-java; node --test tests/repository-boundary.test.mjs tests/warm-theme-contract.test.mjs
```

- [ ] **Step 2: 检查提交内容**

```powershell
git diff --check origin/main..HEAD
git diff --name-only origin/main..HEAD
git status --short
```

- [ ] **Step 3: 人工预览**

Expected: 微信开发者工具中首页/我的页无白字、空白卡片或布局覆盖；HBuilderX 中服务人员 App 可启动。

- [ ] **Step 4: 推送 main**

```powershell
git push origin main
```
