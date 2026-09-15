<template>
  <!-- 公用nav -->
  <view class="navBox">
    <view class="baseSetting">
      <view class="location">
        <image
          src="../../static/new/icon_weizhi4@2x.png"
          @click="handleClick"
        ></image>
        {{ location }}
        <!-- {{ '北京市' }} -->
      </view>
      <view
        class="item"
        :key="index"
        v-for="(item, index) in baseSetting"
        @click="handleLink(item.path)"
      >
        <image :src="item.url" class="itemImg"></image>
        <view class="itemLabel">{{ item.label }}</view>
      </view>
    </view>
    <view class="workbenchBanner">
      <view class="workbenchIcon">
        <view class="workbenchHandle"></view>
        <view class="workbenchBody"><text>✓</text></view>
      </view>
      <view class="workbenchCopy">
        <view class="workbenchEyebrow">服务工作台</view>
        <view class="workbenchTitle">今日专注服务</view>
        <view class="workbenchText">开启接单，附近订单将优先推送给你</view>
      </view>
      <view class="workbenchCircle circleOne"></view>
      <view class="workbenchCircle circleTwo"></view>
    </view>
    <!-- 提示窗示例 -->
    <uni-popup ref="alertDialog" type="dialog" :is-mask-click="false">
      <view class="dialog">
        <view class="content"
          >当前定位显示你在“{{ location }}”，是否需要切换城市？</view
        >
        <view class="footer">
          <view class="cancel" @click="handleNo">否</view>
          <view class="toSet" @click="handleToSet">去切换</view>
        </view>
      </view>
    </uni-popup>
  </view>
  <!-- end -->
</template>

