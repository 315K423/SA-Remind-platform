<template>
  <a-card title="用户管理" :bordered="false">
    <a-space direction="vertical" style="width: 100%" size="large">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="searchParams.userRole" style="width: 160px" allow-clear>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="manager">部门经理</a-select-option>
            <a-select-option value="user">普通员工</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="部门">
          <a-select
            v-model:value="searchParams.departmentId"
            style="width: 180px"
            :options="departmentOptions"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择部门"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="resetSearch">重置</a-button>
            <a-button @click="openAddModal">新增用户</a-button>
            <a-button @click="handleExportExcel">导出 Excel</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table :columns="columns" :data-source="data" :pagination="pagination" row-key="id" @change="doTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar :src="record.userAvatar">{{ (record.userName || 'U').slice(0, 1) }}</a-avatar>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag :color="getRoleColor(record.userRole)">
              {{ getRoleLabel(record.userRole) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'departmentName'">
            {{ record.departmentName || '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除该用户吗？" @confirm="doDelete(record.id)">
                <a-button danger type="link">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-space>

    <UserEditModal
      v-model:open="userModalVisible"
      :mode="userModalMode"
      :user="currentUser"
      :department-options="departmentOptions"
      @success="handleUserSaved"
    />
  </a-card>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, reactive } from 'vue'
import { deleteUser, exportUserExcel, listUserVoByPage } from '@/api/userController'
import { listPage1 as listDepartmentPage } from '@/api/departmentController'
import { message } from 'ant-design-vue'
import { getRoleLabel } from '@/utils/app'
import UserEditModal from '@/components/UserEditModel.vue'

const columns = [
  { title: 'ID', dataIndex: 'id' },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '用户名', dataIndex: 'userName' },
  { title: '头像', dataIndex: 'userAvatar' },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '角色', dataIndex: 'userRole' },
  { title: '部门', dataIndex: 'departmentName' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
]

const data = ref<API.UserVO[]>([])
const total = ref(0)
const departmentOptions = ref<{ label: string; value: string | number }[]>([])
const userModalVisible = ref(false)
const userModalMode = ref<'add' | 'edit'>('add')
const currentUser = ref<API.UserVO | null>(null)

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const loadDepartments = async () => {
  const res = await listDepartmentPage({ pageNum: 1, pageSize: 1000, sortField: 'id', sortOrder: 'asc' })
  if (res.data.code === 0 && res.data.data) {
    departmentOptions.value = (res.data.data.records ?? []).map((item) => ({
      label: item.name || `部门${item.id}`,
      // 后端 Long 可能会以字符串形式返回，这里不强转 number，避免大整数精度丢失。
      value: item.id as any,
    }))
  }
}

const fetchData = async () => {
  const res = await listUserVoByPage({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(`获取数据失败，${res.data.message}`)
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  void fetchData()
}

const resetSearch = () => {
  Object.assign(searchParams, {
    pageNum: 1,
    pageSize: 10,
    userAccount: '',
    userName: '',
    userRole: undefined,
    departmentId: undefined,
  })
  void fetchData()
}

const openAddModal = () => {
  currentUser.value = null
  userModalMode.value = 'add'
  userModalVisible.value = true
}

const openEditModal = (record: API.UserVO) => {
  currentUser.value = { ...record }
  userModalMode.value = 'edit'
  userModalVisible.value = true
}

const handleUserSaved = () => {
  void fetchData()
}

const doDelete = async (id?: API.UserVO['id']) => {
  if (!id) return
  const res = await deleteUser({ id: id as any })
  if (res.data.code === 0) {
    message.success('删除成功')
    await fetchData()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const getRoleColor = (role?: string) => {
  if (role === 'admin') return 'green'
  if (role === 'manager') return 'purple'
  return 'blue'
}

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  void fetchData()
}

const handleExportExcel = async () => {
  const exportParams: API.UserQueryRequest = {
    id: searchParams.id,
    userAccount: searchParams.userAccount,
    userName: searchParams.userName,
    userProfile: searchParams.userProfile,
    userRole: searchParams.userRole,
    departmentId: searchParams.departmentId,
    sortField: searchParams.sortField,
    sortOrder: searchParams.sortOrder,
  }

  const res = await exportUserExcel(
    { userQueryRequest: exportParams },
    { responseType: 'blob' },
  )
  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `用户数据_${new Date().toISOString().slice(0, 10)}.xlsx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  message.success('用户数据导出成功')
}

onMounted(async () => {
  await Promise.all([loadDepartments(), fetchData()])
})
</script>
