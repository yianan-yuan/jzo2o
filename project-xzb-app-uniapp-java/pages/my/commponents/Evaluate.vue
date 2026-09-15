<template>
  <view class="boxBg headTop">
    <view class="headItem score">
      <view class="rateBox">
        <view class="rateIcon">{{ summary.hasEvaluation ? '★' : '✦' }}</view>
        <view class="rateContent">
          <view class="scoreContent">{{ summary.displayRate || '暂无评价' }}</view>
          <view class="label">好评率</view>
          <view class="hint">{{ summary.hasEvaluation ? '来自客户已提交的好评与差评' : '完成更多服务后，客户评价会在这里沉淀' }}</view>
        </view>
        <view class="rateBadge">{{ summary.hasEvaluation ? '服务反馈' : '等待首评' }}</view>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import { getEvaluationSummary } from '@/pages/api/order.js';
const summary = ref({ hasEvaluation: false, displayRate: '暂无评价' });
onMounted(async () => {
  try {
    const res = await getEvaluationSummary();
    summary.value = res?.data || res || summary.value;
  } catch (_) { summary.value = { hasEvaluation: false, displayRate: '暂无评价' }; }
});
</script>
<style src="./../index.scss" lang="scss" scoped></style>
<style lang="scss" scoped>
.headTop{padding:0!important;background:transparent!important}.headItem.score{min-height:164rpx!important;padding:0!important;border-radius:28rpx!important;overflow:hidden;background:linear-gradient(135deg,#fff 0%,#fff8f4 100%)!important;box-shadow:0 16rpx 38rpx rgba(175,99,57,.09)!important}.rateBox{box-sizing:border-box;width:100%;min-height:164rpx;padding:26rpx 30rpx;display:flex;align-items:center;gap:20rpx;position:relative}.rateIcon{width:80rpx;height:80rpx;display:flex;align-items:center;justify-content:center;border-radius:24rpx;background:linear-gradient(145deg,#ff9b73,#f15d40);box-shadow:0 10rpx 20rpx rgba(240,99,65,.25);color:#fff;font-size:42rpx}.rateContent{flex:1}.scoreContent{font-size:40rpx!important;line-height:1.1;color:#342720!important;font-weight:700}.label{margin-top:8rpx;font-size:25rpx!important;color:#67564f!important}.hint{margin-top:8rpx;font-size:20rpx;color:#a29087;white-space:nowrap}.rateBadge{position:absolute;right:26rpx;top:22rpx;padding:7rpx 14rpx;border-radius:24rpx;background:#fff0eb;color:#e46b4b;font-size:20rpx}
</style>
