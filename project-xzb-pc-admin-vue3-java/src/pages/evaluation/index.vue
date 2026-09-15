<template>
  <div class="evaluation-page">
    <div class="toolbar">
      <t-input v-model="query.ordersNo" placeholder="订单编号" clearable />
      <t-input v-model="query.consumerPhone" placeholder="客户电话" clearable />
      <t-select v-model="query.evaluationType" placeholder="评价类型" clearable><t-option :value="1" label="好评" /><t-option :value="2" label="差评" /></t-select>
      <t-select v-model="query.visibleStatus" placeholder="展示状态" clearable><t-option :value="1" label="展示中" /><t-option :value="0" label="已隐藏" /></t-select>
      <t-button theme="primary" @click="search">查询</t-button><t-button variant="outline" @click="reset">重置</t-button>
    </div>
    <t-table row-key="id" :data="rows" :columns="columns" :pagination="pagination" @page-change="changePage">
      <template #type="{ row }"><t-tag :theme="row.evaluationType === 1 ? 'success' : 'danger'">{{ row.evaluationType === 1 ? '好评' : '差评' }}</t-tag></template>
      <template #status="{ row }"><t-tag :theme="row.visibleStatus === 1 ? 'primary' : 'warning'">{{ row.visibleStatus === 1 ? '展示中' : '已隐藏' }}</t-tag></template>
      <template #action="{ row }"><t-link theme="primary" @click="showDetail(row.id)">查看</t-link><t-link class="action" :theme="row.visibleStatus === 1 ? 'danger' : 'primary'" @click="toggleVisibility(row)">{{ row.visibleStatus === 1 ? '隐藏' : '恢复' }}</t-link></template>
    </t-table>
    <t-dialog v-model:visible="detailVisible" header="评价详情" :footer="false" width="680px"><div v-if="detail" class="detail"><div><b>{{ detail.evaluationType === 1 ? '好评' : '差评' }}</b><span>{{ detail.serveItemName }}</span></div><p>{{ detail.content }}</p><div class="photos" v-if="detail.pictureArray?.length"><img v-for="url in detail.pictureArray" :key="url" :src="url" /></div><div class="meta">订单：{{ detail.ordersNo }}　客户：{{ detail.consumerName || '匿名客户' }} {{ detail.consumerPhone }}<br />服务人员：{{ detail.serveProviderName }}　提交时间：{{ formatTime(detail.createTime) }}</div></div></t-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { getEvaluationDetail, getEvaluationPage, updateEvaluationVisibility } from '@/api/evaluation'
const rows = ref<any[]>([]); const detail = ref<any>(null); const detailVisible = ref(false)
const query = reactive<any>({ pageNo: 1, pageSize: 10, ordersNo: '', consumerPhone: '', evaluationType: undefined, visibleStatus: undefined })
const pagination = reactive<any>({ current: 1, pageSize: 10, total: 0, showJumper: true })
const columns = [{ colKey: 'ordersNo', title: '订单编号', width: 190 }, { colKey: 'serveItemName', title: '服务名称', width: 140 }, { colKey: 'consumerName', title: '客户' }, { colKey: 'consumerPhone', title: '联系电话', width: 140 }, { colKey: 'serveProviderName', title: '服务人员', width: 130 }, { colKey: 'type', title: '评价', width: 90 }, { colKey: 'content', title: '评价内容', ellipsis: true }, { colKey: 'status', title: '状态', width: 100 }, { colKey: 'action', title: '操作', width: 130, fixed: 'right' }]
const body = (res:any) => res?.data?.data || res?.data || res || {}
const load = async () => { const page = body(await getEvaluationPage(query)); rows.value = page.list || []; pagination.total = Number(page.total || 0) }
const search = async () => { query.pageNo = 1; pagination.current = 1; try { await load() } catch (e:any) { MessagePlugin.error(e.message || '评价列表加载失败') } }
const reset = () => { Object.assign(query, { pageNo: 1, pageSize: 10, ordersNo: '', consumerPhone: '', evaluationType: undefined, visibleStatus: undefined }); search() }
const changePage = (page:any) => { query.pageNo = page.current; query.pageSize = page.pageSize; pagination.current = page.current; pagination.pageSize = page.pageSize; load() }
const showDetail = async (id:string) => { try { detail.value = body(await getEvaluationDetail(id)); detailVisible.value = true } catch (_) { MessagePlugin.error('评价详情加载失败') } }
const toggleVisibility = async (row:any) => { try { await updateEvaluationVisibility(row.id, row.visibleStatus === 1 ? 0 : 1); MessagePlugin.success(row.visibleStatus === 1 ? '已隐藏评价' : '已恢复评价'); load() } catch (_) { MessagePlugin.error('状态更新失败') } }
const formatTime = (value:string) => (value || '').replace('T', ' ').slice(0, 16)
onMounted(search)
</script>
<style scoped>.evaluation-page{padding:24px;background:#fff;min-height:calc(100vh - 120px)}.toolbar{display:flex;gap:14px;margin-bottom:24px}.toolbar :deep(.t-input),.toolbar :deep(.t-select){width:190px}.action{margin-left:14px}.detail p{line-height:1.8;white-space:pre-wrap}.detail b{color:#e65b43;margin-right:14px}.photos{display:flex;gap:10px;flex-wrap:wrap}.photos img{width:120px;height:120px;object-fit:cover;border-radius:8px}.meta{margin-top:22px;color:#7d7d7d;line-height:1.8}</style>
