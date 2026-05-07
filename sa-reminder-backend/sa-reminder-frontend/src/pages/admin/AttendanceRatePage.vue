<template>
  <a-card title="考勤率统计" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-alert
        type="info"
        show-icon
        message="考勤率统计说明"
        description="系统按考勤事项进行汇总，统计参与人数、已考勤人数、未考勤人数和考勤率。"
      />

      <a-form layout="inline" :model="searchParams" @finish="fetchData">
        <a-form-item label="日程标题">
          <a-input v-model:value="searchParams.scheduleTitle" allow-clear placeholder="请输入考勤事项标题" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="resetSearch">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-card size="small">
            <a-statistic title="当前页考勤事项数" :value="data.length" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic title="当前页平均考勤率" :value="averageAttendanceRate" suffix="%" :precision="2" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic title="低考勤率事项数" :value="lowAttendanceRateCount" suffix="项" />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        row-key="scheduleId"
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'scheduleStartTime'">
            {{ formatDateTime(record.scheduleStartTime) }}
          </template>

          <template v-else-if="column.dataIndex === 'scheduleEndTime'">
            {{ formatDateTime(record.scheduleEndTime) }}
          </template>

          <template v-else-if="column.dataIndex === 'attendanceRate'">
            <RateDonutChart :rate="record.attendanceRate || 0" title="考勤率" :size="86" />
          </template>
        </template>
      </a-table>
    </a-space>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import RateDonutChart from '@/components/RateDonutChart.vue'
import { listAttendanceRatePage } from '@/api/scheduleAttendanceController'
import { formatDateTime } from '@/utils/app'

const columns = [
  { title: '日程ID', dataIndex: 'scheduleId', width: 110 },
  { title: '日程标题', dataIndex: 'scheduleTitle' },
  { title: '开始时间', dataIndex: 'scheduleStartTime', width: 180 },
  { title: '结束时间', dataIndex: 'scheduleEndTime', width: 180 },
  { title: '参与人数', dataIndex: 'participantCount', width: 100 },
  { title: '已考勤人数', dataIndex: 'checkedCount', width: 110 },
  { title: '未考勤人数', dataIndex: 'uncheckedCount', width: 110 },
  { title: '考勤率', dataIndex: 'attendanceRate', width: 120 },
  { title: '签到地点', dataIndex: 'checkInAddress', width: 180 },
]

const data = ref<API.ScheduleAttendanceRateVO[]>([])
const total = ref(0)

const searchParams = reactive<API.ScheduleAttendanceQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  scheduleTitle: '',
})

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const averageAttendanceRate = computed(() => {
  if (data.value.length === 0) return 0
  const sum = data.value.reduce((totalValue, item) => totalValue + Number(item.attendanceRate || 0), 0)
  return Number((sum / data.value.length).toFixed(2))
})

const lowAttendanceRateCount = computed(() =>
  data.value.filter((item) => Number(item.attendanceRate || 0) < 60).length,
)

const fetchData = async () => {
  const res = await listAttendanceRatePage({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取考勤率失败')
  }
}

const resetSearch = async () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    scheduleTitle: '',
  })
  await fetchData()
}

const onTableChange = async (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  await fetchData()
}

onMounted(fetchData)
</script>
