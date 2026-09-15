import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8');

test('shared worker shell uses the established warm visual tokens', () => {
  const theme = read('styles/theme.scss');
  const homeNav = read('components/uni-home-nav/index.vue');
  const footer = read('components/uni-footer/index.vue');
  const tabs = read('components/uni-tab/index.vue');

  assert.match(theme, /--warm-page-bg:/);
  assert.match(theme, /--warm-primary:/);
  assert.match(theme, /--warm-surface:/);
  assert.match(theme, /--warm-shadow:/);
  assert.match(homeNav, /var\(--warm-primary\)/);
  assert.match(footer, /var\(--warm-primary\)/);
  assert.match(tabs, /var\(--warm-primary\)/);
});

test('worker home shows a service workbench banner instead of a housecleaning poster', () => {
  const homeNav = read('components/uni-home-nav/index.vue');

  assert.match(homeNav, /class="workbenchBanner"/);
  assert.match(homeNav, /今日专注服务/);
  assert.match(homeNav, /开启接单，附近订单将优先推送给你/);
  assert.doesNotMatch(homeNav, /img_baanner@2x\.png/);
});

test('worker empty state uses an order radar instead of the legacy monkey asset', () => {
  const empty = read('components/empty/index.vue');

  assert.match(empty, /class="workerEmptyRadar"/);
  assert.match(empty, /保持在线，机会很快到来/);
  assert.doesNotMatch(empty, /static\/new\/empty\.png/);
});

test('worker order thumbnails never fall back to the legacy monkey asset', () => {
  for (const path of [
    'pages/pickup/components/homeList.vue',
    'pages/history/components/homeList.vue',
    'pages/orderInfo/index.vue',
  ]) {
    const source = read(path);
    assert.match(source, /static\/new\/service-placeholder\.png/);
    assert.doesNotMatch(source, /static\/new\/empty\.png/);
  }
});

test('worker dispatch and order flows use page-level warm workspace markers', () => {
  const common = read('styles/common.scss');
  const home = read('pages/index/index.vue');
  const pickup = read('pages/pickup/index.vue');
  const history = read('pages/history/index.vue');
  const detail = read('pages/orderInfo/index.vue');
  const record = read('pages/serveRecord/index.vue');
  const cancel = read('pages/cancel/index.vue');

  assert.match(home, /workerHomeWarm/);
  assert.match(pickup, /workerOrderWarm/);
  assert.match(history, /workerOrderWarm/);
  assert.match(detail, /workerDetailWarm/);
  assert.match(record, /workerDetailWarm/);
  assert.match(cancel, /workerDetailWarm/);
  assert.match(common, /\.workerHomeWarm/);
  assert.match(common, /\.workerOrderWarm/);
  assert.match(common, /\.workerDetailWarm/);
  assert.match(common, /var\(--warm-surface\)/);
});

test('worker profile, settings, and certification flows use warm page markers', () => {
  const common = read('styles/common.scss');

  assert.match(read('pages/my/index.vue'), /workerProfileWarm/);
  assert.match(read('pages/delivery/index.vue'), /workerProfileWarm/);
  assert.match(read('pages/setting/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/serviceSkill/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/getOrder/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/serviceRange/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/city/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/evaluate/index.vue'), /workerSettingsWarm/);
  assert.match(read('pages/account/index.vue'), /workerFormWarm/);
  assert.match(read('pages/auth/index.vue'), /workerFormWarm/);
  assert.match(read('pages/authFail/index.vue'), /workerFormWarm/);
  assert.match(common, /\.workerProfileWarm/);
  assert.match(common, /\.workerSettingsWarm/);
  assert.match(common, /\.workerFormWarm/);
});

test('服务人员 App 不在根样式中覆盖首页导航与通用条目', () => {
  const app = read('App.vue');

  assert.doesNotMatch(app, /\.navFrame,[\s\S]*min-height:\s*100vh/);
  assert.doesNotMatch(app, /\.item,[\s\S]*\.boxBg/);
  assert.doesNotMatch(app, /\.btn,[\s\S]*button\[type='primary'\]/);
});

test('服务人员端显示悦家服务品牌', () => {
  const manifest = read('manifest.json');
  const pages = read('pages.json');

  assert.match(manifest, /悦家服务·服务端/);
  assert.match(pages, /悦家服务/);
  assert.doesNotMatch(pages, /云岚到家/);
});

test('服务人员 App 声明 HBuilderX 所需的构建依赖', () => {
  const packageJson = JSON.parse(read('package.json'));
  const dependencies = packageJson.devDependencies || {};

  assert.equal(dependencies['@dcloudio/vite-plugin-uni'], '3.0.0-alpha-5000120260205002');
  assert.equal(dependencies['@dcloudio/uni-components'], '3.0.0-alpha-5000120260205002');
  assert.equal(dependencies.vite, '5.2.8');
  assert.equal(dependencies.vue, '3.4.21');
  assert.match(dependencies.sass || '', /^\^?1\./);
});

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

test('worker start page does not retain the legacy logo image', () => {
  const start = read('pages/start/index.vue');
  const style = read('pages/start/index.scss');

  assert.match(start, /class="startBrandLockup"/);
  assert.match(start, /悦家服务/);
  assert.doesNotMatch(style, /img_logo@2x\.png/);
});

test('evaluation page uses its own empty-state meaning', () => {
  const evaluate = read('pages/evaluate/index.vue');
  const empty = read('components/empty/index.vue');

  assert.match(evaluate, /<Empty v-else variant="evaluation"><\/Empty>/);
  assert.match(empty, /variant:\s*\{[\s\S]*default:\s*'order'/);
  assert.match(empty, /v-if="variant === 'evaluation'"/);
  assert.match(empty, /暂无评价记录，完成服务后，客户的评价会出现在这里/);
  assert.match(empty, /暂时没有符合条件的订单/);
});

test('rob-order feedback uses a warm semantic mark, not legacy face assets', () => {
  const list = read('pages/index/components/homeList.vue');
  const style = read('pages/index/index.scss');

  assert.match(list, /class="resultMark"/);
  assert.doesNotMatch(list, /class="img"/);
  assert.doesNotMatch(style, /img_success@2x\.png|img_fail@2x\.png/);
});

test('worker order empty state describes the selected category', () => {
  const pickup = read('pages/pickup/index.vue');
  const empty = read('components/empty/index.vue');

  assert.match(pickup, /variant="worker-order"/);
  assert.match(pickup, /:emptyLabel="tabBars\[users\.tabIndex\]\.label"/);
  assert.match(empty, /variant === 'worker-order'/);
});

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
