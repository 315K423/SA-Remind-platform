<template>
  <a-card title="日程管理" :bordered="false">
    <a-space direction="vertical" style="width: 100%" size="large">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="标题">
          <a-input v-model:value="searchParams.title" placeholder="请输入日程标题" allow-clear />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="searchParams.scheduleType" style="width: 160px" allow-clear>
            <a-select-option value="personal">个人日程</a-select-option>
            <a-select-option value="meeting">会议安排</a-select-option>
            <a-select-option value="attendance">考勤事项</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="openAddModal">新建日程</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'scheduleType'">
            <a-tag :color="typeColorMap[record.scheduleType || 'personal']">
              {{ typeTextMap[record.scheduleType || 'personal'] }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-badge
              :status="record.status === 'cancelled' ? 'error' : 'success'"
              :text="statusTextMap[record.status || 'normal'] || record.status || '正常'"
            />
          </template>

          <template v-else-if="column.dataIndex === 'startTime'">
            {{ formatDisplayTime(record.startTime) }}
          </template>

          <template v-else-if="column.dataIndex === 'endTime'">
            {{ formatDisplayTime(record.endTime) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除该日程吗？" @confirm="doDelete(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-space>

    <a-modal
      :open="modalVisible"
      :title="isEdit ? '编辑日程' : '新建日程'"
      @ok="submitForm"
      @cancel="closeModal"
      width="720px"
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
              <a-select v-model:value="formState.scheduleType">
                <a-select-option value="personal">个人日程</a-select-option>
                <a-select-option value="company">公司会议安排</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="开始时间" required>
              <a-date-picker
                v-model:value="formState.startTime"
                show-time
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                placeholder="请选择开始时间"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="结束时间">
              <a-date-picker
                v-model:value="formState.endTime"
                show-time
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
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
            <a-form-item label="可见范围">
              <a-select v-model:value="formState.visibility">
                <a-select-option value="private">仅自己可见</a-select-option>
                <a-select-option value="public">公开</a-select-option>
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
                @change="(checked:boolean) => formState.allDay = checked ? 1 : 0"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item label="内容说明">
              <a-textarea v-model:value="formState.content" :rows="4" placeholder="请输入日程说明" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { add, deleteUsingPost, listPage, update } from '@/api/scheduleEventController'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 120 },
  { title: '标题', dataIndex: 'title' },
  { title: '类型', dataIndex: 'scheduleType', width: 120 },
  { title: '开始时间', dataIndex: 'startTime', width: 180 },
  { title: '结束时间', dataIndex: 'endTime', width: 180 },
  { title: '地点', dataIndex: 'location', width: 150 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 },
]

const typeTextMap: Record<string, string> = {
  personal: '个人日程',
  meeting: '会议安排',
  attendance: '考勤事项',
}

const typeColorMap: Record<string, string> = {
  personal: 'blue',
  meeting: 'purple',
  attendance: 'orange',
}

const statusTextMap: Record<string, string> = {
  normal: '正常',
  cancelled: '已取消',
}

const data = ref<API.ScheduleEventVO[]>([])
const total = ref(0)
const modalVisible = ref(false)
const isEdit = ref(false)

const formState = reactive<any>({
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
})

const searchParams = reactive<API.ScheduleEventQueryRequest>({
  title: undefined,
  scheduleType: undefined,
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'desc',
})

const formatDisplayTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

const fetchData = async () => {
  const res = await listPage({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(`获取日程数据失败，${res.data.message}`)
  }
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
  })
}

const openAddModal = () => {
  resetForm()
  isEdit.value = false
  modalVisible.value = true
}

const openEditModal = (record: API.ScheduleEventVO) => {
  Object.assign(formState, {
    ...record,
    startTime: record.startTime || undefined,
    endTime: record.endTime || undefined,
  })
  isEdit.value = true
  modalVisible.value = true
}

const buildSubmitParams = () => {
  return {
    ...formState,
    startTime: formState.startTime
      ? dayjs(formState.startTime).format('YYYY-MM-DDTHH:mm:ss')
      : undefined,
    endTime: formState.endTime
      ? dayjs(formState.endTime).format('YYYY-MM-DDTHH:mm:ss')
      : undefined,
  }
}

const submitForm = async () => {
  if (!formState.title || !formState.startTime) {
    message.warning('请填写完整的标题和开始时间')
    return
  }

  if (formState.endTime && dayjs(formState.endTime).isBefore(dayjs(formState.startTime))) {
    message.warning('结束时间不能早于开始时间')
    return
  }

  const params = buildSubmitParams()
  const res = isEdit.value ? await update(params) : await add(params)

  if (res.data.code === 0) {
    message.success(isEdit.value ? '更新成功' : '新增成功')
    closeModal()
    fetchData()
  } else {
    message.error(res.data.message || '提交失败')
  }
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

const doDelete = async (id?: number) => {
  if (!id) return
  const res = await deleteUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

onMounted(fetchData)
</script>
