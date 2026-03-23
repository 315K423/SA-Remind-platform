// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /schedule/reminder/admin/scanNow */
export async function scanNow(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/schedule/reminder/admin/scanNow', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /schedule/reminder/popup/list */
export async function listPopupTasks(options?: { [key: string]: any }) {
  return request<API.BaseResponseListScheduleReminderPopupVO>('/schedule/reminder/popup/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /schedule/reminder/popup/read */
export async function readPopup(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/schedule/reminder/popup/read', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /schedule/reminder/popup/read/all */
export async function readAllPopup(
  body: API.ScheduleReminderPopupReadAllRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/schedule/reminder/popup/read/all', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /schedule/reminder/rule/delete */
export async function deleteRule(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/schedule/reminder/rule/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /schedule/reminder/rule/get */
export async function getRuleByScheduleId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRuleByScheduleIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseScheduleReminderRuleVO>('/schedule/reminder/rule/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /schedule/reminder/rule/list/page/vo */
export async function listRuleByPage(
  body: API.ScheduleReminderRuleQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageScheduleReminderRuleVO>(
    '/schedule/reminder/rule/list/page/vo',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: body,
      ...(options || {}),
    }
  )
}

/** 此处后端没有提供注释 POST /schedule/reminder/rule/save */
export async function saveRule(
  body: API.ScheduleReminderRuleSaveRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/schedule/reminder/rule/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
