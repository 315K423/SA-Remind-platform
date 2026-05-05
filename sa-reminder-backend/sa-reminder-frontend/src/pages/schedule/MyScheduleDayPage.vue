<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card :bordered="false">
      <a-row justify="space-between" align="middle">
        <a-col>
          <a-space>
            <a-button @click="goBack">返回月历</a-button>
            <a-date-picker v-model:value="selectedDate" value-format="YYYY-MM-DD" @change="handleDateChange" />
          </a-space>
        </a-col>
        <a-col>
          <div style="font-weight: 600">{{ selectedDate }}</div>
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false" title="当日日程（24 小时视图）">
      <a-empty v-if="hourRows.every((item) => item.scheduleList.length === 0)" description="当天暂无日程" />
      <div v-else class="day-view">
        <div v-for="row in hourRows" :key="row.hour" class="hour-row">
          <div class="hour-label">{{ row.hour }}</div>
          <div class="hour-content">
            <a-space direction="vertical" style="width: 100%">
              <a-card v-for="item in row.scheduleList" :key="item.id" size="small">
                <a-row justify="space-between" align="middle">
                  <a-col :span="18">
                    <a-space wrap>
                      <strong>{{ item.title }}</strong>
                      <a-tag :color="scheduleTypeColorMap[item.scheduleType || 'personal']">
                        {{ scheduleTypeTextMap[item.scheduleType || 'personal'] || item.scheduleType || '日程' }}
                      </a-tag>
                      <a-tag v-if="isExpired(item)" color="default">已过期</a-tag>
                      <span>{{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}</span>
                    </a-space>
                    <div style="margin-top: 8px; color: #8c8c8c">
                      {{ item.location || '未设置地点' }}
                    </div>
                  </a-col>
                  <a-col>
                    <a-space>
                      <a-button v-if="isExpired(item)" type="link" @click="openViewModal(item)">查看</a-button>
                      <a-button v-else type="link" @click="openEditModal(item)" :disabled="!canEdit(item)">编辑</a-button>
                      <a-popconfirm title="确认删除该日程吗？" @confirm="doDelete(item.id)">
                        <a-button type="link" danger :disabled="!canEdit(item)">删除</a-button>
                      </a-popconfirm>
                    </a-space>
                  </a-col>
                </a-row>
              </a-card>
            </a-space>
          </div>
        </div>
      </div>
    </a-card>

    <a-modal
      :open="modalVisible"
      title="编辑日程"
      @ok="submitForm"
      @cancel="closeModal"
      width="960px"
      destroy-on-close
    >
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
                :show-time="{ format: 'HH:mm' }"
                value-format="YYYY-MM-DDTHH:mm"
                format="YYYY-MM-DD HH:mm"
                style="width: 100%"
                placeholder="请选择开始时间"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="结束时间" required>
              <a-date-picker
                v-model:value="formState.endTime"
                :show-time="{ format: 'HH:mm' }"
                value-format="YYYY-MM-DDTHH:mm"
                format="YYYY-MM-DD HH:mm"
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
            <a-form-item label="可见性">
              <a-select v-model:value="formState.visibility">
                <a-select-option value="private">仅参与人可见</a-select-option>
                <a-select-option value="public">公开可见</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
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

    <a-modal :open="viewModalVisible" title="查看日程" :footer="null" @cancel="closeViewModal" width="720px">
      <a-descriptions v-if="viewRecord" bordered :column="1">
        <a-descriptions-item label="日程标题">{{ viewRecord.title || '-' }}</a-descriptions-item>
        <a-descriptions-item label="日程类型">
          {{ scheduleTypeTextMap[viewRecord.scheduleType || 'personal'] || viewRecord.scheduleType || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          {{ scheduleStatusTextMap[viewRecord.status || 'normal'] || viewRecord.status || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ formatDateTime(viewRecord.startTime) }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ formatDateTime(viewRecord.endTime) }}</a-descriptions-item>
        <a-descriptions-item label="地点">{{ viewRecord.location || '未设置地点' }}</a-descriptions-item>
        <a-descriptions-item label="参与部门">{{ getDepartmentNames(viewRecord.departmentIdList) }}</a-descriptions-item>
        <a-descriptions-item label="参与人数">{{ viewRecord.participantUserIdList?.length || 0 }} 人</a-descriptions-item>
        <a-descriptions-item label="内容说明">{{ viewRecord.content || '暂无内容' }}</a-descriptions-item>
        <a-descriptions-item v-if="viewRecord.scheduleType === 'attendance'" label="签到配置">
          <div>是否启用：{{ viewRecord.checkInEnabled === 1 ? '已启用' : '未启用' }}</div>
          <div>签到地点：{{ viewRecord.checkInAddress || '-' }}</div>
          <div>签到半径：{{ viewRecord.checkInRadiusMeters || 200 }} 米</div>
          <div>坐标：{{ viewRecord.checkInLatitude ?? '-' }}，{{ viewRecord.checkInLongitude ?? '-' }}</div>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <a-modal :open="conflictModalVisible" title="检测到日程冲突" :footer="null" @cancel="conflictModalVisible = false">
      <a-alert type="warning" show-icon message="当前提交未通过，请调整时间或参与人后重试。" />
      <a-table
        row-key="scheduleId"
        :columns="conflictColumns"
        :data-source="conflictList"
        :pagination="false"
        style="margin-top: 16px"
      />
    </a-modal>
  </a-space>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { deleteUsingPost, getMyDay, update } from '@/api/scheduleEventController'
import { listPage1 as listDepartmentPage } from '@/api/departmentController'
import { listUserVoByPage } from '@/api/userController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  canEditSchedule,
  formatDateTime,
  isAdmin,
  isManager,
  scheduleStatusTextMap,
  scheduleTypeColorMap,
  scheduleTypeTextMap,
} from '@/utils/app'

type IdValue = string
type DepartmentId = string

interface ScheduleFormState {
  id?: IdValue
  title: string
  content?: string
  location?: string
  startTime?: string
  endTime?: string
  allDay: number
  scheduleType: string
  visibility: string
  status: string
  participantUserIdList: IdValue[]
  departmentIdList: DepartmentId[]
  checkInEnabled: number
  checkInAddress?: string
  checkInLatitude?: number
  checkInLongitude?: number
  checkInRadiusMeters?: number
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const selectedDate = ref<string>((route.query.date as string) || dayjs().format('YYYY-MM-DD'))
const scheduleList = ref<API.ScheduleEventVO[]>([])
const modalVisible = ref(false)
const viewModalVisible = ref(false)
const conflictModalVisible = ref(false)
const conflictList = ref<API.ScheduleConflictVO[]>([])
const viewRecord = ref<API.ScheduleEventVO>()
const departmentMap = ref<Record<string, string>>({})
const participantOptions = ref<{ label: string; value: IdValue }[]>([])

const toIdString = (id?: string | number | null) => {
  if (id === undefined || id === null) return undefined
  return String(id)
}

const normalizeIdList = (ids?: Array<string | number>) => ids?.map((id) => String(id)) ?? []

const toMinuteValue = (value?: string) => {
  if (!value) return undefined
  return dayjs(value).format('YYYY-MM-DDTHH:mm')
}

const isAdminRole = computed(() => isAdmin(loginUserStore.loginUser.userRole))
const isManagerRole = computed(() => isManager(loginUserStore.loginUser.userRole))
const isAttendanceSchedule = computed(() => formState.scheduleType === 'attendance')

const conflictColumns = [
  { title: '冲突类型', dataIndex: 'conflictType' },
  { title: '日程标题', dataIndex: 'title' },
  { title: '开始时间', dataIndex: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime' },
  { title: '关联用户', dataIndex: 'userName' },
]

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

const fetchData = async () => {
  const res = await getMyDay({ date: selectedDate.value })
  if (res.data.code === 0) {
    scheduleList.value = res.data.data?.scheduleList ?? []
  } else {
    message.error(res.data.message || '获取当日日程失败')
  }
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

  const userMap = new Map<IdValue, { label: string; value: IdValue }>()

  if (ids.length === 0 && isAdminRole.value) {
    const res = await listUserVoByPage({
      pageNum: 1,
      pageSize: 1000,
      sortField: 'id',
      sortOrder: 'asc',
    } as any)

    if (res.data.code === 0 && res.data.data) {
      ;(res.data.data.records ?? []).forEach((item: any) => {
        if (item.id !== undefined && item.id !== null) {
          const id = String(item.id)
          userMap.set(id, {
            label: `${item.userName || '未命名'}${item.departmentName ? `（${item.departmentName}）` : ''}`,
            value: id,
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
          if (item.id !== undefined && item.id !== null) {
            const id = String(item.id)
            userMap.set(id, {
              label: `${item.userName || '未命名'}${item.departmentName ? `（${item.departmentName}）` : ''}`,
              value: id,
            })
          }
        })
      }
    }
  }

  participantOptions.value = Array.from(userMap.values())
}

const hourRows = computed(() => {
  return Array.from({ length: 24 }, (_, index) => {
    const hour = `${String(index).padStart(2, '0')}:00`
    const list = scheduleList.value.filter((item) => {
      if (!item.startTime) return index === 0
      return dayjs(item.startTime).hour() === index
    })
    return { hour, scheduleList: list }
  })
})

const getDepartmentNames = (departmentIds?: Array<string | number>) => {
  if (!departmentIds || departmentIds.length === 0) return '-'
  return departmentIds.map((id) => departmentMap.value[String(id)] || `部门${id}`).join('、')
}

const canEdit = (record: API.ScheduleEventVO) => canEditSchedule(loginUserStore.loginUser, record)

const isExpired = (record: API.ScheduleEventVO) => {
  if (!record.endTime) return false
  return dayjs(record.endTime).isBefore(dayjs())
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

const openEditModal = async (record: API.ScheduleEventVO) => {
  const recordId = toIdString(record.id as any)
  if (!recordId) return
  Object.assign(formState, {
    id: recordId,
    title: record.title || '',
    content: record.content || '',
    location: record.location || '',
    startTime: toMinuteValue(record.startTime),
    endTime: toMinuteValue(record.endTime),
    allDay: record.allDay ?? 0,
    scheduleType: record.scheduleType || 'personal',
    visibility: record.visibility || 'private',
    status: record.status || 'normal',
    participantUserIdList: normalizeIdList(record.participantUserIdList as any),
    departmentIdList: normalizeIdList(record.departmentIdList as any),
    checkInEnabled: record.checkInEnabled ?? 0,
    checkInAddress: record.checkInAddress || '',
    checkInLatitude: record.checkInLatitude,
    checkInLongitude: record.checkInLongitude,
    checkInRadiusMeters: record.checkInRadiusMeters ?? 200,
  })
  modalVisible.value = true
  await loadParticipantOptions(formState.departmentIdList)
}

const openViewModal = (record: API.ScheduleEventVO) => {
  viewRecord.value = record
  viewModalVisible.value = true
}

const closeViewModal = () => {
  viewModalVisible.value = false
  viewRecord.value = undefined
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
    startTime: formState.startTime ? dayjs(formState.startTime).format('YYYY-MM-DDTHH:mm') : undefined,
    endTime: formState.endTime ? dayjs(formState.endTime).format('YYYY-MM-DDTHH:mm') : undefined,
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
  if (!formState.id) {
    message.warning('日程 id 不存在，无法修改')
    return
  }
  if (!formState.title || !formState.startTime || !formState.endTime) {
    message.warning('请填写完整的标题、开始时间和结束时间')
    return
  }

  if (!dayjs(formState.endTime).isAfter(dayjs(formState.startTime))) {
    message.warning('结束时间必须晚于开始时间')
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

  const res = await update(buildSubmitParams() as any)

  if (res.data.code !== 0) {
    message.error(res.data.message || '提交失败')
    return
  }

  if (res.data.data?.conflictDetected) {
    conflictList.value = res.data.data.conflictList ?? []
    conflictModalVisible.value = true
    return
  }

  message.success('更新成功')
  closeModal()
  await fetchData()
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

const doDelete = async (id?: string | number) => {
  const idValue = toIdString(id)
  if (!idValue) return
  const res = await deleteUsingPost({ id: idValue } as any)
  if (res.data.code === 0) {
    message.success('删除成功')
    await fetchData()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const handleDateChange = async (value?: string) => {
  selectedDate.value = value || dayjs().format('YYYY-MM-DD')
  await fetchData()
}

const goBack = () => {
  router.push('/')
}

onMounted(async () => {
  await Promise.all([fetchDepartments(), fetchData()])
  await loadParticipantOptions(formState.departmentIdList)
})
</script>

<style scoped>
.day-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hour-row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 16px;
}
.hour-label {
  color: #8c8c8c;
  padding-top: 8px;
}
.hour-content {
  min-height: 60px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
</style>
