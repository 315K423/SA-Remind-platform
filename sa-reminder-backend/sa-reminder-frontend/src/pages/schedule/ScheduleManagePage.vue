<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card title="日程管理" :bordered="false">
      <a-space direction="vertical" style="width: 100%" size="large">
        <a-alert type="info" show-icon :message="pageDesc.title" :description="pageDesc.desc" />

        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="标题">
            <a-input v-model:value="searchParams.title" placeholder="请输入日程标题" allow-clear />
          </a-form-item>

          <a-form-item label="类型">
            <a-select v-model:value="searchParams.scheduleType" style="width: 160px" allow-clear>
              <a-select-option value="personal">个人日程</a-select-option>
              <a-select-option value="meeting">会议安排</a-select-option>
              <a-select-option value="attendance">考勤事项</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-if="isAdminRole" label="部门">
            <a-select
                v-model:value="searchParams.departmentId"
                style="width: 180px"
                allow-clear
                :options="departmentOptions"
                show-search
                option-filter-prop="label"
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit">查询</a-button>
              <a-button @click="resetSearch">重置</a-button>
              <a-button @click="openAddModal">新建日程</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-table row-key="id" :columns="columns" :data-source="data" :pagination="pagination" @change="doTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'scheduleType'">
              <a-tag :color="scheduleTypeColorMap[record.scheduleType || 'personal']">
                {{ scheduleTypeTextMap[record.scheduleType || 'personal'] || record.scheduleType }}
              </a-tag>
            </template>

            <template v-else-if="column.dataIndex === 'status'">
              <a-badge
                  :status="(scheduleStatusColorMap[record.status || 'normal'] as any) || 'default'"
                  :text="scheduleStatusTextMap[record.status || 'normal'] || record.status || '正常'"
              />
            </template>

            <template v-else-if="column.dataIndex === 'startTime'">
              {{ formatDateTime(record.startTime) }}
            </template>

            <template v-else-if="column.dataIndex === 'endTime'">
              {{ formatDateTime(record.endTime) }}
            </template>

            <template v-else-if="column.dataIndex === 'participantUserIdList'">
              {{ record.participantUserIdList?.length || 0 }} 人
            </template>

            <template v-else-if="column.dataIndex === 'departmentIdList'">
              {{ getDepartmentNames(record.departmentIdList) }}
            </template>

            <template v-else-if="column.dataIndex === 'checkInEnabled'">
              <a-tag v-if="record.scheduleType === 'attendance'" :color="record.checkInEnabled === 1 ? 'green' : 'default'">
                {{ record.checkInEnabled === 1 ? '已启用签到' : '未启用签到' }}
              </a-tag>
              <span v-else>-</span>
            </template>

            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="openEditModal(record)" :disabled="!canEdit(record)">编辑</a-button>
                <a-popconfirm title="确认删除该日程吗？" @confirm="doDelete(record.id)">
                  <a-button type="link" danger :disabled="!canEdit(record)">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-space>
    </a-card>

    <a-modal :open="modalVisible" :title="isEdit ? '编辑日程' : '新建日程'" @ok="submitForm" @cancel="closeModal" width="960px" destroy-on-close>
      <a-form layout="vertical" :model="formState">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="日程标题" required>
              <a-input v-model:value="formState.title" placeholder="请输入日程标题" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="日程类型">
              <a-select v-model:value="formState.scheduleType" @change="handleScheduleTypeChange">
                <a-select-option value="personal">个人日程</a-select-option>
                <a-select-option value="meeting">会议安排</a-select-option>
                <a-select-option value="attendance">考勤事项</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="开始时间" required>
              <a-date-picker
                  v-model:value="formState.startTime"
                  show-time
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                  placeholder="请选择开始时间"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="结束时间">
              <a-date-picker
                  v-model:value="formState.endTime"
                  show-time
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                  placeholder="请选择结束时间"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="地点">
              <a-input v-model:value="formState.location" placeholder="请输入地点" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="可见范围">
              <a-select v-model:value="formState.visibility">
                <a-select-option value="private">仅自己可见</a-select-option>
                <a-select-option value="public">公开</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12" v-if="isEdit">
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option value="normal">正常</a-select-option>
                <a-select-option value="cancelled">已取消</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="全天日程">
              <a-switch
                  :checked="formState.allDay === 1"
                  checked-children="是"
                  un-checked-children="否"
                  @change="(checked: boolean) => (formState.allDay = checked ? 1 : 0)"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item v-if="isAdminRole" label="参与部门">
              <a-select
                  v-model:value="formState.departmentIdList"
                  mode="multiple"
                  :options="departmentOptions"
                  placeholder="请选择参与部门"
                  show-search
                  option-filter-prop="label"
                  @change="handleDepartmentChange"
              />
            </a-form-item>

            <a-form-item v-else-if="isManagerRole" label="参与部门">
              <a-input :value="loginUserStore.loginUser.departmentName || '当前所属部门'" disabled />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item label="参与员工">
              <a-select
                  v-model:value="formState.participantUserIdList"
                  mode="multiple"
                  :options="participantOptions"
                  placeholder="请选择参与员工"
                  show-search
                  option-filter-prop="label"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item label="内容说明">
              <a-textarea v-model:value="formState.content" :rows="4" placeholder="请输入日程说明" />
            </a-form-item>
          </a-col>

          <template v-if="isAttendanceSchedule">
            <a-col :span="24">
              <a-divider orientation="left">定位签到配置</a-divider>
            </a-col>
            <a-col :span="12">
              <a-form-item label="启用定位签到">
                <a-switch
                    :checked="formState.checkInEnabled === 1"
                    checked-children="启用"
                    un-checked-children="关闭"
                    @change="(checked: boolean) => (formState.checkInEnabled = checked ? 1 : 0)"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="签到半径（米）">
                <a-input-number
                    v-model:value="formState.checkInRadiusMeters"
                    :min="1"
                    :max="10000"
                    style="width: 100%"
                    placeholder="默认 200 米"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="签到地点描述">
                <a-input v-model:value="formState.checkInAddress" placeholder="例如：公司大楼一层前台" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="签到纬度">
                <a-input-number v-model:value="formState.checkInLatitude" :precision="10" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="签到经度">
                <a-input-number v-model:value="formState.checkInLongitude" :precision="10" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="快捷定位">
                <a-button block @click="fillCurrentLocation">使用当前浏览器定位</a-button>
              </a-form-item>
            </a-col>
          </template>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal :open="conflictModalVisible" title="检测到日程冲突" :footer="null" @cancel="conflictModalVisible = false">
      <a-alert type="warning" show-icon message="当前提交未通过，请调整时间或参与人后重试。" />
      <a-table row-key="scheduleId" :columns="conflictColumns" :data-source="conflictList" :pagination="false" style="margin-top: 16px" />
    </a-modal>
  </a-space>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { add, deleteUsingPost, getById, listPage, update } from '@/api/scheduleEventController'
