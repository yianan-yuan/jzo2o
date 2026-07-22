<!-- 发表评价 -->
<template>
  <view :class="edit ? 'evaluate' : 'bg-wt evaluate appList'">
    <NavBar
      :title="title"
      :isShowBack="true"
      :handleToLink="handleToLink"
    ></NavBar>
    <view class="evaluationBox" v-if="edit">
      <view class="head">
        <view class="title"> 服务评价 </view>
        <!-- 输入框 -->
      </view>
      <!-- 加入星星打分 -->
      <view
        class="head"
        v-for="(item, index) in CommentData.serveItemScoreItems"
        :key="index"
      >
        <view class="title fw-400"> {{ item.itemName }} </view>
        <uni-rate
          activeColor="#F74145"
          color="#ccc"
          :size="21"
          margin="16"
          v-model="item.score"
        ></uni-rate>
        <!-- 输入框 -->
      </view>
      <view class="inputBox">
        <uni-easyinput
          type="textarea"
          v-model="CommentData.serveItemEvaluationContent"
          placeholder="请输入对本次服务的的真实评价…"
          :inputBorder="false"
          :maxlength="
            evaluateConfig?.serveItemEvaluationSystemInfo.evaluationConfig
              .contentWordNum
          "
          :styles="{
            color: '#151515',
          }"
        >
        </uni-easyinput>
        <view class="textNum">
          <!-- 表情 -->
          <Emoji @handleEmoji1="handleEmoji1" :id="1"></Emoji>
          <text
            >{{ CommentData.serveItemEvaluationContent.length }}/{{
              evaluateConfig?.serveItemEvaluationSystemInfo.evaluationConfig
                .contentWordNum
            }}</text
          >
        </view>
      </view>
      <!-- 上传图片 -->
      <view class="uploadPhoto">
        <uni-section type="line">
          <view class="example-body">
            <uni-file-picker
              limit="3"
              ref="upImg"
              v-model="filePickerValue"
              @select="handleSelect"
              @delete="handleDelete"
              :auto-upload="false"
              :preview="true"
              :size-type="['compressed']"
              :source-type="['album', 'camera']"
            ></uni-file-picker>
          </view>
        </uni-section>
      </view>
    </view>

    <!-- 师傅评价 -->
    <view class="evaluationBox shifu" v-if="edit">
      <view class="head">
        <view class="title"> 师傅评价 </view>
        <!-- 输入框 -->
      </view>
      <!-- 加入星星打分 -->
      <view
        class="head"
        v-for="(item, index) in CommentData.serveProviderScoreItems"
        :key="index"
      >
        <view class="title fw-400"> {{ item.itemName }} </view>
        <uni-rate
          activeColor="#F74145"
          color="#ccc"
          :size="21"
          margin="16"
          v-model="item.score"
        ></uni-rate>
        <!-- 输入框 -->
      </view>
      <view class="inputBox">
        <uni-easyinput
          type="textarea"
          v-model="CommentData.serveProviderEvaluationContent"
          placeholder="请输入对本次服务的的真实评价…"
          :maxlength="
            evaluateConfig?.serveProviderEvaluationSystemInfo?.evaluationConfig
              .contentWordNum
          "
          :inputBorder="false"
          :styles="{
            color: '#151515',
          }"
        >
        </uni-easyinput>
        <view class="textNum">
          <!-- 表情 -->
          <Emoji @handleEmoji2="handleEmoji2" :id="2"></Emoji>
          <text
            >{{ CommentData.serveProviderEvaluationContent.length }}/{{
              evaluateConfig?.serveItemEvaluationSystemInfo.evaluationConfig
                .contentWordNum
            }}</text
          ></view
        >
      </view>
    </view>
    <view class="anonymous" v-if="edit">
      <uni-section title="匿名" type="line">
        <uni-data-checkbox
          multiple
          :localdata="anonymous"
          @change="changeAnonymous"
        ></uni-data-checkbox>
      </uni-section>
    </view>
    <view class="evaluationBox" v-if="!edit">
      <view class="successBox">
        <image
          src="../../../static/pjcg@2x.png"
          mode="scaleToFill"
          class="successIcon"
        />
        <view class="text"> 评价成功 </view>
        <!-- 查看评价按钮先隐藏
        <button @click="toComments" class="agree-btn btn">查看评价</button>
        -->
      </view>
    </view>
    <view class="pageFixFoot" v-if="edit">
      <button @click="toSubmit" class="agree-btn btn">确认提交</button>
    </view>
    <!-- 分割线 -->
    <view class="cutLine"></view>
    <!-- 订单列表 -->
    <scroll-view
      scroll-y="true"
      class="evaluateScroll"
      :class="itemData.length > 0 ? '' : 'grey'"
    >
      <view class="orderList" v-if="!edit && itemData.length > 0">
        <view class="title">— 继续评价 —</view>
        <view class="timeList">
          <view
            class="box boxRadius item"
            v-for="(item, index) in itemData"
            :key="index"
          >
            <view class="itemCon">
              <view class="head">
                <image :src="item.serveItemImg"></image>
              </view>
              <view class="rightBox">
                <view class="rText">
                  <view class="itemTit tit"
                    ><text>{{ item.serveItemName }}</text>
                  </view>
                  <view class="info">{{
                    formatDateTimeToDateTimeString(
                      new Date(item.serveStartTime.replace(/-/g, '/'))
                    )
                  }}</view>
                </view>
                <view class="btnBox">
                  <view class="btn" @click.stop="handleDetail(item.id)"
                    >去评价</view
                  ></view
                >
              </view></view
            >
            <view class="address">{{ item.serveAddress }} </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue';
