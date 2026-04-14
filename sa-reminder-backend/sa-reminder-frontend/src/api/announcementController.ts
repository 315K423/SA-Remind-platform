// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /announcement/add */
export async function addAnnouncement(
  body: API.AnnouncementAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/announcement/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /announcement/delete */
export async function deleteAnnouncement(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/announcement/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /announcement/list/page/vo */
export async function listPage2(
  body: API.AnnouncementQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAnnouncementVO>('/announcement/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /announcement/my/list */
export async function myList(options?: { [key: string]: any }) {
  return request<API.BaseResponseListAnnouncementVO>('/announcement/my/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /announcement/read */
export async function read(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/announcement/read', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /announcement/update */
export async function updateAnnouncement(
  body: API.AnnouncementUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/announcement/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
