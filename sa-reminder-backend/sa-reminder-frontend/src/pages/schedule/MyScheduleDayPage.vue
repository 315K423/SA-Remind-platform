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
                      <a-tag>{{ item.scheduleType || 'schedule' }}</a-tag>
                      <span>{{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}</span>
                    </a-space>
                    <div style="margin-top: 8px; color: #8c8c8c">
                      {{ item.location || '未设置地点' }}
                    </div>
                  </a-col>
                  <a-col>
                    <a-space>
                      <a-button type="link" @click="openEdit(item)" :disabled="!canEdit(item)">编辑</a-button>
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
  </a-space>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { deleteUsingPost, getMyDay } from '@/api/scheduleEventController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { canEditSchedule, formatDateTime } from '@/utils/app'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const selectedDate = ref<string>((route.query.date as string) || dayjs().format('YYYY-MM-DD'))
const scheduleList = ref<API.ScheduleEventVO[]>([])

const fetchData = async () => {
  const res = await getMyDay({ date: selectedDate.value })
  if (res.data.code === 0) {
    scheduleList.value = res.data.data?.scheduleList ?? []
  } else {
    message.error(res.data.message || '获取当日日程失败')
  }
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

const canEdit = (record: API.ScheduleEventVO) => canEditSchedule(loginUserStore.loginUser, record)

const openEdit = (record: API.ScheduleEventVO) => {
  if (!record.id) return
  router.push(`/schedule/manage?id=${record.id}`)
}

const doDelete = async (id?: number) => {
  if (!id) return
  const res = await deleteUsingPost({ id })
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
  router.push('/schedule/my')
}

onMounted(fetchData)
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
