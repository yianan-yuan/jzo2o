<template>
  <view class="aiChatPage">
    <NavBar title="AI助手" :isShowBack="true" />
    <view class="chatContainer">
      <scroll-view class="messageList" scroll-y :scroll-top="scrollTopVal" scroll-with-animation>
        <view v-if="!hasUserMessage" class="chatWelcomeCard">
          <image class="welcomeAvatar" src="/static/ai/yuejia-assistant.png" mode="aspectFit" />
          <view class="welcomeCopy">
            <view class="welcomeEyebrow">欢迎来到悦家服务 · 智能服务顾问</view>
            <view class="welcomeTitle">你好，我是悦悦</view>
            <view class="welcomeText">告诉我你的需求，我会帮你找到合适的上门服务。</view>
          </view>
        </view>
        <view v-if="!hasUserMessage" class="quickQuestionSection">
          <view class="quickQuestionTitle">你可以这样问我</view>
          <view class="quickQuestions">
            <view
              v-for="question in quickQuestions"
              :key="question"
              class="quickQuestion"
              @click="askSuggestedQuestion(question)"
            >
              {{ question }}
            </view>
          </view>
        </view>
        <view v-for="(msg, index) in messages" :key="index" class="messageItem" :class="msg.role">
          <view v-if="msg.role === 'assistant'" class="aiMsg">
            <image class="avatar aiAvatar" src="/static/ai/yuejia-assistant.png" mode="aspectFit" />
            <view class="assistantContent">
              <view class="bubble aiBubble">{{ displayAssistantContent(msg) }}</view>
              <view v-if="msg.stage" class="streamStage">{{ formatStage(msg.stage) }}</view>
              <view v-if="msg.error" class="streamError">{{ formatError(msg.error) }}</view>
              <view v-if="msg.recommendations && msg.recommendations.length" class="recommendations">
                <view v-for="card in msg.recommendations.slice(0, 3)" :key="card.serveId" class="recommendationCard" @click="openRecommendation(card)">
                  <view class="recommendationName">{{ card.serveItemName }}</view>
                  <view class="recommendationPrice">￥{{ card.price }}{{ card.priceUnit ? `/${card.priceUnit}` : '' }}</view>
                </view>
              </view>
            </view>
          </view>
          <view v-else class="userMsg">
            <view class="bubble userBubble">{{ msg.content }}</view>
            <view class="avatar userAvatar">我</view>
          </view>
        </view>
        <view v-if="suggestedQuestions.length" class="suggestedQuestions">
          <view v-for="question in suggestedQuestions" :key="question" class="questionChip" @click="askSuggestedQuestion(question)">{{ question }}</view>
        </view>
        <view class="bottomPadding" />
      </scroll-view>
    </view>
    <view class="inputBar">
      <input class="chatInput" v-model="inputText" placeholder="说说你需要什么服务…" confirm-type="send" @confirm="sendMessage" />
      <button class="sendBtn" :disabled="!inputText.trim() || loading" @click="sendMessage">发送</button>
    </view>
    <UniFooter :pagePath="'/pages/ai-chat/index'" />
  </view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import { createAiSession, streamAiMessage } from '../api/ai.js';
import { appendAssistantDelta, createPendingAssistantMessage, displayAssistantContent } from './chat-message-state.js';

const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const scrollTopVal = ref(0);
const sessionId = ref('');
const activeTask = ref(null);
const streamStage = ref('');
const suggestedQuestions = ref([]);
const disposed = ref(false);
const quickQuestions = ['想预约日常保洁', '空调维修怎么收费？', '帮我推荐合适服务'];
const hasUserMessage = computed(() => messages.value.some((message) => message.role === 'user'));

