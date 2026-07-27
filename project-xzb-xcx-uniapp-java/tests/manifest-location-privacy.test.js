import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('declares getLocation as a required WeChat private API', async () => {
  const manifest = await readFile(new URL('../manifest.json', import.meta.url), 'utf8');

  assert.match(manifest, /"requiredPrivateInfos"\s*:\s*\[\s*"getLocation"\s*\]/);
});
