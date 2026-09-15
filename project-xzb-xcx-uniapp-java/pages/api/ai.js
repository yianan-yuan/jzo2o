// 发送聊天消息 - 走 Java 后端（gateway），由后端调用 Ollama
import { baseUrl } from '../../utils/env.js';
import { streamRequest } from '../../utils/streamRequest.js';

const getAuthorization = () => uni.getStorageSync('token');

export const createAiSession = () => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}/aigc/consumer/assistant/sessions`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json;charset=UTF-8',
        Authorization: getAuthorization(),
      },
      success: (res) => resolve(res),
      fail: (err) => reject(err),
    });
  });
};

export const streamAiMessage = (sessionId, params, handlers = {}) => streamRequest({
  url: `/aigc/consumer/assistant/sessions/${sessionId}/messages`,
  data: params,
  onEvent: handlers.onEvent,
  onError: handlers.onError,
  onComplete: handlers.onComplete,
});

const unwrapResponse = (response) => {
  const body = response && response.data;
  return (body && (body.data || body.result || body)) || {};
};

export const sendChatMessage = (params) => new Promise((resolve, reject) => {
  createAiSession().then((sessionResponse) => {
    const sessionId = unwrapResponse(sessionResponse).sessionId;
    if (!sessionId) {
      reject(new Error('AIGC session was not created'));
      return;
    }

    const city = uni.getStorageSync('city');
    const requestParams = {
      ...params,
      cityCode: params.cityCode || (city && city.cityCode),
    };
    let reply = '';
    let recommendations;
    let stage;
    let streamError;
    let settled = false;
    const settle = (callback, value) => {
      if (settled) return;
      settled = true;
      callback(value);
    };

    streamAiMessage(sessionId, requestParams, {
      onEvent(event) {
        if (event.type === 'delta') reply += event.data.text || '';
        if (event.type === 'recommendations') recommendations = event.data;
        if (event.type === 'done') stage = event.data.stage;
        if (event.type === 'error') streamError = event.data;
      },
      onError(error) {
        settle(reject, error);
      },
      onComplete() {
        const data = { reply, recommendations, stage };
        if (streamError) data.error = streamError;
        settle(resolve, {
          data: {
            data,
          },
        });
      },
    });
  }).catch(reject);
});
