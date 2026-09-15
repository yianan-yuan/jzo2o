<template>
  <view class="empty">
    <template v-if="variant === 'evaluation'">
      <view class="evaluationEmptyVisual">
        <view class="evaluationBubble bubbleBack"></view>
        <view class="evaluationBubble bubbleFront"><text>★</text></view>
        <view class="evaluationStars">★ ★ ★ ★ ★</view>
      </view>
      <view class="content">暂无评价记录，完成服务后，客户的评价会出现在这里</view>
    </template>
    <template v-else-if="variant === 'history'">
      <view class="historyEmptyVisual">
        <view class="historyHalo haloOne"></view>
        <view class="historyHalo haloTwo"></view>
        <view class="historyArchive">
          <view class="archiveLine"></view>
          <view class="archiveLine archiveLineShort"></view>
        </view>
        <view class="historyCheck">✓</view>
      </view>
      <view class="content">暂无历史订单，完成服务后会显示在这里</view>
    </template>
    <template v-else>
    <view class="workerEmptyRadar">
      <view class="radarRing ringOuter"></view>
      <view class="radarRing ringMiddle"></view>
      <view class="radarRing ringInner"></view>
      <view class="radarSignal"></view>
      <view class="toolbox"><text>✓</text></view>
    </view>
    <view v-if="variant === 'worker-order'" class="content">暂无{{ emptyLabel }}订单</view>
    <view v-else-if="canPickUp" class="content">暂时没有符合条件的订单，保持在线，机会很快到来</view>
    <view v-else class="content"> 当前未开启接单设置，无法进行抢单哦～ </view>
    </template>
  </view>
</template>

<script setup>
const props = defineProps({
  canPickUp: {
    type: Boolean,
    default: true,
  },
  variant: {
    type: String,
    default: 'order',
  },
  emptyLabel: {
    type: String,
    default: '当前分类',
  },
});
</script>
<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 104rpx 40rpx 80rpx;

  .content {
    max-width: 460rpx;
    font-size: 24rpx;
    line-height: 40rpx;
    color: var(--warm-text-muted);
    text-align: center;
  }
}

.workerEmptyRadar {
  position: relative;
  width: 286rpx;
  height: 230rpx;
  margin-bottom: 28rpx;
}

.radarRing {
  position: absolute;
  right: 50%;
  bottom: 18rpx;
  width: 180rpx;
  height: 72rpx;
  border: 4rpx solid rgba(240, 108, 72, 0.16);
  border-radius: 50%;
  transform: translateX(50%);
}

.ringOuter {
  width: 260rpx;
  height: 104rpx;
  border-color: rgba(240, 108, 72, 0.12);
}

.ringMiddle {
  bottom: 34rpx;
  width: 214rpx;
  height: 86rpx;
}

.ringInner {
  bottom: 50rpx;
  width: 158rpx;
  height: 62rpx;
  border-color: rgba(240, 108, 72, 0.24);
}

.radarSignal {
  position: absolute;
  top: 16rpx;
  right: 44rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50% 50% 50% 0;
  background: #f7a187;
  box-shadow: 0 0 0 14rpx rgba(247, 161, 135, 0.14);
  transform: rotate(-45deg);
}

.toolbox {
  position: absolute;
  bottom: 54rpx;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 114rpx;
  height: 88rpx;
  color: #fff;
  font-size: 48rpx;
  font-weight: 700;
  background: linear-gradient(145deg, #f89b78, var(--warm-primary));
  border-radius: 18rpx;
  box-shadow: 0 18rpx 30rpx rgba(240, 108, 72, 0.22);
  transform: translateX(-50%);

  &::before {
    position: absolute;
    top: -26rpx;
    left: 36rpx;
    width: 42rpx;
    height: 28rpx;
    content: '';
    border: 10rpx solid #f58a69;
    border-bottom: 0;
    border-radius: 18rpx 18rpx 0 0;
  }
}
.evaluationEmptyVisual {
  position: relative;
  width: 250rpx;
  height: 210rpx;
  margin-bottom: 28rpx;
}

.evaluationBubble {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 42rpx;
}

.bubbleBack {
  top: 22rpx;
  right: 22rpx;
  width: 116rpx;
  height: 88rpx;
  background: rgba(247, 161, 135, 0.18);
}

.bubbleFront {
  top: 52rpx;
  left: 32rpx;
  width: 142rpx;
  height: 112rpx;
  background: linear-gradient(145deg, #f89b78, var(--warm-primary));
  box-shadow: 0 18rpx 30rpx rgba(240, 108, 72, 0.18);
  color: #fff;
  font-size: 52rpx;
}

.evaluationStars {
  position: absolute;
  right: 20rpx;
  bottom: 0;
  color: #f2a12d;
  font-size: 25rpx;
  letter-spacing: 4rpx;
}

.historyEmptyVisual {
  position: relative;
  width: 254rpx;
  height: 214rpx;
  margin-bottom: 28rpx;
}

.historyHalo {
  position: absolute;
  left: 50%;
  border: 4rpx solid rgba(240, 108, 72, 0.14);
  border-radius: 50%;
  transform: translateX(-50%);
}

.haloOne {
  bottom: 12rpx;
  width: 232rpx;
  height: 82rpx;
}

.haloTwo {
  bottom: 30rpx;
  width: 184rpx;
  height: 58rpx;
  border-color: rgba(240, 108, 72, 0.22);
}

.historyArchive {
  position: absolute;
  bottom: 46rpx;
  left: 50%;
  box-sizing: border-box;
  width: 128rpx;
  height: 112rpx;
  padding: 30rpx 26rpx;
  background: linear-gradient(145deg, #fbd1c1, #f89570);
  border-radius: 18rpx 18rpx 22rpx 22rpx;
  box-shadow: 0 18rpx 30rpx rgba(240, 108, 72, 0.18);
  transform: translateX(-50%);

  &::before {
    position: absolute;
    top: -22rpx;
    left: 30rpx;
    width: 68rpx;
    height: 26rpx;
    content: '';
    background: #f7ae91;
    border-radius: 16rpx 16rpx 0 0;
  }
}

.archiveLine {
  width: 76rpx;
  height: 8rpx;
  margin-bottom: 14rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8rpx;
}

.archiveLineShort {
  width: 48rpx;
  margin-bottom: 0;
}

.historyCheck {
  position: absolute;
  top: 22rpx;
  right: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52rpx;
  height: 52rpx;
  color: #fff;
  font-size: 34rpx;
  font-weight: 700;
  background: var(--warm-primary);
  border: 6rpx solid #fff7f2;
  border-radius: 50%;
}
</style>
