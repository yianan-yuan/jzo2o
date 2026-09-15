<template>
  <view class="homeList"><view class="card" v-for="item in data" :key="item.id" @click="showDetail(item)">
    <view class="header"><view><text class="type" :class="item.evaluationType === 1 ? 'good' : 'bad'">{{ item.evaluationType === 1 ? '好评' : '差评' }}</text><text class="name">{{ item.serveItemName }}</text></view><text class="time">{{ format(item.createTime) }}</text></view>
    <view class="content">{{ item.content }}</view>
    <view class="images" v-if="item.pictureArray?.length"><image v-for="url in item.pictureArray.slice(0,3)" :key="url" :src="url" mode="aspectFill" /></view>
    <view class="order"><image :src="item.serveItemImg" mode="aspectFill" /><view><view>订单号：{{ item.ordersNo }}</view><view>{{ item.serveAddress }}</view></view><text>详情 ›</text></view>
  </view></view>
</template>
<script setup>
const props = defineProps({ data: { type: Array, default: () => [] } });
const format = (value) => (value || '').replace('T', ' ').slice(0, 16);
const showDetail = (item) => uni.showModal({ title: item.evaluationType === 1 ? '好评内容' : '差评内容', content: item.content || '暂无文字内容', showCancel: false });
</script>
<style src="../index.scss" lang="scss"></style>
<style lang="scss" scoped>.card{margin:22rpx 24rpx;padding:26rpx;border-radius:22rpx;background:#fff;box-shadow:0 10rpx 30rpx rgba(99,57,34,.06)}.header{display:flex;justify-content:space-between}.type{padding:6rpx 12rpx;font-size:22rpx;border-radius:8rpx;margin-right:14rpx}.good{color:#e15e3b;background:#fff0ea}.bad{color:#a55a5a;background:#fff0f0}.name{font-size:30rpx;font-weight:600}.time{font-size:22rpx;color:#9c8a82}.content{font-size:27rpx;color:#463730;line-height:1.65;margin:20rpx 0}.images{display:flex;gap:12rpx}.images image{width:150rpx;height:150rpx;border-radius:12rpx}.order{display:flex;align-items:center;gap:16rpx;margin-top:22rpx;padding-top:18rpx;border-top:1rpx solid #f2e7e1;font-size:22rpx;color:#95837b;line-height:1.65}.order image{width:80rpx;height:80rpx;border-radius:8rpx}.order text{margin-left:auto;color:#f16749}</style>