onLoad(() => {
  disposed.value = false;
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
    const assistantMessage = createPendingAssistantMessage();
    messages.value.push(assistantMessage);
    activeTask.value = streamAiMessage(currentSessionId, { message: text, cityCode: city.cityCode }, {
      onEvent(event) {
        if (event.type === 'status') {
          streamStage.value = event.data.stage || '';
          assistantMessage.stage = streamStage.value;
        }
        if (event.type === 'delta') appendAssistantDelta(assistantMessage, event.data.text);
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
.aiChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #fff4ed 0%, #fff9f6 28%, #fff8f4 100%);
}

.chatContainer { flex: 1; overflow: hidden; padding-top: 88rpx; }
.messageList { height: 100%; padding: 24rpx 24rpx 0; box-sizing: border-box; }
.chatWelcomeCard { display: flex; align-items: center; padding: 28rpx; border-radius: 30rpx; background: linear-gradient(135deg, #f06c48, #ff9a72); box-shadow: 0 16rpx 34rpx rgba(240, 108, 72, .22); color: #fff; }
.welcomeAvatar { width: 112rpx; height: 112rpx; padding: 10rpx; box-sizing: border-box; border-radius: 32rpx; background: rgba(255, 255, 255, .9); margin-right: 20rpx; }
.welcomeEyebrow { font-size: 22rpx; opacity: .82; }
.welcomeTitle { margin-top: 4rpx; font-size: 34rpx; font-weight: 700; }
.welcomeText { margin-top: 8rpx; font-size: 24rpx; line-height: 36rpx; }
.quickQuestionSection { margin: 30rpx 0 36rpx; }
.quickQuestionTitle { margin-bottom: 16rpx; color: var(--warm-text, #2c2522); font-size: 28rpx; font-weight: 700; }
.quickQuestions, .suggestedQuestions { display: flex; flex-wrap: wrap; gap: 16rpx; }
.quickQuestion, .questionChip { padding: 16rpx 20rpx; border: 1rpx solid #f7d8cc; border-radius: 999rpx; background: #fff; color: var(--warm-primary, #f06c48); font-size: 24rpx; box-shadow: 0 8rpx 20rpx rgba(141, 77, 49, .06); }
.messageItem { margin-bottom: 28rpx; }
.aiMsg, .userMsg { display: flex; align-items: flex-start; }
.userMsg { justify-content: flex-end; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 22rpx; flex-shrink: 0; }
.aiAvatar { margin-right: 16rpx; padding: 6rpx; box-sizing: border-box; background: #fff0e9; }
.userAvatar { display: flex; align-items: center; justify-content: center; margin-left: 16rpx; background: linear-gradient(135deg, #f06c48, #ff9670); color: #fff; font-size: 26rpx; }
.assistantContent { max-width: 72%; }
.bubble { padding: 20rpx 24rpx; border-radius: 24rpx; font-size: 28rpx; line-height: 44rpx; word-break: break-all; box-shadow: 0 6rpx 20rpx rgba(141, 77, 49, .08); }
.aiBubble { border-top-left-radius: 8rpx; background: #fff; color: var(--warm-text, #2c2522); }
.userBubble { max-width: 72%; border-top-right-radius: 8rpx; background: linear-gradient(135deg, #f06c48, #ff8a62); color: #fff; }
.streamStage, .streamError { margin-top: 12rpx; font-size: 24rpx; color: var(--warm-text-muted, #7e746f); }
.streamError { color: #dc4b32; }
.recommendations { margin-top: 16rpx; }
.recommendationCard { padding: 20rpx; margin-bottom: 12rpx; border: 1rpx solid #f5e2d9; border-radius: 18rpx; background: #fffaf7; box-shadow: 0 6rpx 18rpx rgba(141, 77, 49, .06); }
.recommendationName { font-size: 28rpx; color: var(--warm-text, #2c2522); }
.recommendationPrice { margin-top: 8rpx; color: var(--warm-primary, #f06c48); font-size: 26rpx; font-weight: 700; }
.suggestedQuestions { margin: 0 0 24rpx 88rpx; }
.bottomPadding { height: 300rpx; }
.inputBar { display: flex; align-items: center; position: fixed; bottom: 112rpx; left: 0; right: 0; z-index: 100; padding: 16rpx 20rpx; border-top: 1rpx solid #f2e8e2; background: rgba(255, 255, 255, .97); box-shadow: 0 -8rpx 24rpx rgba(141, 77, 49, .05); }
.chatInput { flex: 1; height: 76rpx; padding: 0 26rpx; border-radius: 38rpx; background: #fff4ef; color: var(--warm-text, #2c2522); font-size: 27rpx; }
.sendBtn { width: 132rpx; height: 76rpx; margin-left: 16rpx; padding: 0; border: none; border-radius: 38rpx; background: var(--warm-primary, #f06c48); color: #fff; font-size: 26rpx; line-height: 76rpx; }
.sendBtn[disabled] { opacity: .45; }
</style>
