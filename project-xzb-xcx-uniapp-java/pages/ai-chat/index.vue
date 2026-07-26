<template>
  <view class="aiChatPage">
    <NavBar title="AI助手" :isShowBack="true" />
    <view class="chatContainer">
      <scroll-view class="messageList" scroll-y :scroll-top="scrollTopVal" scroll-with-animation>
        <view v-for="(msg, index) in messages" :key="index" class="messageItem" :class="msg.role">
          <view v-if="msg.role === 'assistant'" class="aiMsg">
            <view class="avatar aiAvatar">🤖</view>
            <view class="assistantContent">
              <view class="bubble aiBubble">{{ msg.content }}</view>
              <view v-if="msg.stage" class="streamStage">{{ formatStage(msg.stage) }}</view>
              <view v-if="msg.error" class="streamError">{{ formatError(msg.error) }}</view>
              <view v-if="msg.recommendations && msg.recommendations.length" class="recommendations">
                <view v-for="card in msg.recommendations.slice(0, 3)" :key="card.serveId" class="recommendationCard" @click="openRecommendation(card)">
                  <view class="recommendationName">{{ card.serveItemName }}</view>
                  <view class="recommendationPrice">¥{{ card.price }}{{ card.priceUnit ? `/${card.priceUnit}` : '' }}</view>
                </view>
              </view>
            </view>
          </view>
          <view v-else class="userMsg">
            <view class="bubble userBubble">{{ msg.content }}</view>
            <view class="avatar userAvatar">我</view>
          </view>
        </view>
        <view v-if="loading" class="messageItem assistant"><view class="aiMsg"><view class="avatar aiAvatar">🤖</view><view class="bubble aiBubble loadingBubble">AI 正在思考…</view></view></view>
        <view v-if="suggestedQuestions.length" class="suggestedQuestions">
          <view v-for="question in suggestedQuestions" :key="question" class="questionChip" @click="askSuggestedQuestion(question)">{{ question }}</view>
        </view>
        <view class="bottomPadding" />
      </scroll-view>
    </view>
    <view class="inputBar">
      <input class="chatInput" v-model="inputText" placeholder="输入您的问题..." confirm-type="send" @confirm="sendMessage" />
      <button class="sendBtn" :disabled="!inputText.trim() || loading" @click="sendMessage">发送</button>
    </view>
  </view>
</template>

