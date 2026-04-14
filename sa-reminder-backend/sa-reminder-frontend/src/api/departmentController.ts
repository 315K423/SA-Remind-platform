// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /department/add */
export async function addDepart(body: API.DepartmentAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/department/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/assign/users */
export async function assignUsers(
  body: API.DepartmentAssignUsersRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/department/assign/users', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/delete */
export async function deleteDepart(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/department/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /department/get */
export async function getById1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getById1Params,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseDepartmentVO>('/department/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/import/users */
export async function importUsers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.importUsersParams,
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData()

  if (file) {
    formData.append('file', file)
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele]

    if (item !== undefined && item !== null) {
      if (typeof item === 'object' && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ''))
        } else {
          formData.append(ele, new Blob([JSON.stringify(item)], { type: 'application/json' }))
        }
      } else {
        formData.append(ele, item)
      }
    }
  })

  return request<API.BaseResponseListUserVO>('/department/import/users', {
    method: 'POST',
    params: {
      ...params,
    },
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/list/page/vo */
export async function listPage1(
  body: API.DepartmentQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageDepartmentVO>('/department/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/transfer/users */
export async function transferUsers(
  body: API.DepartmentTransferUsersRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/department/transfer/users', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /department/update */
export async function updateDepart(
  body: API.DepartmentUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/department/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
