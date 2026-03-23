<template>
  <a-card title="弹窗提醒中心" :bordered="false">
    <a-space direction="vertical" style="width: 100%" size="large">
      <a-space>
        <a-button type="primary" @click="fetchData">刷新</a-button>
        <a-button @click="readAll">全部已读</a-button>
      </a-space>

      <a-table row-key="id" :columns="columns" :data-source="data" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'taskStatus'">
            <a-badge :status="record.taskStatus === 'sent' ? 'processing' : 'success'" :text="record.taskStatus || '-'" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="readOne(record.id)">标记已读</a-button>
          </template>
        </template>
      </a-table>
    </a-space>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listPopupTasks, readAllPopup, readPopup } from '@/api/scheduleReminderController'
import { message } from 'ant-design-vue'

const columns = [
  { title: '任务 ID', dataIndex: 'id' },
  { title: '日程 ID', dataIndex: 'scheduleId' },
  { title: '标题', dataIndex: 'popupTitle' },
  { title: '内容', dataIndex: 'popupContent' },
  { title: '计划提醒时间', dataIndex: 'plannedRemindTime' },
  { title: '实际提醒时间', dataIndex: 'actualRemindTime' },
  { title: '状态', dataIndex: 'taskStatus' },
  { title: '操作', key: 'action' },
]

const data = ref<API.ScheduleReminderPopupVO[]>([])

const fetchData = async () => {
  const res = await listPopupTasks()
  if (res.data.code === 0) {
    data.value = res.data.data ?? []
  } else {
    message.error(res.data.message || '获取弹窗提醒失败')
  }
}

const readOne = async (id?: number) => {
  if (!id) return
  const res = await readPopup({ id })
  if (res.data.code === 0) {
    message.success('已读成功')
    fetchData()
  } else {
    message.error(res.data.message || '处理失败')
  }
}

const readAll = async () => {
  const ids = data.value.map((item) => item.id).filter(Boolean) as number[]
  const res = await readAllPopup({ taskIdList: ids })
  if (res.data.code === 0) {
    message.success('全部已读成功')
    fetchData()
  } else {
    message.error(res.data.message || '批量处理失败')
  }
}

onMounted(fetchData)
</script>
