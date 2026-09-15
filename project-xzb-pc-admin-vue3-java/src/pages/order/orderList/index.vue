<!-- 订单列表 -->
<template>
  <router-view v-if="url !== '/order/orderList'"></router-view>
  <div v-else class="base-up-wapper bgTable min-h">
    <!-- 搜索表单区域 -->
    <searchFormBox
      :initSearch="initSearch"
      :typeSelect="typeSelect"
      @handleSearch="handleSearch"
      @handleReset="handleReset"
    ></searchFormBox>
    <!-- end -->
    <!-- 表格 -->
    <tableList
      :list-data="listData"
      :pagination="pagination"
      @fetchData="fetchData"
      @onPageChange="onPageChange"
      @handleClickRefund="handleClickRefund"
      @handleSortChange="handleSortChange"
    ></tableList>
    <!-- end -->
    <!-- 新增，编辑弹窗 -->
    <DialogForm
      :visible="visible"
      :title="title"
      :label="label"
      :data="DialogFormData"
      @handleSubmit="handleSubmit"
      @handleClose="handleClose"
      @fetchData="fetchData"
    />
    <!-- end -->
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderList, refundOrder } from '@/api/order'
import { forEach } from 'lodash'
import DialogForm from './components/DialogForm.vue' // 新增,编辑弹窗.
import tableList from './components/TableList.vue' // 表格
import searchFormBox from './components/SearchForm.vue' // 搜索框表单
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'

const route = useRoute()
const visible = ref(false) // 新增，编辑弹窗
const listData = ref([]) // 列表数据
const label = ref('') // 弹窗label
const refundId = ref('') // 退款id
const dataLoading = ref(false) // 列表数据加载loading
const DialogFormData = ref({}) // 弹窗表单内容
const title = ref('新建') // 弹窗标题
const url = ref('') // 当前路由
const initSearch = ref() // 条转过来的携带数据
const typeSelect = ref([]) // 服务类型下拉框数据
// 分页
const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1, // 默认当前页
  current: 1
})
const createDefaultRequestData = () => ({
  ordersStatus: '',
  id: '',
  contactsPhone: '',
  minCreateTime: '',
  maxCreateTime: '',
  isAsc1: false,
  orderBy1: 'createTime',
  pageNo: 1,
  pageSize: 10,
  payStatus: '',
  refundStatus: ''
})
const requestData = ref(createDefaultRequestData()) // 请求参数
// 搜索功能
const handleSearch = (val) => {
  const [minCreateDate, maxCreateDate] = val.createTime || []
  requestData.value = {
    ...createDefaultRequestData(),
    contactsPhone: val.contactsPhone,
    payStatus: val.payStatus,
    id: val.id,
    ordersStatus: val.ordersStatus,
    refundStatus: val.refundStatus,
    minCreateTime: minCreateDate ? minCreateDate + ' 00:00:00' : '',
    maxCreateTime: maxCreateDate ? maxCreateDate + ' 23:59:59' : ''
  }
  pagination.value.defaultCurrent = 1
  pagination.value.current = 1
  fetchData(requestData.value)
}
// 分页

// 重置，清空搜索框
const handleReset = () => {
  requestData.value = createDefaultRequestData()
  pagination.value.defaultCurrent = 1
  pagination.value.current = 1
  fetchData(requestData.value)
}
// 获取列表数据
const normalizeOrderQuery = (query) => {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null)
  )
}
const fetchData = async (query) => {
  dataLoading.value = true
  try {
    const res = await getOrderList(normalizeOrderQuery(query))
    if (res.code !== 200) {
      throw new Error(res.msg || res.message || '订单列表加载失败')
    }
    listData.value = res.data.list || []
    pagination.value.total = Number(res.data.total || 0)
  } catch (err) {
    console.error(err)
    listData.value = []
    pagination.value.total = 0
    MessagePlugin.error(err.message || '订单列表加载失败')
  } finally {
    dataLoading.value = false
  }
}
// 关闭弹窗
const handleClose = () => {
  visible.value = false // 关闭新增弹窗
}
// 确定提交
const submitRefund = async (val) => {
  await refundOrder({
    id: refundId.value,
    cancelReason: val.description
  })
    .then((res) => {
      if (res.data.code === 200) {
        MessagePlugin.success('已发起退款，请等待支付渠道处理')
        visible.value = false
        fetchData(requestData.value)
      } else {
        MessagePlugin.error(res.data.msg)
      }
    })
    .catch((err) => {
      console.log(err)
    })
}
// 排序
const handleSubmit = (val) => {
  DialogPlugin.confirm({
    header: '确认发起退款？',
    body: `退款原因：${val.description}`,
    confirmBtn: '确认退款',
    cancelBtn: '返回修改',
    onConfirm: () => submitRefund(val)
  })
}

const handleSortChange = (val) => {
  forEach(val, (item) => {
    if (item.sortBy === 'createTime') {
      if (item.descending === true) {
        requestData.value.isAsc1 = false
      } else {
        requestData.value.isAsc1 = true
      }
    }
  })
  fetchData(requestData.value)
}
// 打开退款弹窗
const handleClickRefund = (row) => {
  DialogFormData.value = { description: '' }
  visible.value = true
  refundId.value = row.id
  title.value = '填写退款原因'
  label.value = '退款原因：'
}
// 翻页
const onPageChange = (val) => {
  requestData.value.pageNo = val.defaultCurrent
  requestData.value.pageSize = val.defaultPageSize
  fetchData(requestData.value)
}
watch(
  () => route.path,
  (path) => {
    url.value = path
    if (path === '/order/orderList') {
      handleReset()
    }
  },
  { immediate: true }
)
</script>
<style lang="less" scoped>
.min-h {
  min-height: 720px !important;
}
</style>
