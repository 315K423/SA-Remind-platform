<template>
  <a-card title="公告读取率统计" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-alert
        type="info"
        show-icon
        message="读取率统计说明"
        description="系统根据公告通知范围动态计算应接收人数，并统计已读人数、未读人数和已读率。"
      />

      <a-form layout="inline" :model="searchParams" @finish="fetchData">
        <a-form-item label="公告标题">
          <a-input v-model:value="searchParams.title" allow-clear placeholder="请输入公告标题" />
        </a-form-item>
        <a-form-item label="通知范围">
          <a-select v-model:value="searchParams.scopeType" allow-clear style="width: 160px">
            <a-select-option value="all">全公司</a-select-option>
            <a-select-option value="department">指定部门</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchParams.status" allow-clear style="width: 160px">
            <a-select-option value="published">已发布</a-select-option>
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="disabled">已停用</a-select-option>
          </a-select>
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
            <a-statistic title="当前页公告数" :value="data.length" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic title="当前页平均读取率" :value="averageReadRate" suffix="%" :precision="2" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic title="低读取率公告数" :value="lowReadRateCount" suffix="条" />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        row-key="announcementId"
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'scopeType'">
            <a-tag :color="record.scopeType === 'department' ? 'purple' : 'blue'">
              {{ announcementScopeLabelMap[record.scopeType || 'all'] || record.scopeType || '-' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'readRate'">
            <RateDonutChart :rate="record.readRate || 0" title="已读率" :size="86" />
          </template>

          <template v-else-if="column.dataIndex === 'publishTime'">
            {{ formatDateTime(record.publishTime) }}
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
import { listReadRatePage } from '@/api/announcementController'
import { announcementScopeLabelMap, formatDateTime } from '@/utils/app'

const columns = [
  { title: '公告ID', dataIndex: 'announcementId', width: 110 },
  { title: '公告标题', dataIndex: 'title' },
  { title: '通知范围', dataIndex: 'scopeType', width: 120 },
  { title: '发布人', dataIndex: 'publisherName', width: 120 },
  { title: '发布时间', dataIndex: 'publishTime', width: 180 },
  { title: '应读人数', dataIndex: 'receiverCount', width: 100 },
  { title: '已读人数', dataIndex: 'readCount', width: 100 },
  { title: '未读人数', dataIndex: 'unreadCount', width: 100 },
  { title: '已读率', dataIndex: 'readRate', width: 120 },
]

const data = ref<API.AnnouncementReadRateVO[]>([])
const total = ref(0)

const searchParams = reactive<API.AnnouncementQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'publishTime',
  sortOrder: 'descend',
  title: '',
  scopeType: undefined,
  status: 'published',
})

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const averageReadRate = computed(() => {
  if (data.value.length === 0) return 0
  const sum = data.value.reduce((totalValue, item) => totalValue + Number(item.readRate || 0), 0)
  return Number((sum / data.value.length).toFixed(2))
})

const lowReadRateCount = computed(() => data.value.filter((item) => Number(item.readRate || 0) < 60).length)

const fetchData = async () => {
  const res = await listReadRatePage({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取公告读取率失败')
  }
}

const resetSearch = async () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    sortField: 'publishTime',
    sortOrder: 'descend',
    title: '',
    scopeType: undefined,
    status: 'published',
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