// 接口
import { getOrderScroll, addComment } from '@/pages/api/order.js';
import { useStore } from 'vuex';
import Emoji from './emoji.vue';
import { onLoad, onReachBottom } from '@dcloudio/uni-app';
import { formatDateTimeToDateTimeString } from '@/utils/index.js';
import { getEvaluate } from '../../../pages/api/service';
import { baseUrl } from '@/utils/env.js';
const edit = ref(true); //true评价 false评价完成页面
const token = uni.getStorageSync('token');
const title = ref('评价');
const store = useStore();
const evaluateConfig = ref(); //评价配置
const providerScoreList = ref([]); //师傅评分项列表
const serveItemScoreList = ref([]);//服务评分项列表
const isSendRequest = ref(false); // 是否发送请求
const moreStatus = ref('more'); //more loading noMore
const loading = ref(true);
const pages = ref(0);
const netStatus = ref(true);
let params = reactive({
  sortBy: '',
  ordersStatus: 400,
});
const anonymous = ref([
  {
    text: '匿名评价',
    value: '1',
    checked: true,
  },
]);
// 评价数据
const CommentData = ref({
  // 评价时星级默认为 0 星（用户手动打分）
  serveProviderScoreItems: [
    { itemId: '0', score: 0, itemName: '服务态度' },
    { itemId: '0', score: 0, itemName: '专业能力' },
  ],
  serveItemScoreItems: [
    { itemId: '0', score: 0, itemName: '整体评价' },
  ],
  serveItemEvaluationContent: '',
  isAnonymous: 0,
  ordersId: '',
  serveProviderEvaluationContent: '',
  serveItemPictureArray: [],
});

