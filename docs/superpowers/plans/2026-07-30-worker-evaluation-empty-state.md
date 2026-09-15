# 服务人员评价空状态 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为服务人员 App 的“我的评价”页显示正确的评价空状态，保留订单页空状态行为。

**Architecture:** 在现有 `components/empty` 中增加展示变体 `variant`，默认为 `order`。评价页显式传入 `evaluation`，组件根据变体渲染评价图案与文案；不改变 API、Tab 或列表逻辑。

**Tech Stack:** Vue 3、uni-app、SCSS、Node.js 内置测试。

## Global Constraints

- 评价变体文案必须为“暂无评价记录，完成服务后，客户的评价会出现在这里”。
- 默认 `order` 变体保留现有“暂无符合条件的订单”行为。
- 不修改评价列表接口、筛选 Tab、分页与底部导航。

---

### Task 1: 为评价页提供独立空状态变体

**Files:**

- Modify: `project-xzb-app-uniapp-java/components/empty/index.vue:1-113`
- Modify: `project-xzb-app-uniapp-java/pages/evaluate/index.vue:24`
- Test: `project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

**Interfaces:**

- Consumes: `Empty` 的既有可选 `canPickUp: boolean` 属性。
- Produces: `variant?: 'order' | 'evaluation'`；当 `variant === 'evaluation'` 时展示评价图案和评价文案。

- [ ] **Step 1: 写入失败的回归测试**

在 `yuejia-worker-contract.test.mjs` 添加：

```js
test('evaluation page uses its own empty-state meaning', () => {
  const evaluate = read('pages/evaluate/index.vue');
  const empty = read('components/empty/index.vue');

  assert.match(evaluate, /<Empty v-else variant="evaluation"><\/Empty>/);
  assert.match(empty, /variant:\s*\{[\s\S]*default:\s*'order'/);
  assert.match(empty, /v-if="variant === 'evaluation'"/);
  assert.match(empty, /暂无评价记录，完成服务后，客户的评价会出现在这里/);
  assert.match(empty, /暂无符合条件的订单/);
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: FAIL，因评价页尚未传入 `variant="evaluation"`。

- [ ] **Step 3: 实现最小评价变体**

将评价页空状态替换为：

```vue
<Empty v-else variant="evaluation"></Empty>
```

在 `components/empty/index.vue` 的 props 中加入：

```js
variant: {
  type: String,
  default: 'order',
},
```

当 `variant === 'evaluation'` 时渲染星级和对话气泡组成的 `evaluationEmptyVisual`，并显示：

```vue
<view class="content">暂无评价记录，完成服务后，客户的评价会出现在这里</view>
```

将原有雷达、工具箱和 `canPickUp` 订单文案包裹在 `v-else` 中。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test tests/yuejia-worker-contract.test.mjs`

Expected: PASS。

- [ ] **Step 5: 提交修复**

```powershell
git add project-xzb-app-uniapp-java/components/empty/index.vue project-xzb-app-uniapp-java/pages/evaluate/index.vue project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs
git commit -m "fix: add evaluation empty state"
```

### Task 2: 回归验证

**Files:**

- Modify: 仅当测试暴露问题时修改 Task 1 中的文件。

- [ ] **Step 1: 运行服务人员 App 全量静态合约**

Run: `node --test project-xzb-app-uniapp-java/tests/yuejia-worker-contract.test.mjs`

Expected: 全部 PASS。

- [ ] **Step 2: 检查工作区**

```powershell
git diff --check
git status --short --branch
```

Expected: 无空白错误；除已提交结果外没有未提交修改。
