import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, ChannelQueryParams, MallPaymentChannel } from '@/types'

// 查询支付渠道列表
export function listChannel(query: ChannelQueryParams): Promise<TableDataInfo<MallPaymentChannel[]>> {
  return request({
    url: '/mall-payment/channel/list',
    method: 'get',
    params: query
  })
}

// 查询支付渠道详细
export function getChannel(id: number): Promise<AjaxResult<MallPaymentChannel>> {
  return request({
    url: '/mall-payment/channel/' + id,
    method: 'get'
  })
}

// 新增支付渠道
export function addChannel(data: MallPaymentChannel): Promise<AjaxResult> {
  return request({
    url: '/mall-payment/channel',
    method: 'post',
    data: data
  })
}

// 修改支付渠道
export function updateChannel(data: MallPaymentChannel): Promise<AjaxResult> {
  return request({
    url: '/mall-payment/channel',
    method: 'put',
    data: data
  })
}

// 删除支付渠道
export function delChannel(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mall-payment/channel/' + id,
    method: 'delete'
  })
}


