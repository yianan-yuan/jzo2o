import { request } from '@/utils/request'

export const getEvaluationPage = (params: any) => request.get({
  url: '/customer/operation/evaluation/page', params
})
export const getEvaluationDetail = (id: string) => request.get({
  url: `/customer/operation/evaluation/${id}`
})
export const updateEvaluationVisibility = (id: string, visibleStatus: number) => request.put({
  url: `/customer/operation/evaluation/${id}/visibility`, data: { visibleStatus }
})
export const getProviderEvaluationSummary = (serveProviderId: string | number) => request.get({
  url: '/customer/operation/evaluation/summary', params: { serveProviderId }
})