<script setup>
import { ref, reactive } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getSettingInfo } from '../../pages/api/setting';
import { data } from '../../utils/h5Data.js';
import { useStore } from 'vuex';
const store = useStore(); //vuex获取储存数据
const users = store.state.user;
const workerCityCode = ref();
const workerCityName = ref();
const location = ref('');
const cityCode = ref('');
const alertDialog = ref(null);
onShow(() => {
  getSettingInfo().then((res) => {
    workerCityCode.value = res.data.cityCode;
    workerCityName.value = res.data.cityName;
    if (process.env.VUE_APP_PLATFORM === 'h5') {
      location.value = users.cityCode ? users.cityName : data.city;
      cityCode.value = users.cityCode ? users.cityCode : data.cityCode;
      if (!users.first) {
        return;
      }
      if (users.cityCode) {
        if (users.cityCode !== workerCityCode.value) {
          alertDialog.value.open();
          store.commit('setFirst', false);
        }
      } else {
        alertDialog.value.open();
      }
    } else {
      uni.getLocation({
        type: 'gcj02',
        geocode: true,
        success: function (res) {
          location.value = res.address.city;
          cityCode.value = res.address.cityCode;
          if (!users.first) {
            return;
          }
          if (res.address.cityCode !== workerCityCode.value) {
            alertDialog.value.open();
            store.commit('setFirst', false);
          }
        },
        fail: (err) => {
          location.value = '获取失败';
        },
      });
    }
  });
});
const handleToSet = () => {
  alertDialog.value.close();
  uni.navigateTo({
    url: '/pages/serviceRange/index',
  });
};
const handleNo = () => {
  alertDialog.value.close();
};
const handleClick = () => {
  alertDialog.value.open();
};
// ------定义变量------
const baseSetting = reactive([
  {
    label: '服务技能',
    url: '../../static/new/btn_top_fwjn@2x.png',
    path: '/pages/serviceSkill/index',
  },
  {
    label: '接单设置',
    url: '../../static/new/btn_top_jdsz@2x.png',
    path: '/pages/getOrder/index',
  },
  {
    label: '服务范围',
    url: '../../static/new/btn_top_fwfw@2x.png',
    path: '/pages/serviceRange/index',
  },
  // {
  //   label: '城市设置',
  //   url: '../../static/new/btn_top_fwjn@2x.png',
  //   path: '',
  // },
]);
// ------方法------
const handleLink = (val) => {
  uni.navigateTo({
    url: val,
  });
};
</script>
<style lang="scss" scoped>
@import url(@/styles/theme.scss);
// 导航
.navBox {
  background: linear-gradient(180deg, var(--warm-primary) 0%, #ff9a72 58%, var(--warm-page-bg) 100%);
  height: 344rpx;
  .dialog {
    width: 556rpx;
    // height: 272rpx;
    background-color: var(--warm-surface);
    border-radius: var(--warm-radius-lg);
    position: relative;
    .content {
      // height: 225rpx;
      text-align: center;
      font-size: 28rpx;
      color: var(--warm-text);
      padding-top: 56rpx;
      padding-bottom: 38rpx;
      padding-left: 33rpx;
      padding-right: 33rpx;
    }
    .footer {
      width: 100%;
      font-size: 32rpx;
      color: var(--warm-primary);
      display: flex;
      align-items: center;
      height: 100rpx;
      border-top: 2rpx solid var(--warm-line);
      .cancel,
      .toSet {
        flex: 1;
        text-align: center;
        height: 100%;
        line-height: 100rpx;
      }
      .cancel {
        color: var(--warm-text);
        border-right: 2rpx solid var(--warm-line);
      }
    }
  }
  .workbenchBanner {
    position: relative;
    display: flex;
    align-items: center;
    height: 196rpx;
    width: calc(100% - 44rpx);
    margin: 0 auto;
    margin-top: 34rpx;
    padding: 0 30rpx;
    overflow: hidden;
    box-sizing: border-box;
    border-radius: var(--warm-radius-lg);
    background: linear-gradient(135deg, #fffaf7, #ffe5d9);
    box-shadow: var(--warm-shadow);

    .workbenchIcon {
      position: relative;
      width: 104rpx;
      height: 104rpx;
      margin-right: 24rpx;
      flex-shrink: 0;
    }

    .workbenchHandle {
      position: absolute;
      top: 14rpx;
      left: 30rpx;
      width: 44rpx;
      height: 24rpx;
      border: 8rpx solid var(--warm-primary);
      border-bottom: 0;
      border-radius: 18rpx 18rpx 0 0;
      box-sizing: border-box;
    }

    .workbenchBody {
      position: absolute;
      bottom: 10rpx;
      left: 10rpx;
      width: 84rpx;
      height: 62rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 16rpx;
      background: var(--warm-primary);
      color: #fff;
      font-size: 36rpx;
      font-weight: 700;
      box-shadow: 0 12rpx 22rpx rgba(240, 108, 72, .24);
    }

    .workbenchCopy { position: relative; z-index: 1; }
    .workbenchEyebrow { color: var(--warm-primary); font-size: 22rpx; }
    .workbenchTitle { margin-top: 4rpx; color: var(--warm-text); font-size: 34rpx; font-weight: 700; }
    .workbenchText { margin-top: 8rpx; color: var(--warm-text-muted); font-size: 23rpx; }
    .workbenchCircle { position: absolute; border-radius: 50%; background: rgba(240, 108, 72, .1); }
    .circleOne { width: 150rpx; height: 150rpx; right: -34rpx; top: -54rpx; }
    .circleTwo { width: 82rpx; height: 82rpx; right: 56rpx; bottom: -42rpx; }
  }
  .baseSetting {
    display: flex;
    justify-content: space-between;
    padding: 0 74rpx;
    padding-left: 30rpx;
    .location {
      padding: 15rpx 20rpx;
      max-width: 180rpx;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: center;
      border-radius: 34rpx;
      margin-top: 63rpx;
      background-color: var(--warm-surface);
      color: var(--warm-text);
      box-shadow: 0 8rpx 20rpx rgba(141, 77, 49, 0.12);
      display: flex;
      align-items: center;
      justify-content: center;
      image {
        width: 32rpx;
        height: 36rpx;
        vertical-align: sub;
        margin-right: 6rpx;
      }
    }
    .item {
      text-align: center;
      margin-top: 37rpx;
      min-width: 90rpx;
      padding: 10rpx 6rpx;
      border-radius: 20rpx;
      background: rgba(255, 255, 255, 0.16);
      .itemImg {
        width: 56rpx;
        height: 56rpx;
      }
      .itemLabel {
        font-size: 24rpx;
        color: var(--warm-surface);
      }
    }
  }
}
</style>
