<template>
  <a-card title="弹窗提醒中心" :bordered="false">
    <a-space direction="vertical" style="width: 100%" size="large">
      <a-alert
          type="info"
          show-icon
          message="提醒确认说明"
          description="只有日程参与人会收到提醒。普通提醒点击“确认收到”即可完成处理；考勤事项会显示“定位签到”，定位成功后会自动完成提醒确认。"
      />
      <a-space>
        <a-button type="primary" @click="fetchData">刷新</a-button>
        <a-button v-if="isAdminRole" @click="runScanNow" :loading="scanLoading">管理员自检</a-button>
        <a-button @click="readAll" :disabled="attendanceTaskCount > 0">全部确认</a-button>
      </a-space>

      <a-table row-key="id" :columns="columns" :data-source="data" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'taskStatus'">
            <a-badge
                :status="record.taskStatus === 'sent' ? 'processing' : 'success'"
                :text="record.taskStatus || '-'"
            />
          </template>
          <template v-else-if="column.dataIndex === 'attendanceCheckRequired'">
            <a-tag :color="record.attendanceCheckRequired ? 'orange' : 'blue'">
              {{ record.attendanceCheckRequired ? '需要签到' : '普通提醒' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
                v-if="record.attendanceCheckRequired"
                type="link"
                @click="checkIn(record.id)"
            >
              定位签到
            </a-button>
            <a-button v-else type="link" @click="readOne(record.id)">确认收到</a-button>
          </template>
        </template>
      </a-table>
    </a-space>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listPopupTasks, readAllPopup, readPopup, scanNow } from '@/api/scheduleReminderController'
import { checkIn as checkInAttendance } from '@/api/scheduleAttendanceController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { isAdmin } from '@/utils/app'

const columns = [
  { title: '任务 ID', dataIndex: 'id' },
  { title: '日程 ID', dataIndex: 'scheduleId' },
  { title: '标题', dataIndex: 'popupTitle' },
  { title: '内容', dataIndex: 'popupContent' },
  { title: '计划提醒时间', dataIndex: 'plannedRemindTime' },
  { title: '提醒类型', dataIndex: 'attendanceCheckRequired' },
  { title: '状态', dataIndex: 'taskStatus' },
  { title: '操作', key: 'action' },
]

const loginUserStore = useLoginUserStore()
const isAdminRole = computed(() => isAdmin(loginUserStore.loginUser.userRole))
const data = ref<API.ScheduleReminderPopupVO[]>([])
const scanLoading = ref(false)

const attendanceTaskCount = computed(
    () => data.value.filter((item) => item.attendanceCheckRequired).length,
)

const fetchData = async () => {
  const res = await listPopupTasks()
  if (res.data.code === 0) {
    data.value = res.data.data ?? []
  } else {
    message.error(res.data.message || '获取弹窗提醒失败')
  }
}

const getBrowserLocation = () => {
  return new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          })
        },
        (error) => reject(new Error(error.message || '定位失败')),
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0,
        },
    )
  })
}

const readOne = async (id?: number) => {
  if (!id) return
  const res = await readPopup({ id })
  if (res.data.code === 0) {
    message.success('已确认收到')
    fetchData()
  } else {
    message.error(res.data.message || '处理失败')
  }
}

const checkIn = async (id?: number) => {
  if (!id) return
  try {
    const location = await getBrowserLocation()
    const res = await checkInAttendance({
      taskId: id,
      latitude: location.latitude,
      longitude: location.longitude,
    })
    if (res.data.code === 0 && res.data.data?.success) {
      message.success(res.data.data.message || '签到成功')
      fetchData()
    } else {
      message.error(res.data.data?.message || res.data.message || '签到失败')
    }
  } catch (error: any) {
    message.error(error?.message || '无法获取浏览器定位')
  }
}

const readAll = async () => {
  const ids = data.value
      .filter((item) => !item.attendanceCheckRequired)
      .map((item) => item.id)
      .filter(Boolean) as number[]
  const res = await readAllPopup({ taskIdList: ids })
  if (res.data.code === 0) {
    message.success('全部确认成功')
    fetchData()
  } else {
    message.error(res.data.message || '批量处理失败')
  }
}

const runScanNow = async () => {
  scanLoading.value = true
  try {
    const res = await scanNow()
    if (res.data.code === 0) {
      message.success('提醒自检完成')
      await fetchData()
    } else {
      message.error(res.data.message || '自检失败')
    }
  } finally {
    scanLoading.value = false
  }
}

onMounted(fetchData)
</script>
