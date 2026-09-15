import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8');

test('首页通过路由参数传递所选服务分类', () => {
  const home = read('pages/index/index.vue');
  const service = read('pages/service/index.vue');

  assert.match(home, /url:\s*`\/pages\/service\/index\?serveTypeId=\$\{val\}`/);
  assert.match(service, /const normalizeServeTypeId = \(value\) => String\(value \?\? ''\)/);
  assert.match(service, /requestedServeTypeId\.value = normalizeServeTypeId\(options\?\.serveTypeId\)/);
  assert.match(service, /normalizeServeTypeId\(item\.serveTypeId\) === requestedServeTypeId\.value/);
});

test('AI chat has a warm welcome, quick prompts, and the active footer route', () => {
  const chat = read('pages/ai-chat/index.vue');

  assert.match(chat, /class="chatWelcomeCard"/);
  assert.match(chat, /\/static\/ai\/yuejia-assistant\.png/);
  assert.match(chat, /const quickQuestions = \[/);
  assert.match(chat, /v-for="question in quickQuestions"/);
  assert.match(chat, /<UniFooter :pagePath="'\/pages\/ai-chat\/index'"/);
  assert.match(chat, /streamAiMessage/);
});

test('service search validates city, waits for confirmation, and reserves result viewport space', () => {
  const search = read('pages/search/index.vue');
  const searchStyle = read('pages/search/index.scss');
  const list = read('pages/search/components/List.vue');
  const handleInput = search.match(/const handleInput = \(e\) => \{([\s\S]*?)\n\};/);

  assert.match(search, /const refreshSearchCity = \(\) =>/);
  assert.match(search, /const keyword = searchData\.value\.keyword\.trim\(\);/);
  assert.match(search, /if \(!refreshSearchCity\(\)\) return;/);
  assert.ok(handleInput);
  assert.doesNotMatch(handleInput[1], /getNewData\(/);
  assert.match(list, /class="resultList"/);
  assert.match(list, /\.resultList\s*\{[\s\S]*height:\s*calc\(100vh - 260rpx\)/);
  assert.match(searchStyle, /\.main\s*\{[\s\S]*min-height:\s*0/);
});

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

test('首页推荐图保比例裁剪而不是拉伸', () => {
  const home = read('pages/index/index.vue');
  const style = read('pages/index/index.scss');

  assert.match(home, /mode="widthFix"\s+class="cardImg"/);
  assert.doesNotMatch(style, /\.cardImg\s*\{[^}]*height:/);
});

test('个人信息使用纵向信息组，订单页有独立暖色容器', () => {
  const profile = read('pages/my/index.vue');
  const profileStyle = read('pages/my/index.scss');
  const orders = read('subPages/order/index.vue');

  assert.match(profile, /<view class="profileCopy">/);
  assert.match(profileStyle, /\.profileCopy\s*\{/);
  assert.doesNotMatch(profileStyle, /\.profileHint\s*\{[^}]*position:\s*absolute/);
  assert.match(orders, /orderPageWarm/);
});

test('AI 助手使用本地卡通形象并直接进入对话页', () => {
  const home = read('pages/index/index.vue');
  const footer = read('components/Foot/index.vue');
  const common = read('styles/common.scss');

  assert.doesNotMatch(home, /<AiBall/);
  assert.doesNotMatch(home, /import AiBall/);
  assert.match(footer, /"pagePath":\s*"\/pages\/ai-chat\/index"/);
  assert.match(footer, /"text":\s*"AI助手"/);
  assert.equal((footer.match(/"pagePath":/g) || []).length, 4);
  assert.match(common, /\.tabbar-item\s*\{[\s\S]*flex:\s*0 0 25%;/);
});

test('用户端显示悦家服务品牌', () => {
  const pages = read('pages.json');
  const home = read('pages/index/index.vue');
  const chat = read('pages/ai-chat/index.vue');

  assert.match(pages, /悦家服务/);
  assert.doesNotMatch(pages, /云岚到家/);
  assert.match(home, /悦家服务/);
  assert.match(chat, /欢迎来到悦家服务/);
});

test('mini-program login keeps consent actions reachable under Yuejia branding', () => {
  const login = read('pages/login/index.vue');
  const style = read('pages/login/index.scss');

  assert.match(login, /class="brandLockup"/);
  assert.match(login, /悦家服务/);
  assert.doesNotMatch(login, /static\/logo\.png/);
  assert.match(login, /class="agreementBody"/);
  assert.match(login, /class="agreementActions"/);
  assert.match(login, /@click="decryptPhoneNumber">同意并继续/);
  assert.match(style, /\.agreementBody\s*\{[\s\S]*overflow-y:\s*auto/);
  assert.match(style, /\.agreementActions\s*\{[\s\S]*flex-shrink:\s*0/);
});

test('mini-program H5 entry uses the Yuejia browser icon', () => {
  const entry = read('index.html');

  assert.match(entry, /<link rel="icon" href="\/favicon\.svg" \/>/);
});

test('reservation popup does not apply full-page styling to its address text', () => {
  const serviceDetail = read('pages/service/components/airMaintenance.vue');
  const warmBusiness = read('styles/warm-business.scss');

  assert.match(serviceDetail, /class="address" v-if="addressData\.province"/);
  assert.doesNotMatch(serviceDetail, /scrollBoxHeight/);
  assert.doesNotMatch(warmBusiness, /\.address,\s*\n/);
});

test('customer order tabs are equal-width and history avoids the capsule', () => {
  const order = read('subPages/order/index.vue');
  const nav = read('components/Navbar/index.vue');

  assert.match(order, /\.orderPageWarm :deep\(\.itemTab \.tabItem\)[\s\S]*flex:\s*1/);
  assert.match(nav, /:style="\{ right: historyRight \+ 'px' \}"/);
  assert.match(nav, /historyRight\.value\s*=\s*res\.screenWidth - menuButton\.left \+ 8/);
});

test('customer orders use a non-monkey empty visual and a five-item safe tab rail', () => {
  const empty = read('components/EmptyPage/index.vue');
  const list = read('subPages/order/components/list.vue');
  const order = read('subPages/order/index.vue');

  assert.match(list, /<EmptyPage[^>]*variant="order"/);
  assert.match(empty, /v-if="variant === 'order'"/);
  assert.match(empty, /class="orderEmptyVisual"/);
  assert.match(order, /left:\s*24rpx/);
  assert.match(order, /right:\s*24rpx/);
  assert.match(order, /width:\s*auto\s*!important/);
  assert.doesNotMatch(order, /width:\s*calc\(100% - 48rpx\)/);
});