onLoad((option) => {
  getEvaluateInfo();
  CommentData.value.ordersId = option.id;
});
const itemData = ref([]); //预约数据
const upImg = ref(); // 上传图片
// 提交
const toSubmit = () => {
  // 判断serveItemScoreItems中的每一项是否有空值如果有空值返回false
  const isNull = CommentData.value.serveItemScoreItems.some((item) => {
    return item.score === null;
  });
  if (isNull) {
    uni.showToast({
      title: '请将信息填写完整',
      icon: 'none',
      duration: 2000,
    });
    return;
  }
  if (!CommentData.value.serveItemEvaluationContent) {
    uni.showToast({
      title: '请填写服务评价',
      icon: 'none',
      duration: 2000,
    });
    return;
  }
  if (!localFileList.value.length) {
    uni.showToast({
      title: '请上传评价图片',
      icon: 'none',
      duration: 2000,
    });
    return;
  }
  if (!CommentData.value.ordersId) {
    uni.showToast({
      title: '订单信息缺失',
      icon: 'none',
      duration: 2000,
    });
    return;
  }
  // 防止重复提交
  if (isSubmitting.value) {
    return;
  }
  isSubmitting.value = true;
  // 1. 上传所有未上传的图片（内部已经 showLoading "上传图片中"）
  uploadAllImages()
    .then((uploadedUrls) => {
      // 2. 上传完成，关闭上传 loading，显示提交 loading
      uni.hideLoading();
      uni.showLoading({ title: '提交中', mask: true });
      // 2. 提交评价
      const submitData = {
        ordersId: CommentData.value.ordersId,
        serveItemEvaluationContent: CommentData.value.serveItemEvaluationContent,
        serveItemPictureArray: uploadedUrls,
        serveItemScoreItems: CommentData.value.serveItemScoreItems,
        serveProviderEvaluationContent:
          CommentData.value.serveProviderEvaluationContent === ''
            ? '此用户没有填写评价，系统默认好评'
            : CommentData.value.serveProviderEvaluationContent,
        serveProviderScoreItems: CommentData.value.serveProviderScoreItems,
        isAnonymous: CommentData.value.isAnonymous,
      };
      return addComment(submitData);
    })
    .then((res) => {
      uni.hideLoading();
      isSubmitting.value = false;
      console.log('addComment response:', JSON.stringify(res));
      // 兼容两种响应格式：res.data.code 或 res.code
      const body = (res && res.data) || res;
      const code = body && body.code;
      const msg = body && body.msg;
      if (code == 200 || code == 0) {
        uni.showToast({
          title: '提交成功',
          icon: 'none',
          duration: 2000,
        });
        title.value = '';
        edit.value = false;
        getNewData();
      } else {
        // 业务码非 0/200：显示后端 msg
        uni.showToast({
          title: msg || '提交失败',
          icon: 'none',
          duration: 3000,
        });
      }
    })
    .catch((err) => {
      uni.hideLoading();
      isSubmitting.value = false;
      console.error('addComment error:', JSON.stringify(err));
      const msg =
        (err && err.data && err.data.msg) ||
        (err && err.msg) ||
        (err && err.errMsg) ||
        '提交失败';
      uni.showToast({
        title: msg,
        icon: 'none',
        duration: 3000,
      });
    });
};
// 跳转到评价列表
const toComments = () => {
  uni.navigateTo({
    url: '/pages/commit/index',
  });
};
// 跳转到下一个评价
const handleDetail = (id) => {
  uni.navigateTo({
    url: `/subPages/order/components/evaluate?id=${id}`,
  });
};
// 获取待评价列表
const getNewData = async (type) => {
  params = {
    ...params,
  };
  params.sortBy = itemData.value[itemData.value.length - 1]?.sortBy
    ? itemData.value[itemData.value.length - 1]?.sortBy
    : '';
  if (isSendRequest.value) {
    return;
  }
  moreStatus.value = 'loading';
  loading.value = false;
  await getOrderScroll(params)
    .then((res) => {
      if (res.data.code == 200) {
        const { data } = res.data;
        // items == null 会报错 把他处理掉
        const items = data == null ? [] : data;
        moreStatus.value = items.length < 10 ? 'no-more' : 'more';
        // 从第一页请求 清空之前的数据
        // 下拉数据合并
        itemData.value = itemData.value ? [...itemData.value, ...items] : items;
        // 如果 当前页面的数据已经全部数据了 那么停止拿数据
        pages.value = data.length;
        if (pages.value < 10) {
          isSendRequest.value = true;
          moreStatus.value = 'noMore';
        }
        uni.stopPullDownRefresh();
        netStatus.value = true;
        loading.value = true;
      }
    })
    .catch((err) => {
      // 弹出错误提示
      console.log(err);
      netStatus.value = false;
    });
};
// 上拉加载
onReachBottom(() => {
  if (!edit.value && itemData.value.length > 0) {
    if (pages.value < 10) {
      moreStatus.value = 'noMore';
      return false;
    } else {
      moreStatus.value = 'loading';
      let times = setTimeout(() => {
        getNewData();
      }, 1000); //这里延时一秒在加载方法有个loading效果
    }
  }
});
// 返回
const handleToLink = () => {
  if (edit.value) {
    uni.navigateBack();
  } else {
    uni.redirectTo({
      url: '/subPages/order/index',
    });
  }
};
// 匿名
const changeAnonymous = (e) => {
  CommentData.value.isAnonymous = e.detail.data.length === 0 ? 0 : 1;
};
// 获取评价信息
const getEvaluateInfo = () => {
  getEvaluate().then((res) => {
    if (res.data.code === 200) {
      evaluateConfig.value = res.data.data;
      providerScoreList.value =
        evaluateConfig.value.serveProviderEvaluationSystemInfo.evaluationConfig.scoreConfigList.filter(
          (item) => item.enabled == true
        );
      CommentData.value.serveProviderScoreItems = providerScoreList.value.map(
        (item) => {
          return {
            itemId: item.itemId,
            score: 0,
            itemName: item.itemName,
          };
        }
      );
      serveItemScoreList.value =
        evaluateConfig.value.serveItemEvaluationSystemInfo.evaluationConfig.scoreConfigList.filter(
          (item) => item.enabled == true
        );
      CommentData.value.serveItemScoreItems = serveItemScoreList.value.map(
        (item) => {
          return {
            itemId: item.itemId,
            score: 0,
            itemName: item.itemName,
          };
        }
      );
      store.commit('user/setEvaluate', res.data.data);
    }
  });
};
// 是否提交中（防止重复提交）
const isSubmitting = ref(false);
// file-picker 内部维护的图片数组（v-model）
const filePickerValue = ref([]);
// 选中后只把临时路径存到本地列表，提交时上传
const localFileList = ref([]);
// 上传单张图片到服务器，返回上传后的 url
const uploadOneImage = (file) => {
  return new Promise((resolve, reject) => {
    // file 可能是 { url, path, ... } 多种结构
    const filePath =
      file.path ||
      file.url ||
      (file.file && (file.file.path || file.file.url));
    if (!filePath) {
      reject(new Error('图片路径为空'));
      return;
    }
    uni.uploadFile({
      url: `${baseUrl}/publics/storage/upload`,
      filePath,
      name: 'file',
      header: {
        Authorization: uni.getStorageSync('token') || '',
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          if (data && data.data && data.data.url) {
            resolve(data.data.url);
          } else {
            reject(new Error((data && data.msg) || '上传失败'));
          }
        } catch (e) {
          reject(new Error('解析响应失败'));
        }
      },
      fail: (err) => {
        reject(err);
      },
    });
  });
};
// 把所有待上传图片依次上传，返回 url 列表
const uploadAllImages = () => {
  const list = localFileList.value || [];
  if (!list.length) {
    return Promise.resolve([]);
  }
  uni.showLoading({ title: '上传图片中', mask: true });
  const tasks = list.map((item) => uploadOneImage(item));
  return Promise.all(tasks)
    .then((urls) => {
      uni.hideLoading();
      return urls;
    })
    .catch((err) => {
      uni.hideLoading();
      uni.showToast({
        title: '图片上传失败',
        icon: 'none',
        duration: 2000,
      });
      return Promise.reject(err);
    });
};
// 选中文件后：保存到本地数组
const handleSelect = (e) => {
  // @select 事件参数：{ tempFiles, tempFilePaths }
  if (e && e.tempFiles && e.tempFiles.length) {
    // 把新选择的文件路径合并到本地列表
    const newPaths = e.tempFiles.map((f) => ({
      path: f.path || f.url,
      name: f.name,
      extname: f.extname,
    }));
    localFileList.value = [...localFileList.value, ...newPaths];
  }
};
// 删除文件回调
const handleDelete = (e) => {
  // e.index 是被删除的下标（部分版本提供）
  const idx = typeof e.index === 'number' ? e.index : -1;
  if (idx >= 0 && idx < localFileList.value.length) {
    localFileList.value.splice(idx, 1);
  } else {
    // 通过 url 匹配删除
    const path = e.tempFile && (e.tempFile.path || e.tempFile.url);
    if (path) {
      localFileList.value = localFileList.value.filter(
        (f) => f.path !== path
      );
    }
  }
};
// handleEmoji'
const handleEmoji1 = (val) => {
  CommentData.value.serveItemEvaluationContent += `${val.id}`;
};
const handleEmoji2 = (val) => {
  CommentData.value.serveProviderEvaluationContent += `${val.id}`;
};
</script>

<style lang="scss" scoped src="../index.scss"></style>
<style lang="scss" scoped>
.address {
  margin-top: 4rpx;
  padding-left: 12rpx;
  padding-right: 12rpx;
  font-family: PingFangSC-Regular;
  font-weight: 400;
  font-size: 28rpx;
  color: #888888;
  letter-spacing: 0.16px;
  margin-bottom: 20rpx;
}
.grey {
  background-color: #f8f8f8;
}
.fw-400{
  font-weight: 400 !important;
}
.rightBox {
  display: flex;
  justify-content: space-between;
  flex: 1;
  .btnBox {
    display: flex;
    align-content: center;
    // 上下居中
    .btn{
      margin: auto;
    }
    
  }
}
.appList .itemCon .tit {
  margin-top: 0;
  padding-bottom: 11rpx;
}
</style>
