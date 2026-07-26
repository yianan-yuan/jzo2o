import test from 'node:test';
import assert from 'node:assert/strict';
import { createSseParser } from '../utils/sseParser.js';

test('parses utf8 and sse frames split across chunks', () => {
  const events = [];
  const parser = createSseParser((event) => events.push(event));
  const bytes = new TextEncoder().encode(
    'event: delta\ndata: {"text":"你好"}\n\nevent: done\ndata: {"stage":"RECOMMENDING"}\n\n',
  );

  parser.push(bytes.slice(0, 31).buffer);
  parser.push(bytes.slice(31, 36).buffer);
  parser.push(bytes.slice(36).buffer);
  parser.finish();

  assert.deepEqual(events, [
    { type: 'delta', data: { text: '你好' } },
    { type: 'done', data: { stage: 'RECOMMENDING' } },
  ]);
});

test('preserves incomplete utf8 sequences without TextDecoder', () => {
  const originalTextDecoder = globalThis.TextDecoder;
  const events = [];
  const bytes = new TextEncoder().encode('event: delta\ndata: {"text":"你好"}\n\n');

  try {
    globalThis.TextDecoder = undefined;
    const parser = createSseParser((event) => events.push(event));
    const chineseOffset = bytes.indexOf(0xe4);
    parser.push(bytes.slice(0, chineseOffset + 1).buffer);
    parser.push(bytes.slice(chineseOffset + 1).buffer);
    parser.finish();
  } finally {
    globalThis.TextDecoder = originalTextDecoder;
  }

  assert.deepEqual(events, [{ type: 'delta', data: { text: '你好' } }]);
});

test('parses multiple event types delivered in one chunk', () => {
  const events = [];
  const parser = createSseParser((event) => events.push(event));

  parser.push(new TextEncoder().encode(
    'event: status\ndata: {"stage":"GENERATING"}\n\n'
      + 'event: error\ndata: {"code":"AIGC_MODEL_UNAVAILABLE","retryable":true}\n\n',
  ).buffer);
  parser.finish();

  assert.deepEqual(events, [
    { type: 'status', data: { stage: 'GENERATING' } },
    { type: 'error', data: { code: 'AIGC_MODEL_UNAVAILABLE', retryable: true } },
  ]);
});
