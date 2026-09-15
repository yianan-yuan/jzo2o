import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8');

test('admin console uses Yuejia branding without legacy login notices', () => {
  const login = read('src/pages/login/index.vue');
  const loginForm = read('src/pages/login/components/Login.vue');
  const sideNav = read('src/layouts/simpleComponents/SideNav.vue');

  assert.match(read('index.html'), /<title>悦家服务管理端<\/title>/);
  assert.match(login, /悦家服务/);
  assert.match(login, /管理端/);
  assert.doesNotMatch(login, /logofull\.png/);
  assert.doesNotMatch(loginForm, /仅用于IT培训教学使用/);
  assert.doesNotMatch(loginForm, /class="tips"/);
  assert.match(sideNav, /悦家服务/);
  assert.doesNotMatch(sideNav, /test-img\/logofull\.png/);
  assert.doesNotMatch(sideNav, /logBlackTem/);
  assert.match(read('src/layouts/components/Footer.vue'), /悦家服务/);
  assert.match(read('src/store/modules/notification.ts'), /悦家服务中心新增保洁服务已通过审核/);
});

test('admin documentation uses Yuejia branding', () => {
  const readme = read('README.md');

  assert.match(readme, /# 悦家服务-管理端/);
  assert.doesNotMatch(readme, /云岚到家|云岚家政/);
  assert.match(read('docs/index.html'), /悦家服务管理端/);
  assert.match(read('docs/cover.md'), /悦家服务管理端/);
});

test('admin H5 entry uses the Yuejia browser icon', () => {
  const entry = read('index.html');

  assert.match(entry, /<link rel="icon" href="\/favicon\.svg" \/>/);
});
