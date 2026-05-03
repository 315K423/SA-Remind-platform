// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /email/scan */
export async function scan(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.scanParams,
  options?: { [key: string]: any }
) {
  return request<any>('/email/scan', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
