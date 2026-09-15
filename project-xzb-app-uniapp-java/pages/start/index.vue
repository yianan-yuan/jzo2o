<!-- 启动页 -->
<template>
  <view class="starPage">
    <view class="page"> </view>
    <view class="bottom">
      <view class="startBrandLockup">
        <view class="startBrandMark">悦</view>
        <view>
          <view class="startBrandName">悦家服务</view>
          <view class="startBrandRole">服务端</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { onMounted } from 'vue';
export default {
  name: 'StarPage',
  setup: () => {
    onMounted(() => {
      const times = setTimeout(() => {
        uni.redirectTo({
          url: '/pages/login/user',
        });
        clearTimeout(times);
      }, 3000);
    });
    return {};
  },
};
</script>

<style src="./index.scss" lang="scss" scoped></style>
