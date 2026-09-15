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

test('worker route containers receive the warm surface system', () => {
  assert.match(app, /\.navFrame[\s\S]*var\(--warm-page-bg\)/);
  assert.match(app, /\.item[\s\S]*var\(--warm-surface\)/);
});

test('worker business actions and state labels use the warm system', () => {
  assert.match(app, /\.btn[\s\S]*var\(--warm-primary\)/);
  assert.match(app, /\.status[\s\S]*var\(--warm-primary-soft\)/);
});
