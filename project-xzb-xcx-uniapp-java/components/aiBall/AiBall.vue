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
      <image class="aiPortrait" src="/static/ai/yuejia-assistant.png" mode="aspectFill" />
      <view class="aiLabel">AI</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';

const t = ref(0);
const l = ref(0);
const aiTop = ref(400);
const aiLeft = ref(300);
const ai = ref();
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
  uni.navigateTo({
    url: '/pages/ai-chat/index',
  });
};
</script>

<style lang="scss" scoped>
.aiBall {
  position: fixed;
  z-index: 998;
  top: 36%;
  right: 10px;
  width: 112rpx;
  height: 124rpx;
  overflow: hidden;
  border: 3rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 32rpx;
  background: linear-gradient(160deg, #fff6ef 0%, #ffd6c1 100%);
  box-shadow: 0 10rpx 26rpx rgba(196, 83, 47, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;

  &:active {
    transform: scale(0.92);
  }
}

.aiPortrait {
  width: 100%;
  height: 100%;
  object-position: 50% 28%;
  position: relative;
  z-index: 1;
}

.aiLabel {
  position: absolute;
  right: 8rpx;
  bottom: 8rpx;
  z-index: 2;
  padding: 3rpx 8rpx;
  border-radius: 999rpx;
  background: var(--warm-primary, #f06c48);
  color: #fff;
  font-size: 18rpx;
  font-weight: 700;
}
</style>
