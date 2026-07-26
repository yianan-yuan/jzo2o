import test from 'node:test';
import assert from 'node:assert/strict';
import { streamRequest } from '../utils/streamRequest.js';
import { sendChatMessage } from '../pages/api/ai.js';

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

test('delivers status, recommendations, and done events in order', () => {
  const previousUni = globalThis.uni;
  const events = [];
  let chunkHandler;

  globalThis.uni = {
    getStorageSync() { return 'test-token'; },
    request() {
      return {
        onChunkReceived(handler) { chunkHandler = handler; },
      };
    },
  };

  try {
    streamRequest({
      url: '/aigc/consumer/assistant/sessions/s1/messages',
      data: {},
      onEvent: (event) => events.push(event),
    });
    const payload =
      'event: status\ndata: {"stage":"SEARCHING_SERVICES"}\n\n'
      + 'event: recommendations\ndata: [{"serveId":1,"serveItemName":"日常保洁","price":99,"priceUnit":"次","actionType":"SERVICE_DETAIL"}]\n\n'
      + 'event: done\ndata: {"stage":"RECOMMENDING","suggestedQuestions":["还有其他保洁吗？"]}\n\n';
    chunkHandler({ data: new TextEncoder().encode(payload).buffer });

    assert.deepEqual(events.map((event) => event.type), ['status', 'recommendations', 'done']);
    assert.equal(events[1].data[0].serveId, 1);
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

test('adapts the new AIGC session stream to the legacy chat response shape', async () => {
  const previousUni = globalThis.uni;
  const requests = [];
  let chunkHandler;

  globalThis.uni = {
    getStorageSync(key) {
      if (key === 'token') return 'test-token';
      if (key === 'city') return { cityCode: '110000' };
      return undefined;
    },
    request(options) {
      requests.push(options);
      if (requests.length === 1) {
        options.success({ data: { data: { sessionId: 'session-1' } } });
        return {};
      }
      return {
        onChunkReceived(handler) { chunkHandler = handler; },
      };
    },
  };

  try {
    const responsePromise = sendChatMessage({ message: 'cleaning please' });
    await Promise.resolve();
    chunkHandler({ data: new TextEncoder().encode(
      'event: delta\ndata: {"text":"Hello "}\n\n'
        + 'event: delta\ndata: {"text":"there"}\n\n'
        + 'event: recommendations\ndata: [{"serveId":1}]\n\n'
        + 'event: done\ndata: {"stage":"RECOMMENDING"}\n\n',
    ).buffer });
    requests[1].complete();

    assert.deepEqual(await responsePromise, {
      data: {
        data: {
          reply: 'Hello there',
          recommendations: [{ serveId: 1 }],
          stage: 'RECOMMENDING',
        },
      },
    });
    assert.equal(requests[0].url, 'http://127.0.0.1:11500/aigc/consumer/assistant/sessions');
    assert.equal(requests[1].url, 'http://127.0.0.1:11500/aigc/consumer/assistant/sessions/session-1/messages');
    assert.deepEqual(requests[1].data, { message: 'cleaning please', cityCode: '110000' });
  } finally {
    globalThis.uni = previousUni;
  }
});
