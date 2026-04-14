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

    <a-modal
      :open="modalVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="640px"
      destroy-on-close
      @ok="submitForm"
      @cancel="closeModal"
    >
      <a-form layout="vertical" :model="formState">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" required>
              <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账号" :required="!isEdit">
              <a-input v-model:value="formState.userAccount" :disabled="isEdit" placeholder="请输入账号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色" required>
              <a-select v-model:value="formState.userRole">
                <a-select-option value="admin">管理员</a-select-option>
                <a-select-option value="manager">部门经理</a-select-option>
                <a-select-option value="user">普通员工</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属部门">
              <a-select
                v-model:value="formState.departmentId"
                :options="departmentOptions"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="请选择部门"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="头像地址">
              <a-input v-model:value="formState.userAvatar" placeholder="请输入头像 URL" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="简介">
              <a-textarea v-model:value="formState.userProfile" :rows="3" placeholder="请输入简介" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { addUser, deleteUser, listUserVoByPage, updateUser } from '@/api/userController'
import { listPage1 as listDepartmentPage } from '@/api/departmentController'
import { message } from 'ant-design-vue'
import { getRoleLabel } from '@/utils/app'

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
const modalVisible = ref(false)
const isEdit = ref(false)
const departmentOptions = ref<{ label: string; value: number }[]>([])

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const formState = reactive<API.UserAddRequest & API.UserUpdateRequest>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
  departmentId: undefined,
})

const loadDepartments = async () => {
  const res = await listDepartmentPage({ pageNum: 1, pageSize: 1000, sortField: 'id', sortOrder: 'asc' })
  if (res.data.code === 0 && res.data.data) {
    departmentOptions.value = (res.data.data.records ?? []).map((item) => ({
      label: item.name || `部门${item.id}`,
      value: item.id as number,
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
  fetchData()
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
  fetchData()
}

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
    departmentId: undefined,
  })
}

const openAddModal = () => {
  resetForm()
  isEdit.value = false
  modalVisible.value = true
}

const openEditModal = (record: API.UserVO) => {
  Object.assign(formState, record)
  isEdit.value = true
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

const submitForm = async () => {
  if (!formState.userName) {
    message.warning('请输入用户名')
    return
  }
  if (!isEdit.value && !formState.userAccount) {
    message.warning('请输入账号')
    return
  }

  const payload = {
    ...formState,
    departmentId: formState.departmentId || undefined,
  }
  const res = isEdit.value ? await updateUser(payload) : await addUser(payload)
  if (res.data.code === 0) {
    message.success(isEdit.value ? '更新成功' : '新增成功')
    closeModal()
    fetchData()
  } else {
    message.error(res.data.message || '提交失败')
  }
}

const doDelete = async (id?: number) => {
  if (!id) return
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败')
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
  fetchData()
}

onMounted(async () => {
  await Promise.all([loadDepartments(), fetchData()])
})
</script>
