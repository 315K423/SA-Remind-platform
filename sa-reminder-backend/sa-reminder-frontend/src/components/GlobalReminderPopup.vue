<template>
  <a-modal :open="visible" :footer="null" title="日程临期提醒" @cancel="handleClose">
    <div v-if="currentTask">
      <a-space direction="vertical" style="width: 100%" size="middle">
        <a-alert :message="currentTask.popupTitle || '日程提醒'" type="warning" show-icon />

        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="提醒内容">
            {{ currentTask.popupContent || '您有一条日程即将开始，请及时查看。' }}
          </a-descriptions-item>
          <a-descriptions-item label="计划提醒时间">
            {{ currentTask.plannedRemindTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="提醒序号">
            第 {{ (currentTask.remindIndex ?? 0) + 1 }} 次
          </a-descriptions-item>
          <a-descriptions-item v-if="currentTask.attendanceCheckRequired" label="签到地点">
            {{ currentTask.checkInAddress || '未设置' }}
          </a-descriptions-item>
          <a-descriptions-item v-if="currentTask.attendanceCheckRequired" label="签到半径">
            {{ currentTask.checkInRadiusMeters || 200 }} 米
          </a-descriptions-item>
        </a-descriptions>

        <a-alert
            v-if="currentTask.attendanceCheckRequired"
            type="info"
            show-icon
            message="该提醒需要定位签到"
            description="点击“定位签到”后浏览器会申请定位权限。定位成功且与设定签到点距离在允许范围内，将自动完成本次提醒确认。"
        />
        <a-alert
            v-else
            type="info"
            show-icon
            message="未确认时会继续提醒"
            description="如果你关闭弹窗但没有确认，系统会根据提醒规则在下一次间隔继续提醒。"
        />

        <a-space style="justify-content: flex-end; width: 100%">
          <a-button @click="handleClose">稍后处理</a-button>
          <a-button
              v-if="currentTask.attendanceCheckRequired"
              type="primary"
              :loading="actionLoading"
              @click="handleCheckIn"
          >
            定位签到
          </a-button>
          <a-button v-else type="primary" :loading="actionLoading" @click="handleReadCurrent">
            确认收到
          </a-button>
        </a-space>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { listPopupTasks, readPopup } from '@/api/scheduleReminderController'
import { checkIn as checkInAttendance } from '@/api/scheduleAttendanceController'
import { message } from 'ant-design-vue'

type ReminderPopupTask = API.ScheduleReminderPopupVO

const taskList = ref<ReminderPopupTask[]>([])
const visible = ref(false)
const actionLoading = ref(false)
let timer: number | undefined

const currentTask = computed(() => taskList.value[0])

const loadTasks = async () => {
  const res = await listPopupTasks()
  if (res.data.code === 0) {
    taskList.value = res.data.data ?? []
  }
}

const removeCurrentTask = () => {
  const id = currentTask.value?.id
  if (!id) {
    visible.value = false
    return
  }
  taskList.value = taskList.value.filter((item) => item.id !== id)
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

const handleReadCurrent = async () => {
  const id = currentTask.value?.id
  if (!id) {
    visible.value = false
    return
  }
  actionLoading.value = true
  try {
    const res = await readPopup({ id })
    if (res.data.code === 0) {
      message.success('已确认收到提醒')
      removeCurrentTask()
    } else {
      message.error(res.data.message || '确认失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleCheckIn = async () => {
  const id = currentTask.value?.id
  if (!id) return
  actionLoading.value = true
  try {
    const location = await getBrowserLocation()
    const res = await checkInAttendance({
      taskId: id,
      latitude: location.latitude,
      longitude: location.longitude,
    })
    if (res.data.code === 0 && res.data.data?.success) {
      message.success(res.data.data.message || '签到成功')
      removeCurrentTask()
    } else {
      message.error(res.data.data?.message || res.data.message || '签到失败')
      await loadTasks()
    }
  } catch (error: any) {
    message.error(error?.message || '无法获取浏览器定位')
  } finally {
    actionLoading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

watch(
    taskList,
    (newVal) => {
      visible.value = newVal.length > 0
    },
    { deep: true },
)

onMounted(() => {
  loadTasks()
  timer = window.setInterval(loadTasks, 15000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>
