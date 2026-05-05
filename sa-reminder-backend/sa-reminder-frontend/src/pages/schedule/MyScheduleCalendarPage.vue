<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card :bordered="false" title="我的日程">
      <a-row justify="space-between" align="middle" :gutter="16">
        <a-col>
          <a-space>
            <a-button @click="goToday">回到今天</a-button>
            <a-button type="primary" @click="goManage">进入日程管理</a-button>
          </a-space>
        </a-col>
        <a-col>
          <div style="color: #8c8c8c">
            展示自己创建的日程以及作为参与人的日程，点击某一天可进入 24 小时视图。
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false">
      <a-calendar v-model:value="calendarValue" @panelChange="handlePanelChange" @select="handleSelect">
        <template #dateCellRender="{ current }">
          <ul class="events">
            <li v-for="item in getListData(current)" :key="`${item.id}-${item.startTime}`" @click.stop="openDay(current)">
              <a-badge :status="getScheduleBadgeStatus(item)" :text="item.title" />
            </li>
          </ul>
        </template>
      </a-calendar>
    </a-card>
  </a-space>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { Dayjs } from 'dayjs'
import { listMyMonth } from '@/api/scheduleEventController'
import { message } from 'ant-design-vue'

const router = useRouter()
const calendarValue = ref(dayjs())
const scheduleMap = ref<Record<string, API.ScheduleEventVO[]>>({})

const fetchMonthData = async (value: Dayjs) => {
  const res = await listMyMonth({
    year: value.year(),
    month: value.month() + 1,
  })
  if (res.data.code === 0) {
    const map: Record<string, API.ScheduleEventVO[]> = {}
    ;(res.data.data ?? []).forEach((item) => {
      const date = (item.startTime || '').slice(0, 10)
      if (!date) return
      if (!map[date]) map[date] = []
      map[date].push(item)
    })
    scheduleMap.value = map
  } else {
    message.error(res.data.message || '获取月历日程失败')
  }
}

const getListData = (current: Dayjs) => {
  return scheduleMap.value[current.format('YYYY-MM-DD')] || []
}

const getScheduleBadgeStatus = (record: API.ScheduleEventVO) => {
  if (record.status === 'cancelled') {
    return 'error'
  }

  const now = dayjs()
  const startTime = record.startTime ? dayjs(record.startTime) : null
  const endTime = record.endTime ? dayjs(record.endTime) : null

  // 结束时间早于当前时间，说明日程已经过期，显示灰色圆点。
  if (endTime && endTime.isBefore(now)) {
    return 'default'
  }

  // 开始时间已到，但结束时间未到，说明日程正在进行，显示蓝色圆点。
  if (startTime && !startTime.isAfter(now) && (!endTime || endTime.isAfter(now))) {
    return 'processing'
  }

  // 未开始的正常日程仍然显示绿色圆点。
  return 'success'
}

const openDay = (value: Dayjs) => {
  router.push(`/schedule/my/day?date=${value.format('YYYY-MM-DD')}`)
}

const handlePanelChange = async (value: Dayjs) => {
  calendarValue.value = value
  await fetchMonthData(value)
}

type CalendarSelectInfo = {
  source?: 'year' | 'month' | 'date' | 'customize'
}

const handleSelect = async (value: Dayjs, info?: CalendarSelectInfo) => {
  // 只有点击日期单元格时才进入日日程页；切换年份/月只刷新月历数据。
  if (info?.source && info.source !== 'date') {
    calendarValue.value = value
    await fetchMonthData(value)
    return
  }
  openDay(value)
}

const goToday = async () => {
  calendarValue.value = dayjs()
  await fetchMonthData(calendarValue.value)
}

const goManage = () => {
  router.push('/schedule/manage')
}

onMounted(async () => {
  await fetchMonthData(calendarValue.value)
})
</script>

<style scoped>
.events {
  list-style: none;
  margin: 0;
  padding: 0;
}
.events li {
  cursor: pointer;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
