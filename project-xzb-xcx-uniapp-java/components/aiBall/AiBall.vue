<template>
  <view>
    <view
      v-show="show"
      ref="ai"
      class="aiBall"
      :style="{ top: `${aiTop}px`, left: `${aiLeft}px` }"
      @touchstart="downAi"
      @touchmove.prevent="moveAi"
      @click="handleClickAi"
    >
      <view class="aiIcon">🤖</view>
    </view>
    <uni-popup
      ref="popup"
      type="center"
      background-color="transparent"
      mask-background-color="rgba(0,0,0,0.6)"
      @maskClick="handleClose"
    >
      <view class="uniBody">
        <view class="body">
          <view class="bodyIcon">🤖</view>
          <view class="bodyTitle">AI 智能助手</view>
          <view class="bodyDesc">基于 Qwen3 大模型，随时随地为你解答问题</view>
        </view>
        <view class="button" @click="handleToAiChat">立即体验AI助手</view>
        <view class="close" @click="handleClose">
          <image src="/static/guanbi@2x.png" mode="scaleToFill" />
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref } from 'vue';

const t = ref(0);
const l = ref(0);
const aiTop = ref(400);
const aiLeft = ref(300);
const ai = ref();
const popup = ref();
const show = ref(true);

const downAi = (e) => {
  e.preventDefault();
  t.value = e.touches[0].clientY - aiTop.value;
  l.value = e.touches[0].clientX - aiLeft.value;
};

const moveAi = (e) => {
  aiTop.value = e.touches[0].clientY - t.value;
  aiLeft.value = e.touches[0].clientX - l.value;
};

const handleClickAi = () => {
  popup.value.open();
  show.value = false;
};

const handleToAiChat = () => {
  if (!uni.getStorageSync('token')) {
    uni.navigateTo({
      url: '/pages/login/index?isLogin=1&reason=使用AI助手需要先登录',
    });
    return;
  }
  uni.navigateTo({
    url: '/pages/ai-chat/index',
  });
};

const handleClose = () => {
  popup.value.close();
  show.value = true;
};
</script>

<style lang="scss" scoped>
.aiBall {
  position: fixed;
  z-index: 998;
  top: 36%;
  right: 10px;
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #EC4899 100%);
  box-shadow: 0 8rpx 24rpx rgba(99, 102, 241, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;

  &::after {
    content: '';
    position: absolute;
    top: 8rpx;
    left: 12rpx;
    right: 30rpx;
    height: 24rpx;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0) 100%);
    border-radius: 20rpx;
    pointer-events: none;
  }

  &:active {
    transform: scale(0.92);
  }
}

.aiIcon {
  color: #fff;
  font-size: 48rpx;
  line-height: 1;
  filter: drop-shadow(0 2rpx 4rpx rgba(0, 0, 0, 0.2));
  position: relative;
  z-index: 1;
}

.uniBody {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.body {
  width: 478rpx;
  height: 400rpx;
  background: #fff;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;

  .bodyIcon {
    font-size: 100rpx;
    margin-bottom: 30rpx;
  }

  .bodyTitle {
    font-size: 36rpx;
    font-weight: bold;
    color: var(--neutral-color-main, #151515);
    margin-bottom: 16rpx;
  }

  .bodyDesc {
    font-size: var(--font-size-13, 26rpx);
    color: var(--neutral-color-font, #888);
    text-align: center;
    line-height: 40rpx;
  }
}

.button {
  width: 478rpx;
  height: 88rpx;
  background: #d83332;
  border-radius: 46rpx;
  color: #ffcb6b;
  font-size: var(--font-size-16, 32rpx);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 30rpx;
}

.close {
  width: 56rpx;
  height: 56rpx;
  padding: 5rpx;
  margin-top: 30rpx;

  image {
    width: 100%;
    height: 100%;
    display: block;
  }
}
</style>
