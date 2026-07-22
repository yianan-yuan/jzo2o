<template>
  <view class="aiChatPage">
    <NavBar title="AI助手" :isShowBack="true"></NavBar>
    <view class="chatContainer">
      <scroll-view
        class="messageList"
        scroll-y
        :scroll-top="scrollTopVal"
        scroll-with-animation
      >
        <view v-if="messages.length === 0" class="welcomeTip">
          <view class="welcomeIcon">🤖</view>
          <view class="welcomeText">欢迎来到云岚到家，请问需要什么帮助？</view>
        </view>
        <view
          v-for="(msg, index) in messages"
          :key="index"
          class="messageItem"
          :class="msg.role"
        >
          <view v-if="msg.role === 'assistant'" class="aiMsg">
            <view class="avatar aiAvatar">
              <text class="avatarIcon">🤖</text>
            </view>
            <view class="bubble aiBubble">{{ msg.content }}</view>
          </view>
          <view v-else class="userMsg">
            <view class="bubble userBubble">{{ msg.content }}</view>
            <view class="avatar userAvatar">
              <text class="avatarIcon">我</text>
            </view>
          </view>
        </view>
        <view v-if="loading" class="messageItem assistant">
          <view class="aiMsg">
            <view class="avatar aiAvatar">
              <text class="avatarIcon">🤖</text>
            </view>
            <view class="bubble aiBubble loadingBubble">
              <view class="dotFlashing">
                <view class="dot"></view>
                <view class="dot"></view>
                <view class="dot"></view>
              </view>
            </view>
          </view>
        </view>
        <view class="bottomPadding"></view>
      </scroll-view>
    </view>
    <view class="inputBar">
      <input
        class="chatInput"
        v-model="inputText"
        placeholder="输入您的问题..."
        confirm-type="send"
        @confirm="sendMessage"
      />
      <button
        class="sendBtn"
        :disabled="!inputText.trim() || loading"
        @click="sendMessage"
      >
        发送
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { sendChatMessage } from '../api/ai.js';

const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const scrollTopVal = ref(0);

// 进入页面默认显示 AI 欢迎语
onLoad(() => {
  messages.value.push({
    role: 'assistant',
    content: '欢迎来到云岚到家，请问需要什么帮助？',
  });
});

const sendMessage = async () => {
  const text = inputText.value.trim();
  if (!text || loading.value) return;

  // 添加用户消息
  messages.value.push({ role: 'user', content: text });
  inputText.value = '';
  loading.value = true;
  scrollToBottom();

  try {
    const res = await sendChatMessage({ message: text });
    const body = res && res.data;
    const reply = (body && (body.data || body.result || body)).reply
      || (body && body.reply)
      || '';
    if (reply) {
      messages.value.push({ role: 'assistant', content: reply });
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用，请稍后再试。' });
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，网络连接失败，请稍后再试。' });
  }
  loading.value = false;
  scrollToBottom();
};

const scrollToBottom = () => {
  nextTick(() => {
    scrollTopVal.value = scrollTopVal.value + 99999;
  });
};
</script>

<style lang="scss">
.aiChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #fff5f5 0%, #f7f8fa 100%);
}

.chatContainer {
  flex: 1;
  overflow: hidden;
  padding-top: 88rpx;
}

.messageList {
  height: 100%;
  padding: 24rpx 24rpx 0;
  box-sizing: border-box;
}

.welcomeTip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
}

.welcomeIcon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #F74346 0%, #FF7A7D 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(247, 67, 70, 0.25);
  margin-bottom: 30rpx;
}

.welcomeText {
  font-size: var(--font-size-14, 28rpx);
  color: var(--neutral-color-font, #888);
  text-align: center;
}

.messageItem {
  margin-bottom: 28rpx;
}

.aiMsg,
.userMsg {
  display: flex;
  align-items: flex-start;
}

.userMsg {
  justify-content: flex-end;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.avatarIcon {
  font-size: 40rpx;
  line-height: 1;
}

.aiAvatar {
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #EC4899 100%);
  margin-right: 16rpx;
}

.userAvatar {
  background: linear-gradient(135deg, #F74346 0%, #FF7A7D 100%);
  margin-left: 16rpx;
}

.bubble {
  max-width: 70%;
  padding: 20rpx 26rpx;
  border-radius: 20rpx;
  font-size: var(--font-size-14, 28rpx);
  line-height: 44rpx;
  word-break: break-all;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}

.aiBubble {
  background: #fff;
  border-top-left-radius: 6rpx;
  color: var(--neutral-color-main, #151515);
}

.userBubble {
  background: linear-gradient(135deg, #F74346 0%, #FF5C5F 100%);
  color: #fff;
  border-top-right-radius: 6rpx;
  box-shadow: 0 2rpx 12rpx rgba(247, 67, 70, 0.2);
}

.loadingBubble {
  padding: 24rpx 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dotFlashing {
  display: flex;
  align-items: center;

  .dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: var(--neutral-color-font2, #BDBDBD);
    margin: 0 6rpx;
    animation: dotFlashing 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: 0s;
    }
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes dotFlashing {
  0%, 80%, 100% {
    opacity: 0.2;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.bottomPadding {
  height: 120rpx;
}

.inputBar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 16rpx 20rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid var(--neutral-color-line, #F4F4F4);
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.chatInput {
  flex: 1;
  height: 72rpx;
  background: var(--neutral-color-background, #f5f5f5);
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: var(--font-size-14, 28rpx);
}

.sendBtn {
  width: 144rpx;
  height: 72rpx;
  background: var(--essential-color-red, #F74346);
  color: #fff;
  border-radius: 36rpx;
  margin-left: 20rpx;
  font-size: var(--font-size-14, 28rpx);
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  padding: 0;

  &[disabled] {
    opacity: 0.5;
  }
}
</style>
