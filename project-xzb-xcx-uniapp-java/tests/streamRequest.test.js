import test from 'node:test';
import assert from 'node:assert/strict';
import { streamRequest } from '../utils/streamRequest.js';

test('streams parsed events with authenticated SSE request options', () => {
  const previousUni = globalThis.uni;
  const events = [];
  let requestOptions;
  let chunkHandler;
  let aborted = false;
  const task = {
    onChunkReceived(handler) { chunkHandler = handler; },
    abort() { aborted = true; },
  };

  globalThis.uni = {
    getStorageSync(key) { return key === 'token' ? 'test-token' : undefined; },
    request(options) { requestOptions = options; return task; },
  };

  try {
    const returnedTask = streamRequest({
      url: '/aigc/consumer/assistant/sessions/s1/messages',
      data: { message: '保洁', cityCode: '110000' },
      onEvent: (event) => events.push(event),
    });
    chunkHandler({ data: new TextEncoder().encode('event: delta\ndata: {"text":"你好"}\n\n').buffer });
    returnedTask.abort();

    assert.equal(returnedTask, task);
    assert.equal(requestOptions.url, 'http://127.0.0.1:11500/aigc/consumer/assistant/sessions/s1/messages');
    assert.deepEqual(requestOptions.data, { message: '保洁', cityCode: '110000' });
    assert.equal(requestOptions.method, 'POST');
    assert.equal(requestOptions.enableChunked, true);
    assert.equal(requestOptions.header.Accept, 'text/event-stream');
    assert.equal(requestOptions.header.Authorization, 'test-token');
    assert.deepEqual(events, [{ type: 'delta', data: { text: '你好' } }]);
    assert.equal(aborted, true);
  } finally {
    globalThis.uni = previousUni;
  }
});

test('forwards request failures and completes the parser', () => {
  const previousUni = globalThis.uni;
  let requestOptions;
  const errors = [];
  let completed = 0;

  globalThis.uni = {
    getStorageSync() { return 'test-token'; },
    request(options) {
      requestOptions = options;
      return { onChunkReceived() {} };
    },
  };

  try {
    streamRequest({
      url: '/aigc/consumer/assistant/sessions/s1/messages',
      data: {},
      onError: (error) => errors.push(error),
      onComplete: () => { completed += 1; },
    });
    requestOptions.fail({ errMsg: 'request:fail' });
    requestOptions.complete();

    assert.deepEqual(errors, [{ errMsg: 'request:fail' }]);
    assert.equal(completed, 1);
  } finally {
    globalThis.uni = previousUni;
  }
});
