<template>
  <a-modal
    :open="visible"
    title="日程临期提醒"
    ok-text="标记已读"
    cancel-text="稍后处理"
    @ok="handleReadCurrent"
    @cancel="handleClose"
  >
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
        </a-descriptions>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { listPopupTasks, readAllPopup, readPopup } from '@/api/scheduleReminderController'
import { message } from 'ant-design-vue'

const taskList = ref<API.ScheduleReminderPopupVO[]>([])
const visible = ref(false)
let timer: number | undefined

const currentTask = computed(() => taskList.value[0])

const loadTasks = async () => {
  const res = await listPopupTasks()
  if (res.data.code === 0) {
    taskList.value = res.data.data ?? []
  }
}

const handleReadCurrent = async () => {
  const id = currentTask.value?.id
  if (!id) {
    visible.value = false
    return
  }
  const res = await readPopup({ id })
  if (res.data.code === 0) {
    message.success('已标记为已读')
    taskList.value = taskList.value.filter((item) => item.id !== id)
  }
}

const handleClose = async () => {
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
  timer = window.setInterval(loadTasks, 30000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>
