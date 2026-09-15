<template>
  <view class="commit-page">
    <NavBar title="我的评价" :isShowBack="true" />
    <view class="tabs"><view v-for="tab in tabs" :key="tab.value" :class="{ active: query.evaluationType === tab.value }" @click="switchType(tab.value)">{{ tab.label }}</view></view>
    <view v-if="list.length" class="list">
      <view class="card" v-for="item in list" :key="item.id" @click="openDetail(item.id)">
        <view class="card-head"><view><text class="tag" :class="item.evaluationType === 1 ? 'good' : 'bad'">{{ item.evaluationType === 1 ? '好评' : '差评' }}</text><text class="service">{{ item.serveItemName }}</text></view><text class="time">{{ formatTime(item.createTime) }}</text></view>
        <view class="content">{{ item.content }}</view>
        <view v-if="item.pictureArray?.length" class="photos"><image v-for="url in item.pictureArray.slice(0,3)" :key="url" :src="url" mode="aspectFill" /></view>
        <view class="address">{{ item.serveAddress || '服务地址待补充' }} <text>查看详情 ›</text></view>
      </view>
      <view v-if="!hasMore" class="no-more">没有更多评价了</view>
    </view>
    <view v-else class="empty"><image src="../../static/order-confirm-empty.png" mode="aspectFit" /><text>暂无{{ query.evaluationType === 1 ? '好评' : query.evaluationType === 2 ? '差评' : '' }}记录</text><text>完成服务后的评价会显示在这里</text></view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { onLoad, onReachBottom } from '@dcloudio/uni-app';
import { getCommentList } from '@/pages/api/order.js';
import { buildEvaluationPageParams } from '@/utils/evaluation-query.js';

const tabs = [{ label: '全部', value: null }, { label: '好评', value: 1 }, { label: '差评', value: 2 }];
const query = reactive({ pageNo: 1, pageSize: 10, evaluationType: null });
const list = ref([]); const hasMore = ref(true); const loading = ref(false);
const unwrap = (res) => res?.data?.data || res?.data || res || {};
const load = async (reset = false) => {
  if (loading.value || (!reset && !hasMore.value)) return;
  if (reset) { query.pageNo = 1; list.value = []; hasMore.value = true; }
  loading.value = true;
  try {
    const result = unwrap(await getCommentList(buildEvaluationPageParams(query)));
    const rows = result.list || [];
    list.value.push(...rows);
    hasMore.value = list.value.length < Number(result.total || 0);
    if (hasMore.value) query.pageNo += 1;
  } finally { loading.value = false; }
};
const switchType = (value) => { query.evaluationType = value; load(true); };
const openDetail = (id) => uni.navigateTo({ url: `/pages/commit/detail?id=${id}` });
const formatTime = (value) => (value || '').replace('T', ' ').slice(0, 16);
onLoad(() => load(true)); onReachBottom(() => load());
</script>

<style lang="scss" scoped>
.commit-page{min-height:100vh;background:#fff7f3}.tabs{display:flex;background:#fff;padding:20rpx 24rpx 0;box-shadow:0 8rpx 18rpx rgba(89,48,28,.04)}.tabs view{flex:1;text-align:center;padding:18rpx 0 22rpx;color:#806f68;position:relative}.tabs view.active{font-weight:700;color:#ef6245}.tabs view.active:after{content:'';position:absolute;height:5rpx;border-radius:10rpx;background:#f7684b;bottom:0;left:34%;right:34%}.list{padding:24rpx}.card{background:#fff;border-radius:22rpx;padding:26rpx;margin-bottom:20rpx;box-shadow:0 8rpx 28rpx rgba(116,59,32,.06)}.card-head{display:flex;justify-content:space-between;align-items:center}.tag{font-size:22rpx;padding:6rpx 12rpx;border-radius:8rpx;margin-right:14rpx}.good{color:#e05b37;background:#fff0ea}.bad{color:#a85b5b;background:#fff0f0}.service{font-size:30rpx;font-weight:600;color:#302522}.time{font-size:22rpx;color:#a5938b}.content{font-size:27rpx;color:#4e403a;line-height:1.65;margin:22rpx 0}.photos{display:flex;gap:12rpx}.photos image{width:160rpx;height:160rpx;border-radius:12rpx}.address{font-size:23rpx;color:#9a8980;margin-top:18rpx;display:flex;justify-content:space-between}.address text{color:#ed6247}.empty{height:70vh;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#887971;font-size:28rpx;gap:16rpx}.empty image{width:260rpx;height:220rpx}.empty text:last-child{font-size:23rpx;color:#ab9b93}.no-more{text-align:center;color:#a99890;font-size:23rpx;padding:16rpx}
</style>
