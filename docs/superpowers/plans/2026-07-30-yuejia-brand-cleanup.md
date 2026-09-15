# 悦家服务全端品牌清理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将小程序、服务人员 App 和管理端的可见品牌统一为悦家服务，并恢复小程序服务条款确认后的登录入口。

**Architecture:** 三个客户端各自保留原有路由和登录接口，只替换其页面级品牌呈现。小程序条款弹窗拆成可滚动正文和固定操作栏，保证“同意并继续”始终可点击；管理端与服务人员 App 使用代码品牌标识替代旧的带字位图。

**Tech Stack:** Vue 3、uni-app、SCSS、Vue TypeScript、TDesign、Node.js 内置测试。

## Global Constraints

- 用户可见品牌统一为“悦家服务”，服务人员 App 角色为“服务端”，管理端角色为“管理端”。
- 移除“仅用于 IT 培训教学使用”提示。
- 小程序不得绕过服务条款；确认按钮调用现有 `decryptPhoneNumber` 登录函数。
- 不修改 API、路由、登录鉴权、微信/支付配置、`tencent.wechat` 包配置或 `czriComponents` 路径。
- 不引用包含旧品牌文字的旧 logo 位图作为新登录或管理端侧栏标识。

---

## File Structure

- `project-xzb-xcx-uniapp-java/pages/login/index.vue`：小程序品牌标识、条款操作文案和登录触发点。
- `project-xzb-xcx-uniapp-java/pages/login/index.scss`：可滚动条款正文与固定操作栏布局。
- `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`：小程序品牌和可达登录操作的静态合约。
- `project-xzb-app-uniapp-java/pages/login/user.vue` 与 `pages/login/index.scss`：服务人员 App 登录品牌与样式。
- `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`：服务人员 App 登录品牌合约。
- `project-xzb-pc-admin-vue3-java/src/pages/login/index.vue`、`components/Login.vue`、`src/layouts/simpleComponents/SideNav.vue`：管理端登录和侧栏品牌。
- `project-xzb-pc-admin-vue3-java/src/layouts/components/Footer.vue`：管理端通用页脚版权。
- `project-xzb-pc-admin-vue3-java/src/store/modules/notification.ts`：用户可见示例通知。
- `project-xzb-pc-admin-vue3-java/index.html`、`README.md`、`docs/index.html`、`docs/cover.md`：管理端入口和文档名称。
- `project-xzb-pc-admin-vue3-java/tests/yuejia-brand-contract.test.mjs`：管理端品牌与版权的静态合约。
- `project-xzb-pc-admin-vue3-java/package.json`：增加执行管理端静态合约的 `test:brand` 脚本。

## Task 1: 小程序登录品牌与可达条款确认

**Files:**
- Modify: `project-xzb-xcx-uniapp-java/pages/login/index.vue:5-35`
- Modify: `project-xzb-xcx-uniapp-java/pages/login/index.scss:4-67`
- Test: `project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs`

**Interfaces:**
- Consumes: 既有 `decryptPhoneNumber()`、`handleClose()` 与 `uni-popup` ref。
- Produces: 条款确认按钮 `@click="decryptPhoneNumber"`，不会改变登录 API 或令牌处理。

- [ ] **Step 1: 写入失败的静态合约**

在 `yuejia-ui-contract.test.mjs` 追加测试，要求登录页不再使用旧 logo，包含代码品牌文字、滚动正文容器和固定操作栏：

```js
test('mini-program login uses Yuejia branding and keeps consent actions reachable', () => {
  const login = read('pages/login/index.vue');
  const style = read('pages/login/index.scss');

  assert.match(login, /class="brandLockup"/);
  assert.match(login, /悦家服务/);
  assert.doesNotMatch(login, /static\/logo\.png/);
  assert.match(login, /class="agreementBody"/);
  assert.match(login, /class="agreementActions"/);
  assert.match(login, /@click="decryptPhoneNumber">同意并继续/);
  assert.match(style, /\.agreementBody[\s\S]*overflow-y:\s*auto/);
  assert.match(style, /\.agreementActions[\s\S]*flex-shrink:\s*0/);
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/yuejia-ui-contract.test.mjs`

Expected: FAIL，提示找不到 `brandLockup` 或 `agreementBody`。

- [ ] **Step 3: 实现最小登录页与弹窗布局修改**

在 `index.vue` 用文本品牌块替换旧 `<image src="../../static/logo.png">`，并将条款正文、操作区分别命名：

```vue
<view class="brandLockup">
  <view class="brandMark">✦</view>
  <view class="brandName">悦家服务</view>
  <view class="brandTagline">让每一次上门服务都更安心</view>
</view>

<scroll-view class="agreementBody" scroll-y>
  <!-- 既有服务条款正文 -->
</scroll-view>
<view class="agreementActions">
  <button class="cancel-btn btn" @click="handleClose">不同意</button>
  <button class="agree-btn btn" @click="decryptPhoneNumber">同意并继续</button>
</view>
```

