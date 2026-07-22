// 发送聊天消息 - 走 Java 后端（gateway），由后端调用 Ollama
export const sendChatMessage = (params) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'http://localhost:11500/customer/consumer/ai/chat',
      data: params,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      success: (res) => resolve(res),
      fail: (err) => reject(err),
    });
  });
};
