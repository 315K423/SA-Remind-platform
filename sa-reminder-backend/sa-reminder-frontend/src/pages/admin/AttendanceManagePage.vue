<template>
  <a-card title="考勤管理" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-alert
        type="info"
        show-icon
        message="定位签到管理说明"
        description="管理员可查看所有考勤事项的签到结果，并手动将参与人的考勤状态调整为“考勤成功”或“未考勤”。"
      />

      <a-form layout="inline" :model="searchParams" @finish="fetchData">
        <a-form-item label="日程标题">
          <a-input v-model:value="searchParams.scheduleTitle" allow-clear placeholder="请输入日程标题" />
        </a-form-item>
        <a-form-item label="用户名称">
          <a-input v-model:value="searchParams.userName" allow-clear placeholder="请输入用户名称" />
        </a-form-item>
        <a-form-item label="考勤状态">
          <a-select v-model:value="searchParams.attendanceStatus" style="width: 160px" allow-clear>
            <a-select-option value="checked_in">考勤成功</a-select-option>
            <a-select-option value="not_checked">未考勤</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="resetSearch">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table row-key="participantId" :columns="columns" :data-source="data" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'scheduleStartTime'">
            {{ formatDateTime(record.scheduleStartTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'attendanceStatus'">
            <a-badge
              :status="(attendanceStatusColorMap[record.attendanceStatus || 'not_checked'] as any) || 'default'"
              :text="attendanceStatusTextMap[record.attendanceStatus || 'not_checked'] || record.attendanceStatus || '-'"
            />
          </template>
          <template v-else-if="column.dataIndex === 'checkInTime'">
            {{ formatDateTime(record.checkInTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openModal(record, 'checked_in')">标记成功</a-button>
              <a-button type="link" danger @click="openModal(record, 'not_checked')">标记未考勤</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-space>

    <a-modal :open="modalVisible" title="修改考勤状态" @ok="submitUpdate" @cancel="closeModal">
      <a-form layout="vertical" :model="formState">
        <a-form-item label="参与人ID">
          <a-input :value="formState.participantId" disabled />
        </a-form-item>
        <a-form-item label="考勤状态">
          <a-select v-model:value="formState.attendanceStatus">
            <a-select-option value="checked_in">考勤成功</a-select-option>
            <a-select-option value="not_checked">未考勤</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { listAttendancePage, updateAttendanceStatus } from '@/api/scheduleAttendanceController'
import { message } from 'ant-design-vue'
import { attendanceStatusColorMap, attendanceStatusTextMap, formatDateTime } from '@/utils/app'

const columns = [
  { title: '参与记录ID', dataIndex: 'participantId', width: 110 },
  { title: '日程标题', dataIndex: 'scheduleTitle' },
  { title: '开始时间', dataIndex: 'scheduleStartTime', width: 180 },
  { title: '用户名称', dataIndex: 'userName', width: 140 },
  { title: '参与角色', dataIndex: 'participantRole', width: 110 },
  { title: '考勤状态', dataIndex: 'attendanceStatus', width: 120 },
  { title: '签到时间', dataIndex: 'checkInTime', width: 180 },
  { title: '签到地址', dataIndex: 'checkInAddress', width: 180 },
  { title: '签到距离(米)', dataIndex: 'checkInDistanceMeters', width: 120 },
  { title: '操作', key: 'action', width: 160 },
]

const data = ref<API.ScheduleAttendanceVO[]>([])
const total = ref(0)
const modalVisible = ref(false)

const searchParams = reactive<API.ScheduleAttendanceQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  scheduleTitle: '',
  userName: '',
  attendanceStatus: undefined,
})

const formState = reactive<API.ScheduleAttendanceUpdateRequest>({
  participantId: undefined,
  attendanceStatus: 'checked_in',
})

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const fetchData = async () => {
  const res = await listAttendancePage({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取考勤列表失败')
  }
}

const resetSearch = async () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    scheduleTitle: '',
    userName: '',
    attendanceStatus: undefined,
  })
  await fetchData()
}

const onTableChange = async (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  await fetchData()
}

const openModal = (record: API.ScheduleAttendanceVO, status: string) => {
  formState.participantId = record.participantId
  formState.attendanceStatus = status
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  formState.participantId = undefined
  formState.attendanceStatus = 'checked_in'
}

const submitUpdate = async () => {
  const res = await updateAttendanceStatus({ ...formState })
  if (res.data.code === 0) {
    message.success('考勤状态更新成功')
    closeModal()
    await fetchData()
  } else {
    message.error(res.data.message || '更新失败')
  }
}

onMounted(fetchData)
</script>