<script setup>
import { nextTick, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { createAiSession, streamAiMessage } from '../api/ai.js';

const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const scrollTopVal = ref(0);
const sessionId = ref('');
const activeTask = ref(null);
const streamStage = ref('');
const suggestedQuestions = ref([]);
const disposed = ref(false);

onLoad(() => {
  disposed.value = false;
  messages.value.push({ role: 'assistant', content: '欢迎来到云岚到家，请问需要什么帮助？' });
});

const showToast = (title) => uni.showToast({ title, icon: 'none' });
const unwrapResponse = (response) => {
  const body = response && response.data;
  return (body && (body.data || body.result || body)) || {};
};
const ensureSession = async () => {
  if (sessionId.value) return sessionId.value;
  const response = await createAiSession();
  sessionId.value = unwrapResponse(response).sessionId || '';
  if (!sessionId.value) throw new Error('AIGC session was not created');
  return sessionId.value;
};
const redirectToLogin = () => uni.navigateTo({ url: '/pages/login/index?isLogin=1&reason=使用AI助手需要先登录' });
const isSessionExpired = (error) => [401, 404].includes(Number(error?.statusCode || error?.code));
const renewExpiredSession = (error) => {
  if (!isSessionExpired(error)) return;
  sessionId.value = '';
  showToast('会话已过期，已为您创建新会话，请重新发送问题');
  ensureSession().catch(() => {});
};

const sendMessage = async () => {
  const text = inputText.value.trim();
  if (!text || loading.value || activeTask.value) return;
  if (!uni.getStorageSync('token')) {
    redirectToLogin();
    return;
  }
  const city = uni.getStorageSync('city');
  if (!city || !city.cityCode) {
    showToast('请先选择服务城市');
    uni.navigateTo({ url: '/pages/city/index' });
    return;
  }

  messages.value.push({ role: 'user', content: text });
  inputText.value = '';
  loading.value = true;
  streamStage.value = '';
  suggestedQuestions.value = [];
  scrollToBottom();
  try {
    const currentSessionId = await ensureSession();
    if (disposed.value) {
      loading.value = false;
      return;
    }
    const assistantMessage = { role: 'assistant', content: '', recommendations: [], stage: '', error: null };
    messages.value.push(assistantMessage);
    activeTask.value = streamAiMessage(currentSessionId, { message: text, cityCode: city.cityCode }, {
      onEvent(event) {
        if (event.type === 'status') {
          streamStage.value = event.data.stage || '';
          assistantMessage.stage = streamStage.value;
        }
        if (event.type === 'delta') assistantMessage.content += event.data.text || '';
        if (event.type === 'recommendations') assistantMessage.recommendations = event.data || [];
        if (event.type === 'done') {
          streamStage.value = event.data.stage || streamStage.value;
          assistantMessage.stage = streamStage.value;
          suggestedQuestions.value = event.data.suggestedQuestions || [];
        }
        if (event.type === 'error') {
          assistantMessage.error = event.data;
          renewExpiredSession(event.data);
        }
        scrollToBottom();
      },
      onComplete() {
        loading.value = false;
        activeTask.value = null;
        scrollToBottom();
      },
      onError(error) {
        assistantMessage.error = error;
        renewExpiredSession(error);
        loading.value = false;
        activeTask.value = null;
        scrollToBottom();
      },
    });
  } catch (error) {
    messages.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用，请稍后再试。', error });
    loading.value = false;
    activeTask.value = null;
    scrollToBottom();
  }
};

const askSuggestedQuestion = (question) => {
  if (loading.value || activeTask.value) return;
  inputText.value = question;
  sendMessage();
};
const openRecommendation = (card) => {
  if (!card || !card.serveId) return;
  uni.navigateTo({ url: `/pages/service/components/airMaintenance?id=${card.serveId}&title=${encodeURIComponent(card.serveItemName || '')}` });
};
const formatStage = (stage) => ({ SEARCHING_SERVICES: '正在为您查找服务…', RECOMMENDING: '已为您找到推荐服务' }[stage] || stage);
const formatError = (error) => error?.message || error?.msg || 'AI服务暂时不可用，请稍后再试。';
const scrollToBottom = () => nextTick(() => { scrollTopVal.value += 99999; });

onUnload(() => {
  disposed.value = true;
  activeTask.value?.abort();
});
</script>

<style lang="scss">
.aiChatPage { height: 100vh; display: flex; flex-direction: column; background: linear-gradient(180deg, #fff5f5 0%, #f7f8fa 100%); }
.chatContainer { flex: 1; overflow: hidden; padding-top: 88rpx; }
.messageList { height: 100%; padding: 24rpx 24rpx 0; box-sizing: border-box; }
.messageItem { margin-bottom: 28rpx; }
.aiMsg, .userMsg { display: flex; align-items: flex-start; }
.userMsg { justify-content: flex-end; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.aiAvatar { background: linear-gradient(135deg, #6366F1, #EC4899); margin-right: 16rpx; }
.userAvatar { background: linear-gradient(135deg, #F74346, #FF7A7D); margin-left: 16rpx; }
.assistantContent { max-width: 70%; }
.bubble { padding: 20rpx 26rpx; border-radius: 20rpx; font-size: var(--font-size-14, 28rpx); line-height: 44rpx; word-break: break-all; box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, .06); }
.aiBubble { background: #fff; border-top-left-radius: 6rpx; color: var(--neutral-color-main, #151515); }
.userBubble { max-width: 70%; background: linear-gradient(135deg, #F74346, #FF5C5F); color: #fff; border-top-right-radius: 6rpx; }
.loadingBubble { padding: 24rpx 40rpx; color: #888; }
.streamStage, .streamError { margin-top: 12rpx; font-size: 24rpx; color: #888; }
.streamError { color: #F74346; }
.recommendations { margin-top: 16rpx; }
.recommendationCard { background: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 12rpx; box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, .06); }
.recommendationName { font-size: 28rpx; color: #151515; }
.recommendationPrice { margin-top: 8rpx; color: #F74346; font-size: 26rpx; }
.suggestedQuestions { display: flex; flex-wrap: wrap; gap: 12rpx; margin: 0 0 24rpx 88rpx; }
.questionChip { padding: 12rpx 20rpx; border-radius: 28rpx; background: #fff; color: #6366F1; font-size: 24rpx; }
.bottomPadding { height: 120rpx; }
.inputBar { display: flex; align-items: center; background: #fff; padding: 16rpx 20rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #F4F4F4; position: fixed; bottom: 0; left: 0; right: 0; z-index: 100; }
.chatInput { flex: 1; height: 72rpx; background: #f5f5f5; border-radius: 36rpx; padding: 0 30rpx; font-size: 28rpx; }
.sendBtn { width: 144rpx; height: 72rpx; background: #F74346; color: #fff; border-radius: 36rpx; margin-left: 20rpx; font-size: 28rpx; display: flex; align-items: center; justify-content: center; border: none; padding: 0; }
.sendBtn[disabled] { opacity: .5; }
</style>
