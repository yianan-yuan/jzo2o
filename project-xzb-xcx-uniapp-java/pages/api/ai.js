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
