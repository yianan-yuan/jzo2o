<template>
  <view class="detail-page"><NavBar title="评价详情" :isShowBack="true" />
    <view v-if="item" class="card"><view class="head"><text :class="item.evaluationType === 1 ? 'good' : 'bad'">{{ item.evaluationType === 1 ? '好评' : '差评' }}</text><text>{{ item.serveItemName }}</text></view><view class="time">{{ formatTime(item.createTime) }}</view><view class="content">{{ item.content }}</view><view class="photos" v-if="item.pictureArray?.length"><image v-for="url in item.pictureArray" :key="url" :src="url" mode="aspectFill" @click="preview(url)" /></view><view class="service"><image :src="item.serveItemImg" mode="aspectFill" /><view><view>{{ item.serveItemName }}</view><view>{{ item.serveAddress }}</view><view>{{ item.serveStartTime }}</view></view></view></view>
  </view>
</template>
<script setup>
import { ref } from 'vue'; import { onLoad } from '@dcloudio/uni-app'; import { getCommentDetail } from '@/pages/api/order.js';
const item = ref(null); const unwrap = (res) => res?.data?.data || res?.data || res;
onLoad(async ({ id }) => { item.value = unwrap(await getCommentDetail(id)); });
const formatTime = (value) => (value || '').replace('T', ' ').slice(0, 16);
const preview = (current) => uni.previewImage({ current, urls: item.value.pictureArray });
</script>
<style lang="scss" scoped>.detail-page{min-height:100vh;background:#fff7f3}.card{margin:24rpx;padding:30rpx;background:#fff;border-radius:24rpx}.head{font-size:32rpx;font-weight:700}.head text:first-child{font-size:22rpx;padding:6rpx 12rpx;border-radius:8rpx;margin-right:16rpx}.good{color:#e05b37;background:#fff0ea}.bad{color:#a85b5b;background:#fff0f0}.time{color:#a3928a;font-size:23rpx;margin-top:16rpx}.content{font-size:29rpx;line-height:1.7;color:#3d302b;margin:32rpx 0}.photos{display:flex;gap:14rpx;flex-wrap:wrap}.photos image{width:190rpx;height:190rpx;border-radius:12rpx}.service{margin-top:34rpx;border-radius:16rpx;background:#fff8f5;padding:18rpx;display:flex;gap:18rpx;font-size:24rpx;color:#75655d;line-height:1.7}.service image{width:110rpx;height:110rpx;border-radius:10rpx}</style>
