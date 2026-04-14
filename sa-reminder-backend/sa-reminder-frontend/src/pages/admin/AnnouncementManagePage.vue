<template>
  <a-card title="公告管理" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-form layout="inline" :model="searchParams" @finish="fetchData">
        <a-form-item label="标题">
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
            <a-button @click="openModal()">新建公告</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        row-key="id"
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

          <template v-else-if="column.dataIndex === 'status'">
            <a-badge
              :status="record.status === 'published' ? 'success' : 'default'"
              :text="record.status || '-'"
            />
          </template>

          <template v-else-if="column.dataIndex === 'departmentIdList'">
            {{ getDepartmentNames(record.departmentIdList) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除该公告吗？" @confirm="doDelete(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-space>

    <a-modal
      :open="modalVisible"
      :title="formState.id ? '编辑公告' : '新建公告'"
      width="760px"
      destroy-on-close
      @ok="submitForm"
      @cancel="closeModal"
    >
      <a-form layout="vertical" :model="formState">
        <a-form-item label="公告标题" required>
          <a-input v-model:value="formState.title" placeholder="请输入公告标题" />
        </a-form-item>

        <a-form-item label="公告内容" required>
          <a-textarea v-model:value="formState.content" :rows="6" placeholder="请输入公告内容" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="通知范围" required>
              <a-radio-group v-model:value="formState.scopeType" @change="handleScopeTypeChange">
                <a-radio value="all">全公司</a-radio>
                <a-radio value="department">指定部门</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option value="published">已发布</a-select-option>
                <a-select-option value="draft">草稿</a-select-option>
                <a-select-option value="disabled">已停用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item v-if="formState.scopeType === 'department'" label="目标部门" required>
          <a-select
            v-model:value="formState.departmentIdList"
            mode="multiple"
            :options="departmentOptions"
            show-search
            option-filter-prop="label"
            placeholder="请选择接收公告的部门"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { addAnnouncement, deleteAnnouncement, listPage2, updateAnnouncement } from '@/api/announcementController'
import { listPage1 as listDepartmentPage } from '@/api/departmentController'
import { message } from 'ant-design-vue'
import { announcementScopeLabelMap } from '@/utils/app'

type DepartmentId = string

interface AnnouncementSearchForm {
  pageNum: number
  pageSize: number
  sortField?: string
  sortOrder?: string
  title?: string
  scopeType?: string
  status?: string
}

interface AnnouncementFormState {
  id?: number
  title: string
  content: string
  scopeType: string
  status: string
  departmentIdList: DepartmentId[]
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '通知范围', dataIndex: 'scopeType', width: 120 },
  { title: '目标部门', dataIndex: 'departmentIdList', width: 220 },
  { title: '状态', dataIndex: 'status', width: 120 },
  { title: '发布时间', dataIndex: 'publishTime', width: 180 },
  { title: '操作', key: 'action', width: 140 },
]

const data = ref<API.AnnouncementVO[]>([])
const total = ref(0)
const modalVisible = ref(false)

/**
 * 关键修复点：
 * 部门 ID 统一按 string 保存，避免 Long / 雪花 ID 在前端 number 中丢精度
 */
const departmentMap = ref<Record<string, string>>({})

const searchParams = reactive<AnnouncementSearchForm>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'publishTime',
  sortOrder: 'desc',
  title: '',
  scopeType: undefined,
  status: undefined,
})

const formState = reactive<AnnouncementFormState>({
  id: undefined,
  title: '',
  content: '',
  scopeType: 'all',
  status: 'published',
  departmentIdList: [],
})

const departmentOptions = computed(() =>
  Object.entries(departmentMap.value).map(([value, label]) => ({
    label,
    value,
  })),
)

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const fetchDepartments = async () => {
  const res = await listDepartmentPage({
    pageNum: 1,
    pageSize: 1000,
    sortField: 'id',
    sortOrder: 'asc',
  } as any)

  if (res.data.code === 0 && res.data.data) {
    const map: Record<string, string> = {}
    ;(res.data.data.records ?? []).forEach((item: any) => {
      if (item.id !== undefined && item.id !== null) {
        const id = String(item.id)
        map[id] = item.name || `部门${id}`
      }
    })
    departmentMap.value = map
  } else {
    message.error(res.data.message || '获取部门列表失败')
  }
}

const getDepartmentNames = (departmentIdList?: Array<string | number>) => {
  if (!departmentIdList || departmentIdList.length === 0) return '-'
  return departmentIdList
    .map((id) => departmentMap.value[String(id)] || `部门${id}`)
    .join('、')
}

const fetchData = async () => {
  const res = await listPage2({ ...searchParams } as any)
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取公告列表失败')
  }
}

const resetSearch = async () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    title: '',
    scopeType: undefined,
    status: undefined,
  })
  await fetchData()
}

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    title: '',
    content: '',
    scopeType: 'all',
    status: 'published',
    departmentIdList: [],
  })
}

const openModal = (record?: API.AnnouncementVO) => {
  resetForm()
  if (record) {
    Object.assign(formState, {
      id: record.id,
      title: record.title || '',
      content: record.content || '',
      scopeType: record.scopeType || 'all',
      status: record.status || 'published',
      departmentIdList: (record.departmentIdList || []).map((id) => String(id)),
    })
  }
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

const handleScopeTypeChange = () => {
  if (formState.scopeType !== 'department') {
    formState.departmentIdList = []
  }
}

const submitForm = async () => {
  if (!formState.title || !formState.content) {
    message.warning('请填写完整的标题和内容')
    return
  }

  if (
    formState.scopeType === 'department' &&
    (!formState.departmentIdList || formState.departmentIdList.length === 0)
  ) {
    message.warning('请选择目标部门')
    return
  }

  const payload = {
    ...formState,
    departmentIdList: formState.scopeType === 'department' ? formState.departmentIdList : [],
  }

  const res = formState.id
    ? await updateAnnouncement(payload as any)
    : await addAnnouncement(payload as any)

  if (res.data.code === 0) {
    message.success(formState.id ? '公告更新成功' : '公告创建成功')
    closeModal()
    await fetchData()
  } else {
    message.error(res.data.message || '提交失败')
  }
}

const doDelete = async (id?: number) => {
  if (!id) return
  const res = await deleteAnnouncement({ id } as any)
  if (res.data.code === 0) {
    message.success('删除成功')
    await fetchData()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const onTableChange = async (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  await fetchData()
}

onMounted(async () => {
  await Promise.all([fetchDepartments(), fetchData()])
})
</script>
