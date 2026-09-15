import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

test('worker App is source-only and excludes local artifacts', () => {
  const ignore = readFileSync('../.gitignore', 'utf8');
  const packageJson = JSON.parse(readFileSync('package.json', 'utf8'));
  assert.equal(existsSync('package.json'), true);
  assert.equal(existsSync('node_modules'), false);
  assert.equal(packageJson.devDependencies?.vite, undefined);
  assert.equal(packageJson.devDependencies?.['@dcloudio/vite-plugin-uni'], undefined);
  assert.match(ignore, /node_modules/);
  assert.match(ignore, /unpackage/);
  assert.match(ignore, /project\.private\.config\.json/);
  assert.equal(existsSync('.git'), false);
});
