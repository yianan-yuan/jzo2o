<!-- 首页 -->
<template>
  <view class="homePage">
    <view class="homeHero">
      <view class="homeHeroTitle">悦家服务</view>
      <view class="homeHeroSubtitle">让每一次上门服务都更安心</view>
      <view class="homeSearch" @click="handleSearch">
        <view class="city" @click.stop="toCity">
          <image src="/static/dw.png" mode="scaleToFill" />
          <view class="address">{{ city.name || '选择城市' }}</view>
        </view>
        <view class="searchDivider"></view>
        <image src="/static/ss.png" mode="scaleToFill" class="input-uni-icon" />
        <view class="searchPlaceholder">日常保洁</view>
      </view>
    </view>
    <view class="homeContent">
      <view class="promotionBanner">
        <image src="/static/banner-cleaning-one-yuan.png" mode="aspectFill" />
        <view class="promotionCopy">
          <view class="promotionLabel">限时福利</view>
          <view class="promotionTitle">日常保洁限时优惠价</view>
          <view class="promotionPrice">¥1</view>
          <view class="promotionHint">专业保洁上门服务，限时优惠</view>
        </view>
      </view>
      <view class="tips">
        <view>
          <image src="/static/smile.png" mode="scaleToFill" />
          <test> 悦家服务平台，给你贴心的专业上门服务 </test>
        </view>
        <view>
          <image src="/static/yuan.png" mode="scaleToFill" />
          <test> 标准定价 售后无忧 </test>
        </view>
      </view>
      <view class="couponBenefitCard" @click="toCoupon">
        <view class="couponBenefitCopy">
          <view class="couponBenefitLabel">优惠福利</view>
          <view class="couponBenefitTitle">领券下单更划算</view>
          <view class="couponBenefitDesc">全员可领，优惠券结算时可用</view>
          <view class="couponBenefitAction">去抢券</view>
        </view>
        <image
          class="couponBenefitImage"
          src="../../static/coupon-empty.png"
          mode="aspectFit"
        ></image>
      </view>
      <!-- 未选择城市或者城市下无服务时的显示 -->
      <view class="empty-box" v-if="!city.name || menuData.length === 0">
        <image src="../../static/city-service-empty.png" mode="aspectFit"></image>
        <view v-if="!city.name">
          用户当前未授权位置，请手动选择城市进行下单
        </view>
        <view v-else> 当前城市暂未开通服务，请切换其他城市进行下单 </view>
        <button class="agree-btn btn" @click="toCity()">
          手动选择服务城市
        </button>
      </view>

      <view class="serviceSection" v-if="menuData.length">
        <view class="sectionTitle">服务分类</view>
        <scroll-view class="serviceCategoryList" scroll-x>
          <view
            class="serviceCategory"
            v-for="(item, index) in menuData"
            :key="index"
            @click="toService(item.serveTypeId, 1)"
          >
            <image :src="item.serveTypeIcon" mode="scaleToFill" />
            <view>{{ item.serveTypeName }}</view>
          </view>
        </scroll-view>
        <view class="serviceGrid">
          <view class="serviceGroup" v-for="(item, index) in menuData" :key="index">
            <view class="serviceGroupTitle">{{ item.serveTypeName }}</view>
            <view class="serviceItems">
              <view
                class="serviceItem"
                v-for="(content, key) in item.serveResDTOList"
                :key="key"
                @click="toService(content.id, 2)"
              >
                <image :src="content.serveItemIcon" mode="scaleToFill" />
                <view>{{ content.serveItemName }}</view>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="recommendSection" v-if="hotData.length > 0">
        <view class="sectionTitle">精选推荐</view>
        <view class="recommendCard" v-for="(item, index) in hotData" :key="index">
          <view
            class="tag"
            :class="
              (index + 1) % 3 === 1
                ? 'tag1'
                : (index + 1) % 3 === 2
                ? 'tag2'
                : 'tag3'
            "
            >{{
              // (index + 1)/3 取余数
              (index + 1) % 3 === 1
                ? '专业、贴心的上门服务'
                : (index + 1) % 3 === 2
                ? '标准定价，售后无忧'
                : '悦家服务，安心上门更省心'
            }}</view
          >
          <image
            :src="item.serveItemImg"
            mode="widthFix"
            class="cardImg"
            @click="toService(item.id, 2)"
          />
          <view class="cardName">{{ item.serveItemName }}</view>
          <view class="reservation">
            <view class="servicePrice">￥<text>{{ item.price }}</text></view>
            <button @click="toService(item.id, 2)">立即预约</button>
          </view>
        </view>
      </view>
    </view>
    <view class="foot">
      <button @click="toLogin" class="agree-btn btn" v-if="false">登录</button>
      <button @click="toEvaluate" class="agree-btn btn" v-if="false">
        评价
      </button>
    </view>
    <!-- footer -->
    <UniFooter :pagePath="'/pages/index/index'"></UniFooter>
    <!-- end -->
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getHomeService, getHotServe } from '../api/index.js';
const nickName = ref(''); //昵称
const token = ref(''); //token
const menuData = ref([]); //菜单数据
const hotData = ref([]); //热门服务数据
const searchVal = ref(''); //搜索框的值
const city = ref({}); //城市

onShow(() => {
  // 获取nickName
  nickName.value = uni.getStorageSync('nickName');
  // 获取token
  token.value = uni.getStorageSync('token');

  if (!token.value && !nickName.value) {
    uni.navigateTo({
      url: '/pages/login/index',
    });
  } else {
    // 延时获取城市，防止城市未获取到
    setTimeout(() => {
      city.value = uni.getStorageSync('city');
      if (city.value) {
        getHomeServiceData();
        getHotServeData();
      }
    }, 200);
  }
});

const toLogin = () => {
  uni.navigateTo({
    url: '/pages/login/index',
  });
};
// 监听下拉刷新
onPullDownRefresh(() => {
  getHomeServiceData();
  getHotServeData();
});
// 跳转到服务列表页面和服务详情页面
const toService = (val, number) => {
  if (number === 1) {
    uni.reLaunch({
      url: `/pages/service/index?serveTypeId=${val}`,
    });
  } else {
    uni.navigateTo({
      url: `/pages/service/components/airMaintenance?id=${val}`,
    });
  }
};
// 跳转到城市选择页面
const toCity = () => {
  uni.navigateTo({
    url: '/pages/city/index',
  });
};
// 跳转到抢券页面
const toCoupon = () => {
  uni.navigateTo({
    url: '/pages/coupon/index',
  });
};
// 跳转到评价页面
const toEvaluate = () => {
  uni.navigateTo({
    url: '/subPages/order/components/evaluate',
  });
};
// 获取金刚区图标
const getHomeServiceData = async () => {
  await getHomeService({
    regionId: uni.getStorageSync('city').id,
  }).then((res) => {
    if (res.data.code === 200) {
      menuData.value = res.data.data;
    }
  });
};
// 获取热门服务
const getHotServeData = async () => {
  await getHotServe({
    regionId: uni.getStorageSync('city').id,
  }).then((res) => {
    if (res.data.code === 200) {
      hotData.value = res.data.data;
    }
  });
};
// 搜索
const handleSearch = () => {
  uni.navigateTo({
    url: `/pages/search/index`,
  });
};
</script>
<style src="./index.scss" lang="scss" scoped></style>
