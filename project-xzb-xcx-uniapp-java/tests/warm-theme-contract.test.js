import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const theme = readFileSync(new URL('../styles/theme.scss', import.meta.url), 'utf8');
const app = readFileSync(new URL('../App.vue', import.meta.url), 'utf8');
const componentText = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');

test('warm visual tokens are available globally', () => {
  for (const token of [
    '--warm-page-bg',
    '--warm-surface',
    '--warm-primary',
    '--warm-primary-soft',
    '--warm-text',
    '--warm-text-muted',
    '--warm-line',
    '--state-success',
    '--state-warning',
    '--state-danger',
  ]) {
    assert.match(theme, new RegExp(token));
  }
  assert.match(app, /var\(--warm-page-bg\)/);
});

test('shared controls consume the warm token layer', () => {
  for (const path of ['../components/uni-nav/index.vue', '../components/uni-tab/index.vue']) {
    assert.match(componentText(path), /var\(--warm-primary\)/);
  }
});

test('global styles provide only the page background, never business page overrides', () => {
  assert.match(app, /page\s*\{[\s\S]*background-color:\s*var\(--warm-page-bg\)/);
  assert.doesNotMatch(app, /\.myPage,[\s\S]*background:\s*var\(--warm-page-bg\)\s*!important/);
  assert.doesNotMatch(app, /\.card,[\s\S]*box-shadow:\s*var\(--warm-shadow\)/);
  assert.doesNotMatch(app, /\.btn,[\s\S]*background:\s*var\(--warm-primary\)\s*!important/);
});

test('home page owns its warm hero, service grid, and recommendation card', () => {
  const home = componentText('../pages/index/index.vue');
  assert.match(home, /class="homeHero"/);
  assert.match(home, /class="serviceGrid"/);
  assert.match(home, /class="recommendCard"/);
});

test('profile page owns its warm hero and account panels', () => {
  const profile = componentText('../pages/my/index.vue');
  assert.match(profile, /class="profileHero"/);
  assert.match(profile, /class="[^"]*orderPanel[^"]*"/);
  assert.match(profile, /class="[^"]*supportPanel[^"]*"/);
});

test('business page styles import the local warm flow layer', () => {
  for (const path of [
    '../pages/search/index.scss',
    '../pages/address/index.scss',
    '../pages/city/index.scss',
    '../pages/pay/index.scss',
    '../pages/commit/index.scss',
    '../pages/login/index.scss',
    '../pages/service/index.vue',
    '../pages/service/components/airMaintenance.vue',
    '../pages/message/index.vue',
    '../pages/coupon/index.vue',
    '../pages/coupon/coupon.vue',
    '../pages/coupon/components/list.vue',
  ]) {
    assert.match(componentText(path), /@import\s+(?:url\()?['"]@\/styles\/warm-business\.scss['"]\)?;/);
  }
});

test('home keeps the coupon entry as a fixed benefit card instead of a floating window', () => {
  const home = componentText('../pages/index/index.vue');

  assert.match(home, /city-service-empty\.png/);
  assert.doesNotMatch(home, /zwnr@2x\.png/);
  assert.match(home, /class="couponBenefitCard"/);
  assert.match(home, /优惠福利/);
  assert.match(home, /coupon-empty\.png/);
  assert.match(home, /url:\s*'\/pages\/coupon\/index'/);
  assert.doesNotMatch(home, /ExpBall|expBall/);
});

test('each customer empty state uses a function-specific non-monkey illustration', () => {
  const empty = componentText('../components/EmptyPage/index.vue');
  const coupon = componentText('../pages/coupon/components/list.vue');

  assert.match(empty, /coupon-empty\.png/);
  assert.match(coupon, /variant="coupon"/);
  assert.match(componentText('../pages/address/index.vue'), /address-empty\.png/);
  assert.match(componentText('../pages/commit/index.vue'), /order-confirm-empty\.png/);
  assert.match(componentText('../subPages/history/index.vue'), /history-order-empty\.png/);

  for (const path of [
    '../components/EmptyPage/index.vue',
    '../pages/address/index.vue',
    '../pages/commit/index.vue',
    '../subPages/history/index.vue',
  ]) {
    assert.doesNotMatch(componentText(path), /zwnr@?2x\.png/);
  }
});
