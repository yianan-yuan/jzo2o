import { baseUrl } from './env.js';
import { createSseParser } from './sseParser.js';

const getAuthorization = () => (typeof globalThis.uni?.getStorageSync === 'function'
  ? globalThis.uni.getStorageSync('token')
  : '');

export const streamRequest = ({ url, data, onEvent, onError, onComplete }) => {
  const parser = createSseParser(onEvent || (() => {}));
  const requestTask = globalThis.uni.request({
    url: `${baseUrl}${url}`,
    data,
    method: 'POST',
    enableChunked: true,
    header: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json;charset=UTF-8',
      Authorization: getAuthorization(),
    },
    fail(error) { if (onError) onError(error); },
    complete(response) {
      parser.finish();
      if (onComplete) onComplete(response);
    },
  });
  if (typeof requestTask.onChunkReceived === 'function') {
    requestTask.onChunkReceived((chunk) => parser.push(chunk.data));
  }
  return requestTask;
};