import { listPage1 as listDepartmentPage } from '@/api/departmentController'
import { listUserVoByPage } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  canEditSchedule,
  formatDateTime,
  isAdmin,
  isManager,
  scheduleStatusColorMap,
  scheduleStatusTextMap,
  scheduleTypeColorMap,
  scheduleTypeTextMap,
} from '@/utils/app'

type DepartmentId = string

interface ScheduleSearchForm {
  pageNum: number
  pageSize: number
  sortField?: string
  sortOrder?: string
  title?: string
  scheduleType?: string
  departmentId?: DepartmentId
  participantUserId?: number
}

interface ScheduleFormState {
  id?: number
  title: string
  content?: string
  location?: string
  startTime?: string
  endTime?: string
  allDay: number
  scheduleType: string
  visibility: string
  status: string
  participantUserIdList: number[]
  departmentIdList: DepartmentId[]
  checkInEnabled: number
  checkInAddress?: string
  checkInLatitude?: number
  checkInLongitude?: number
  checkInRadiusMeters?: number
}

const route = useRoute()
const loginUserStore = useLoginUserStore()
const isAdminRole = computed(() => isAdmin(loginUserStore.loginUser.userRole))
const isManagerRole = computed(() => isManager(loginUserStore.loginUser.userRole))
const isAttendanceSchedule = computed(() => formState.scheduleType === 'attendance')

const columns = [
  { title: 'ID', dataIndex: 'id', width: 100 },
  { title: '标题', dataIndex: 'title' },
  { title: '类型', dataIndex: 'scheduleType', width: 120 },
  { title: '开始时间', dataIndex: 'startTime', width: 180 },
  { title: '结束时间', dataIndex: 'endTime', width: 180 },
  { title: '参与部门', dataIndex: 'departmentIdList', width: 180 },
  { title: '参与人数', dataIndex: 'participantUserIdList', width: 100 },
  { title: '签到配置', dataIndex: 'checkInEnabled', width: 130 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 },
]