将 `.servicePop :deep(.uni-popup__wrapper)` 改为 `max-height: 72vh` 的纵向 flex 容器，去掉固定 `height: 500rpx` 和压缩操作区的 `padding`；为 `.agreementBody` 设置 `flex: 1; overflow-y: auto`，为 `.agreementActions` 设置 `flex-shrink: 0`。保留 `decryptPhoneNumber` 的现有实现。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test tests/yuejia-ui-contract.test.mjs`

Expected: PASS。

- [ ] **Step 5: 提交小程序改动**

```powershell
git add project-xzb-xcx-uniapp-java/pages/login/index.vue project-xzb-xcx-uniapp-java/pages/login/index.scss project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs
git commit -m "fix: restore mini program login consent action"
```

## Task 2: 服务人员 App 登录品牌清理

**Files:**
- Modify: `project-xzb-app-uniapp-java/pages/login/user.vue:3-50`
- Modify: `project-xzb-app-uniapp-java/pages/login/index.scss:112-126`
- Test: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

**Interfaces:**
- Consumes: 服务人员 App 的既有登录表单和 `pages/login/index.scss`。
- Produces: `brandLockup` 仅负责视觉显示，不改变表单、验证码或登录事件。

- [ ] **Step 1: 写入失败的静态合约**

追加测试：

```js
test('worker login shows Yuejia service branding without training notice', () => {
  const login = read('pages/login/user.vue');
  const style = read('pages/login/index.scss');

  assert.match(login, /class="workerBrandLockup"/);
  assert.match(login, /悦家服务/);
  assert.match(login, /服务端/);
  assert.doesNotMatch(login, /img_logo@2x\.png/);
  assert.doesNotMatch(login, /仅用于IT培训教学使用/);
  assert.doesNotMatch(style, /\.gentleReminder/);
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: FAIL，提示找不到 `workerBrandLockup`。

- [ ] **Step 3: 实现代码品牌标识并删除培训提示**

在 `user.vue` 使用下面的结构替换旧 `<image>` 和培训提示：

```vue
<view class="workerBrandLockup">
  <view class="workerBrandMark">✦</view>
  <view>
    <view class="workerBrandName">悦家服务</view>
    <view class="workerBrandRole">服务端</view>
  </view>
</view>
```

在 `index.scss` 删除 `.gentleReminder` 与 `.logo`；新增 `.workerBrandLockup`、`.workerBrandMark`、`.workerBrandName` 和 `.workerBrandRole`，采用既有暖橙 `var(--warm-primary)`、居中布局，不影响 `.loginBox` 内输入框和按钮规则。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: PASS。

- [ ] **Step 5: 提交服务人员 App 改动**

```powershell
git add project-xzb-app-uniapp-java/pages/login/user.vue project-xzb-app-uniapp-java/pages/login/index.scss project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs
git commit -m "feat: rebrand worker login"
```

## Task 3: 管理端登录、侧栏、页脚与示例通知

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/index.html:8`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/login/index.vue:5-16,55-58`
- Modify: `project-xzb-pc-admin-vue3-java/src/pages/login/components/Login.vue:45-52,105-110`
- Modify: `project-xzb-pc-admin-vue3-java/src/layouts/simpleComponents/SideNav.vue:10-30,153-167`
- Modify: `project-xzb-pc-admin-vue3-java/src/layouts/components/Footer.vue:1-5`
- Modify: `project-xzb-pc-admin-vue3-java/src/store/modules/notification.ts:5-9`
- Create: `project-xzb-pc-admin-vue3-java/tests/yuejia-brand-contract.test.mjs`
- Modify: `project-xzb-pc-admin-vue3-java/package.json`

**Interfaces:**
- Consumes: TDesign layout slots和既有 `showLogo`、`collapsed` 条件。
- Produces: 展开、收起及明暗主题均由同一个文本品牌组件显示；登录表单提交保持 `onSubmit` 不变。

- [ ] **Step 1: 创建失败的管理端品牌合约**

创建 `tests/yuejia-brand-contract.test.mjs`，读取入口和关键组件并断言：

```js
assert.match(read('index.html'), /<title>悦家服务管理端<\/title>/);
assert.match(read('src/pages/login/index.vue'), /悦家服务/);
assert.doesNotMatch(read('src/pages/login/index.vue'), /logofull\.png/);
assert.doesNotMatch(read('src/pages/login/components/Login.vue'), /仅用于IT培训教学使用/);
assert.match(read('src/layouts/simpleComponents/SideNav.vue'), /悦家服务/);
assert.doesNotMatch(read('src/layouts/simpleComponents/SideNav.vue'), /test-img\/logofull\.png/);
assert.match(read('src/layouts/components/Footer.vue'), /悦家服务/);
```

在 `package.json` 的 scripts 中加入：

```json
"test:brand": "node --test tests/yuejia-brand-contract.test.mjs"
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:brand`

Expected: FAIL，因脚本或新断言尚不存在。

- [ ] **Step 3: 替换管理端可见品牌和版权**

在登录页和侧栏用可复用的内联 DOM 标识替代旧图片；文字必须包含 `悦家服务`，登录页包含 `管理端`。侧栏在 `collapsed` 时仅显示暖橙色标记，在展开时显示标记和名称。删除 `Login.vue` 的 `.tips` DOM 和 CSS。

将 `index.html` 标题改为 `悦家服务管理端`，登录和通用页脚改为：

```vue
Copyright © 2026 悦家服务. All Rights Reserved.
```

将示例通知改为：

```ts
content: '悦家服务中心新增保洁服务已通过审核！'
```

- [ ] **Step 4: 运行管理端品牌合约**

Run: `npm run test:brand`

Expected: PASS。

- [ ] **Step 5: 提交管理端界面改动**

```powershell
git add project-xzb-pc-admin-vue3-java/index.html project-xzb-pc-admin-vue3-java/src project-xzb-pc-admin-vue3-java/tests/yuejia-brand-contract.test.mjs project-xzb-pc-admin-vue3-java/package.json
git commit -m "feat: rebrand admin console"
```

## Task 4: 更新对外项目说明与文档站

**Files:**
- Modify: `project-xzb-pc-admin-vue3-java/README.md:1-3`
- Modify: `project-xzb-pc-admin-vue3-java/docs/index.html:5,20`
- Modify: `project-xzb-pc-admin-vue3-java/docs/cover.md:3-9`
- Test: `project-xzb-pc-admin-vue3-java/tests/yuejia-brand-contract.test.mjs`

**Interfaces:**
- Consumes: Task 3 的管理端品牌名。
- Produces: GitHub README 和文档站入口均显示“悦家服务管理端”。

- [ ] **Step 1: 扩展失败的文档品牌断言**

在 `yuejia-brand-contract.test.mjs` 添加：

```js
assert.match(read('README.md'), /# 悦家服务-管理端/);
assert.doesNotMatch(read('README.md'), /云岚到家|云岚家政/);
assert.match(read('docs/index.html'), /悦家服务管理端/);
assert.match(read('docs/cover.md'), /悦家服务管理端/);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:brand`

Expected: FAIL，提示文档仍包含旧标题。

- [ ] **Step 3: 修改对外可见文档名称**

将 README 的标题和首段项目名改为“悦家服务-管理端”与“悦家服务家政项目”；将文档站 `<title>`、Docsify `name`、封面介绍改为“悦家服务管理端”。保留 TDesign 链接、`czriComponents` 技术路径和历史仓库链接，避免破坏文档示例。

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:brand`

Expected: PASS。

- [ ] **Step 5: 提交文档品牌改动**

```powershell
git add project-xzb-pc-admin-vue3-java/README.md project-xzb-pc-admin-vue3-java/docs project-xzb-pc-admin-vue3-java/tests/yuejia-brand-contract.test.mjs
git commit -m "docs: rename admin console branding"
```

## Task 5: 全量扫描与构建验证

**Files:**
- Modify: 仅在测试或构建暴露问题时修改对应文件。

**Interfaces:**
- Consumes: 前四项的页面与静态合约。
- Produces: 可复现的全仓品牌扫描、三组静态测试和管理端生产构建结果。

- [ ] **Step 1: 运行三个静态测试**

```powershell
node --test project-xzb-xcx-uniapp-java/tests/yuejia-ui-contract.test.mjs
node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs
Set-Location project-xzb-pc-admin-vue3-java; npm run test:brand
```

Expected: 三个命令均 PASS。

- [ ] **Step 2: 扫描可见旧品牌残留**

```powershell
rg -n -i "云岚到家|云岚家政|仅用于IT培训|IT培训教学" . -g '!**/node_modules/**' -g '!**/dist/**' -g '!**/unpackage/**' -g '!**/.git/**' -g '!**/uni_modules/**'
```

Expected: 无匹配；若只出现在合约测试的否定断言中，调整扫描范围排除 `tests/` 后应无匹配。

- [ ] **Step 3: 构建管理端**

Run: `npm run build`

Expected: Vite 生产构建成功；仅记录不阻断构建的既有警告。

- [ ] **Step 4: 构建服务人员 App 并记录环境结果**

Run: `npm run build:h5`

Expected: 成功；若 HBuilderX Sass 插件环境仍报未安装，记录为本机编译环境限制，不能归因于本次品牌改动。

- [ ] **Step 5: 检查提交状态并提交验证修正（如有）**

```powershell
git status --short
git diff --check
```

若验证没有产生修正，不创建额外提交；若为通过测试而修正了源文件或测试，则以 `test: verify Yuejia brand cleanup` 提交。
