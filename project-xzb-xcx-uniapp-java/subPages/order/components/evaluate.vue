<template>
  <view class="evaluation-page">
    <NavBar title="评价" :isShowBack="true" :handleToLink="goBack" />
    <view class="card">
      <view class="title">本次师傅服务</view>
      <view class="sub-title">请基于真实服务体验给出评价</view>
      <view class="type-row">
        <view class="type-option" :class="{ active: form.evaluationType === 1 }" @click="form.evaluationType = 1">
          <text class="emoji">👍</text><text>好评</text>
        </view>
        <view class="type-option" :class="{ active: form.evaluationType === 2 }" @click="form.evaluationType = 2">
          <text class="emoji">👎</text><text>差评</text>
        </view>
      </view>
      <textarea v-model="form.content" class="content" maxlength="300" placeholder="请写下本次师傅服务的真实感受（必填）" />
      <view class="count">{{ form.content.length }}/300</view>
      <view class="upload-title">服务图片 <text>（可选，最多 9 张）</text></view>
      <uni-file-picker v-model="files" limit="9" :auto-upload="false" :preview="true" :size-type="['compressed']" :source-type="['album', 'camera']" @select="handleSelect" @delete="handleDelete" />
    </view>
    <view class="bottom"><button class="submit" :disabled="submitting" @click="submit">确认提交</button></view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { addComment } from '@/pages/api/order.js';
import { baseUrl } from '@/utils/env.js';
import { keepOrderId } from '@/utils/orderId.js';

const files = ref([]);
const localFiles = ref([]);
const submitting = ref(false);
const form = reactive({ ordersId: null, evaluationType: null, content: '', pictureArray: [] });

onLoad((option) => { form.ordersId = keepOrderId(option.id); });

const handleSelect = (event) => {
  const selected = (event.tempFiles || []).map((file) => ({ path: file.path || file.url, name: file.name }));
  localFiles.value.push(...selected);
};
const handleDelete = (event) => {
  const index = typeof event.index === 'number' ? event.index : -1;
  if (index >= 0) localFiles.value.splice(index, 1);
};
const upload = (file) => new Promise((resolve, reject) => {
  uni.uploadFile({
    url: `${baseUrl}/publics/storage/upload`, filePath: file.path, name: 'file',
    header: { Authorization: uni.getStorageSync('token') || '' },
    success: (res) => {
      try {
        const body = JSON.parse(res.data);
        if (body?.data?.url) resolve(body.data.url);
        else reject(new Error(body?.msg || '图片上传失败'));
      } catch (_) { reject(new Error('图片上传响应异常')); }
    }, fail: reject,
  });
});
const submit = async () => {
  if (!form.ordersId) return uni.showToast({ title: '订单信息缺失', icon: 'none' });
  if (!form.evaluationType) return uni.showToast({ title: '请选择好评或差评', icon: 'none' });
  if (!form.content.trim()) return uni.showToast({ title: '请填写评价内容', icon: 'none' });
  if (submitting.value) return;
  submitting.value = true;
  uni.showLoading({ title: '提交中', mask: true });
  try {
    form.pictureArray = await Promise.all(localFiles.value.map(upload));
    const res = await addComment({ ...form, content: form.content.trim() });
    const body = res?.data || res;
    if (body?.code !== 200 && body?.success !== true) throw new Error(body?.msg || '提交失败');
    uni.showToast({ title: '评价提交成功', icon: 'success' });
    setTimeout(() => uni.redirectTo({ url: '/pages/commit/index' }), 500);
  } catch (error) {
    uni.showToast({ title: error?.message || '提交失败', icon: 'none' });
  } finally { uni.hideLoading(); submitting.value = false; }
};
const goBack = () => uni.navigateBack();
</script>

<style lang="scss" scoped>
.evaluation-page{min-height:100vh;background:#fff7f3;padding-bottom:160rpx}.card{margin:28rpx;background:#fff;border-radius:28rpx;padding:32rpx;box-shadow:0 12rpx 32rpx rgba(244,103,73,.08)}.title{font-size:36rpx;font-weight:700;color:#2b2523}.sub-title{font-size:25rpx;color:#9b8982;margin:14rpx 0 28rpx}.type-row{display:flex;gap:24rpx}.type-option{flex:1;border:2rpx solid #f1e5df;border-radius:20rpx;padding:24rpx 0;text-align:center;color:#7f706a;background:#fffaf8}.type-option.active{border-color:#f7684b;background:#fff0eb;color:#ed563a;font-weight:700}.emoji{margin-right:12rpx;font-size:34rpx}.content{box-sizing:border-box;width:100%;height:240rpx;margin-top:32rpx;padding:22rpx;border-radius:18rpx;background:#fff8f5;font-size:28rpx;color:#342a26}.count{text-align:right;color:#a6958d;font-size:22rpx;margin-top:8rpx}.upload-title{font-size:28rpx;font-weight:600;margin:28rpx 0 18rpx;color:#382d28}.upload-title text{font-size:22rpx;color:#a6958d;font-weight:400}.bottom{position:fixed;left:0;right:0;bottom:0;padding:24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));background:#fff}.submit{background:#f7684b;color:#fff;border-radius:48rpx;font-size:32rpx}.submit[disabled]{opacity:.55}
</style>