const conflictColumns = [
  { title: '冲突类型', dataIndex: 'conflictType' },
  { title: '日程标题', dataIndex: 'title' },
  { title: '开始时间', dataIndex: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime' },
  { title: '关联用户', dataIndex: 'userName' },
]

const pageDesc = computed(() => {
  if (isAdminRole.value) {
    return {
      title: '管理员视角',
      desc: '可查看全部日程，创建或编辑时可按部门选择参与范围，再从这些部门中选择参与员工；考勤事项可配置定位签到参数。',
    }
  }
  if (isManagerRole.value) {
    return {
      title: '部门经理视角',
      desc: '只能查看自己所属部门相关日程，创建或编辑时可选择本部门员工参与。',
    }
  }
  return {
    title: '员工视角',
    desc: '可查看与自己相关的日程，并且只能修改自己创建的日程。',
  }
})

const data = ref<API.ScheduleEventVO[]>([])
const total = ref(0)
const modalVisible = ref(false)
const conflictModalVisible = ref(false)
const isEdit = ref(false)
const conflictList = ref<API.ScheduleConflictVO[]>([])

const departmentMap = ref<Record<string, string>>({})
const participantOptions = ref<{ label: string; value: number }[]>([])

const searchParams = reactive<ScheduleSearchForm>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'startTime',
  sortOrder: 'desc',
  title: '',
  scheduleType: undefined,
  departmentId: undefined,
})

const formState = reactive<ScheduleFormState>({
  id: undefined,
  title: '',
  content: '',
  location: '',
  startTime: undefined,
  endTime: undefined,
  allDay: 0,
  scheduleType: 'personal',
  visibility: 'private',
  status: 'normal',
  participantUserIdList: [],
  departmentIdList: [],
  checkInEnabled: 0,
  checkInAddress: '',
  checkInLatitude: undefined,
  checkInLongitude: undefined,
  checkInRadiusMeters: 200,
})

const departmentOptions = computed(() =>
    Object.entries(departmentMap.value).map(([value, label]) => ({
      label,
      value,
    })),
)

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const getDepartmentNames = (departmentIds?: Array<string | number>) => {
  if (!departmentIds || departmentIds.length === 0) return '-'
  return departmentIds.map((id) => departmentMap.value[String(id)] || `部门${id}`).join('、')
}

const resetAttendanceConfig = () => {
  formState.checkInEnabled = 0
  formState.checkInAddress = ''
  formState.checkInLatitude = undefined
  formState.checkInLongitude = undefined
  formState.checkInRadiusMeters = 200
}

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    title: '',
    content: '',
    location: '',
    startTime: undefined,
    endTime: undefined,
    allDay: 0,
    scheduleType: 'personal',
    visibility: 'private',
    status: 'normal',
    participantUserIdList: [],
    departmentIdList:
        isManagerRole.value && loginUserStore.loginUser.departmentId
            ? [String(loginUserStore.loginUser.departmentId)]
            : [],
    checkInEnabled: 0,
    checkInAddress: '',
    checkInLatitude: undefined,
    checkInLongitude: undefined,
    checkInRadiusMeters: 200,
  })
}

const fetchDepartments = async () => {
  const res = await listDepartmentPage({
    pageNum: 1,
    pageSize: 1000,
    sortField: 'id',
    sortOrder: 'asc',
  } as any)

  if (res.data.code === 0 && res.data.data) {
    const map: Record<string, string> = {}
    ;(res.data.data.records ?? []).forEach((item: any) => {
      if (item.id !== undefined && item.id !== null) {
        const id = String(item.id)
        map[id] = item.name || `部门${id}`
      }
    })
    departmentMap.value = map
  }
}

const loadParticipantOptions = async (departmentIds?: string[]) => {
  const ids = departmentIds?.length
      ? departmentIds
      : isManagerRole.value && loginUserStore.loginUser.departmentId
          ? [String(loginUserStore.loginUser.departmentId)]
          : []

  if (ids.length === 0 && !isAdminRole.value) {
    participantOptions.value = []
    return
  }

  const userMap = new Map<number, { label: string; value: number }>()

  if (ids.length === 0 && isAdminRole.value) {
    const res = await listUserVoByPage({
      pageNum: 1,
      pageSize: 1000,
      sortField: 'id',
      sortOrder: 'asc',
    } as any)

    if (res.data.code === 0 && res.data.data) {
      ;(res.data.data.records ?? []).forEach((item: any) => {
        if (item.id) {
          userMap.set(item.id, {
            label: `${item.userName || '未命名'}${item.departmentName ? `（${item.departmentName}）` : ''}`,
            value: item.id,
          })
        }
      })
    }
  } else {
    for (const departmentId of ids) {
      const res = await listUserVoByPage({
        pageNum: 1,
        pageSize: 1000,
        departmentId,
      } as any)

      if (res.data.code === 0 && res.data.data) {
        ;(res.data.data.records ?? []).forEach((item: any) => {
          if (item.id) {
            userMap.set(item.id, {
              label: `${item.userName || '未命名'}${item.departmentName ? `（${item.departmentName}）` : ''}`,
              value: item.id,
            })
          }
        })
      }
    }
  }

  participantOptions.value = Array.from(userMap.values())
}

