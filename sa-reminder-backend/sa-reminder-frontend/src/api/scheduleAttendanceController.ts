// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /schedule/attendance/admin/list/page/vo */
export async function listAttendancePage(
  body: API.ScheduleAttendanceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageScheduleAttendanceVO>(
    '/schedule/attendance/admin/list/page/vo',
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

/** 此处后端没有提供注释 POST /schedule/attendance/admin/updateStatus */
export async function updateAttendanceStatus(
  body: API.ScheduleAttendanceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/schedule/attendance/admin/updateStatus', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /schedule/attendance/checkIn */
export async function checkIn(
  body: API.ScheduleAttendanceCheckInRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseScheduleAttendanceCheckInVO>('/schedule/attendance/checkIn', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