const fetchData = async () => {
  const payload: Record<string, any> = { ...searchParams }

  if (isManagerRole.value && loginUserStore.loginUser.departmentId) {
    payload.departmentId = String(loginUserStore.loginUser.departmentId)
  }

  if (!isAdminRole.value && !isManagerRole.value && loginUserStore.loginUser.id) {
    payload.participantUserId = loginUserStore.loginUser.id
  }

  const res = await listPage(payload)

  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取日程失败')
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const resetSearch = () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    title: '',
    scheduleType: undefined,
    departmentId: undefined,
  })
  fetchData()
}

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const openAddModal = async () => {
  resetForm()
  isEdit.value = false
  modalVisible.value = true
  await loadParticipantOptions(formState.departmentIdList)
}

const openEditModal = async (record: API.ScheduleEventVO) => {
  Object.assign(formState, {
    id: record.id,
    title: record.title || '',
    content: record.content || '',
    location: record.location || '',
    startTime: record.startTime || undefined,
    endTime: record.endTime || undefined,
    allDay: record.allDay ?? 0,
    scheduleType: record.scheduleType || 'personal',
    visibility: record.visibility || 'private',
    status: record.status || 'normal',
    participantUserIdList: [...(record.participantUserIdList || [])],
    departmentIdList: (record.departmentIdList || []).map((id) => String(id)),
    checkInEnabled: record.checkInEnabled ?? 0,
    checkInAddress: record.checkInAddress || '',
    checkInLatitude: record.checkInLatitude,
    checkInLongitude: record.checkInLongitude,
    checkInRadiusMeters: record.checkInRadiusMeters ?? 200,
  })

  isEdit.value = true
  modalVisible.value = true
  await loadParticipantOptions(formState.departmentIdList)
}

const handleDepartmentChange = async (value?: string[]) => {
  formState.departmentIdList = value || []
  formState.participantUserIdList = []
  await loadParticipantOptions(value)
}

const handleScheduleTypeChange = (value: string) => {
  if (value !== 'attendance') {
    resetAttendanceConfig()
  }
}

const fillCurrentLocation = async () => {
  if (!navigator.geolocation) {
    message.error('当前浏览器不支持定位')
    return
  }
  navigator.geolocation.getCurrentPosition(
      (position) => {
        formState.checkInLatitude = position.coords.latitude
        formState.checkInLongitude = position.coords.longitude
        if (!formState.checkInAddress) {
          formState.checkInAddress = '浏览器当前定位点'
        }
        message.success('已填充当前位置坐标')
      },
      (error) => {
        message.error(error.message || '定位失败')
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      },
  )
}

const buildSubmitParams = () => {
  const payload: Record<string, any> = {
    ...formState,
    startTime: formState.startTime ? dayjs(formState.startTime).format('YYYY-MM-DDTHH:mm:ss') : undefined,
    endTime: formState.endTime ? dayjs(formState.endTime).format('YYYY-MM-DDTHH:mm:ss') : undefined,
    departmentIdList:
        isManagerRole.value && loginUserStore.loginUser.departmentId
            ? [String(loginUserStore.loginUser.departmentId)]
            : formState.departmentIdList,
  }

  if (payload.scheduleType !== 'attendance') {
    payload.checkInEnabled = 0
    payload.checkInAddress = undefined
    payload.checkInLatitude = undefined
    payload.checkInLongitude = undefined
    payload.checkInRadiusMeters = undefined
  }

  return payload
}

const submitForm = async () => {
  if (!formState.title || !formState.startTime) {
    message.warning('请填写完整的标题和开始时间')
    return
  }

  if (formState.endTime && dayjs(formState.endTime).isBefore(dayjs(formState.startTime))) {
    message.warning('结束时间不能早于开始时间')
    return
  }

  if (
      formState.scheduleType === 'attendance' &&
      formState.checkInEnabled === 1 &&
      (!formState.checkInLatitude || !formState.checkInLongitude)
  ) {
    message.warning('考勤事项启用定位签到时，请填写签到经纬度')
    return
  }

  const params = buildSubmitParams()
  const res = isEdit.value ? await update(params as any) : await add(params as any)

  if (res.data.code !== 0) {
    message.error(res.data.message || '提交失败')
    return
  }

  if (res.data.data?.conflictDetected) {
    conflictList.value = res.data.data.conflictList ?? []
    conflictModalVisible.value = true
    return
  }

  message.success(isEdit.value ? '更新成功' : '新增成功')
  closeModal()
  fetchData()
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

const canEdit = (record: API.ScheduleEventVO) => canEditSchedule(loginUserStore.loginUser, record)

const doDelete = async (id?: number) => {
  if (!id) return
  const res = await deleteUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const loadRouteEdit = async () => {
  const id = Number(route.query.id)
  if (!id) return
  const res = await getById({ id })
  if (res.data.code === 0 && res.data.data) {
    await openEditModal(res.data.data)
  }
}

onMounted(async () => {
  await Promise.all([fetchDepartments(), fetchData()])
  await loadParticipantOptions(formState.departmentIdList)
  await loadRouteEdit()
})
</script>
