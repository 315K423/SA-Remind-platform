<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card title="部门管理" :bordered="false">
      <a-space direction="vertical" style="width: 100%" size="large">
        <a-form layout="inline" :model="searchParams" @finish="fetchDepartments">
          <a-form-item label="部门名称">
            <a-input v-model:value="searchParams.name" allow-clear placeholder="请输入部门名称" />
          </a-form-item>
          <a-form-item label="部门编码">
            <a-input v-model:value="searchParams.code" allow-clear placeholder="请输入部门编码" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit">查询</a-button>
              <a-button @click="resetSearch">重置</a-button>
              <a-button @click="openDepartmentModal()">新建部门</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-table row-key="id" :columns="departmentColumns" :data-source="departmentList" :pagination="pagination" @change="onDepartmentTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="openDepartmentModal(record)">编辑</a-button>
                <a-button type="link" @click="openAssignDrawer(record)">分配员工</a-button>
                <a-button type="link" @click="openTransferDrawer(record)">调出员工</a-button>
                <a-popconfirm title="确认删除该部门吗？" @confirm="doDeleteDepartment(record.id)">
                  <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-space>
    </a-card>

    <a-card title="Excel 批量导入员工" :bordered="false">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="10">
          <a-form layout="vertical">
            <a-form-item label="导入到部门" required>
              <a-select v-model:value="importState.departmentId" :options="departmentOptions" show-search option-filter-prop="label" />
            </a-form-item>
            <a-form-item label="导入默认角色">
              <a-select v-model:value="importState.defaultRole">
                <a-select-option value="user">普通员工</a-select-option>
                <a-select-option value="manager">部门经理</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="Excel 文件" required>
              <a-upload :before-upload="handleBeforeUpload" :max-count="1" accept=".xls,.xlsx">
                <a-button>选择文件</a-button>
              </a-upload>
            </a-form-item>
            <a-space>
              <a-button type="primary" @click="doImportUsers">开始导入</a-button>
              <span style="color: #8c8c8c">建议列头包含：账号、用户名、头像、简介</span>
            </a-space>
          </a-form>
        </a-col>
        <a-col :xs="24" :lg="14">
          <a-alert
            type="info"
            show-icon
            message="导入说明"
            description="导入后的员工只会属于当前选择的部门。若员工已归属其他部门，后台会按你的规则进行更新或拒绝，请以返回结果为准。"
          />
          <a-divider />
          <a-list bordered :data-source="importPreview" size="small">
            <template #renderItem="{ item }">
              <a-list-item>{{ item }}</a-list-item>
            </template>
          </a-list>
        </a-col>
      </a-row>
    </a-card>

    <a-modal :open="departmentModalVisible" :title="departmentForm.id ? '编辑部门' : '新建部门'" @ok="submitDepartment" @cancel="closeDepartmentModal">
      <a-form layout="vertical" :model="departmentForm">
        <a-form-item label="部门名称" required>
          <a-input v-model:value="departmentForm.name" placeholder="请输入部门名称" />
        </a-form-item>
        <a-form-item label="部门编码" required>
          <a-input v-model:value="departmentForm.code" placeholder="请输入部门编码" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="departmentForm.description" :rows="3" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer :open="assignDrawerVisible" width="720" title="分配部门员工" @close="closeAssignDrawer">
      <a-space direction="vertical" style="width: 100%" size="large">
        <a-alert type="info" show-icon :message="`当前部门：${currentDepartment?.name || '-'}`" />
        <a-select
          v-model:value="assignSelectedUserIds"
          mode="multiple"
          style="width: 100%"
          :options="allUserOptions"
          placeholder="请选择要加入该部门的员工"
          show-search
          option-filter-prop="label"
        />
        <a-button type="primary" @click="submitAssignUsers">确认分配</a-button>
      </a-space>
    </a-drawer>

    <a-drawer :open="transferDrawerVisible" width="900" title="调整员工部门归属" @close="closeTransferDrawer">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-alert type="info" show-icon :message="`来源部门：${currentDepartment?.name || '-'}`" />
          <a-divider />
          <a-select
            v-model:value="transferSelectedUserIds"
            mode="multiple"
            style="width: 100%"
            :options="currentDepartmentUserOptions"
            placeholder="请选择要调出的员工"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form layout="vertical">
            <a-form-item label="目标部门" required>
              <a-select v-model:value="transferTargetDepartmentId" :options="targetDepartmentOptions" show-search option-filter-prop="label" />
            </a-form-item>
          </a-form>
          <a-button type="primary" @click="submitTransferUsers">确认调配</a-button>
        </a-col>
      </a-row>
    </a-drawer>
  </a-space>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addDepart,
  assignUsers,
  deleteDepart,
  importUsers,
  listPage1,
  transferUsers,
  updateDepart,
} from '@/api/departmentController'
import { listUserVoByPage } from '@/api/userController'
import { message } from 'ant-design-vue'

const departmentColumns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '部门名称', dataIndex: 'name' },
  { title: '部门编码', dataIndex: 'code' },
  { title: '描述', dataIndex: 'description' },
  { title: '人数', dataIndex: 'userCount', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 260 },
]

const departmentList = ref<API.DepartmentVO[]>([])
const total = ref(0)
const currentDepartment = ref<API.DepartmentVO>()
const departmentModalVisible = ref(false)
const assignDrawerVisible = ref(false)
const transferDrawerVisible = ref(false)
const allUsers = ref<API.UserVO[]>([])
const selectedImportFile = ref<File>()
const assignSelectedUserIds = ref<number[]>([])
const transferSelectedUserIds = ref<number[]>([])
const transferTargetDepartmentId = ref<number>()

const searchParams = reactive<API.DepartmentQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const departmentForm = reactive<API.DepartmentUpdateRequest>({
  id: undefined,
  name: '',
  code: '',
  description: '',
})

const importState = reactive({
  departmentId: undefined as number | undefined,
  defaultRole: 'user',
})

const importPreview = computed(() => {
  const preview = [`默认角色：${importState.defaultRole === 'manager' ? '部门经理' : '普通员工'}`]
  preview.push(`目标部门：${departmentOptions.value.find((item) => item.value === importState.departmentId)?.label || '-'}`)
  preview.push(`当前文件：${selectedImportFile.value?.name || '未选择文件'}`)
  return preview
})

const departmentOptions = computed(() =>
  departmentList.value.map((item) => ({ label: item.name || `部门${item.id}`, value: item.id as number })),
)

const targetDepartmentOptions = computed(() =>
  departmentOptions.value.filter((item) => item.value !== currentDepartment.value?.id),
)

const allUserOptions = computed(() =>
  allUsers.value.map((item) => ({
    label: `${item.userName || '未命名'}${item.departmentName ? `（${item.departmentName}）` : ''}`,
    value: item.id as number,
  })),
)

const currentDepartmentUserOptions = computed(() =>
  allUsers.value
    .filter((item) => Number(item.departmentId) === Number(currentDepartment.value?.id))
    .map((item) => ({
      label: `${item.userName || '未命名'} - ${item.userAccount || ''}`,
      value: item.id as number,
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
  const res = await listPage1({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    departmentList.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(res.data.message || '获取部门列表失败')
  }
}

const fetchAllUsers = async () => {
  const res = await listUserVoByPage({ pageNum: 1, pageSize: 1000, sortField: 'id', sortOrder: 'asc' })
  if (res.data.code === 0 && res.data.data) {
    allUsers.value = res.data.data.records ?? []
  }
}

const resetSearch = async () => {
  Object.assign(searchParams, { pageNum: 1, pageSize: 10, name: '', code: '' })
  await fetchDepartments()
}

const onDepartmentTableChange = async (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  await fetchDepartments()
}

const resetDepartmentForm = () => {
  Object.assign(departmentForm, { id: undefined, name: '', code: '', description: '' })
}

const openDepartmentModal = (record?: API.DepartmentVO) => {
  if (record) {
    Object.assign(departmentForm, record)
  } else {
    resetDepartmentForm()
  }
  departmentModalVisible.value = true
}

const closeDepartmentModal = () => {
  departmentModalVisible.value = false
  resetDepartmentForm()
}

const submitDepartment = async () => {
  if (!departmentForm.name || !departmentForm.code) {
    message.warning('请填写完整的部门名称和编码')
    return
  }
  const res = departmentForm.id ? await updateDepart(departmentForm) : await addDepart(departmentForm as API.DepartmentAddRequest)
  if (res.data.code === 0) {
    message.success(departmentForm.id ? '部门更新成功' : '部门创建成功')
    closeDepartmentModal()
    await fetchDepartments()
  } else {
    message.error(res.data.message || '提交失败')
  }
}

const doDeleteDepartment = async (id?: number) => {
  if (!id) return
  const res = await deleteDepart({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    await fetchDepartments()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

const openAssignDrawer = async (record: API.DepartmentVO) => {
  currentDepartment.value = record
  assignSelectedUserIds.value = allUsers.value
    .filter((item) => Number(item.departmentId) === Number(record.id))
    .map((item) => item.id as number)
  assignDrawerVisible.value = true
}

const closeAssignDrawer = () => {
  assignDrawerVisible.value = false
  currentDepartment.value = undefined
  assignSelectedUserIds.value = []
}

const submitAssignUsers = async () => {
  if (!currentDepartment.value?.id) return
  const res = await assignUsers({
    departmentId: currentDepartment.value.id,
    userIdList: assignSelectedUserIds.value,
  })
  if (res.data.code === 0) {
    message.success('员工分配成功')
    closeAssignDrawer()
    await Promise.all([fetchDepartments(), fetchAllUsers()])
  } else {
    message.error(res.data.message || '员工分配失败')
  }
}

const openTransferDrawer = async (record: API.DepartmentVO) => {
  currentDepartment.value = record
  transferSelectedUserIds.value = []
  transferTargetDepartmentId.value = undefined
  transferDrawerVisible.value = true
}

const closeTransferDrawer = () => {
  transferDrawerVisible.value = false
  currentDepartment.value = undefined
  transferSelectedUserIds.value = []
  transferTargetDepartmentId.value = undefined
}

const submitTransferUsers = async () => {
  if (!currentDepartment.value?.id || !transferTargetDepartmentId.value || transferSelectedUserIds.value.length === 0) {
    message.warning('请选择需要调配的员工和目标部门')
    return
  }
  const res = await transferUsers({
    fromDepartmentId: currentDepartment.value.id,
    toDepartmentId: transferTargetDepartmentId.value,
    userIdList: transferSelectedUserIds.value,
  })
  if (res.data.code === 0) {
    message.success('员工调配成功')
    closeTransferDrawer()
    await Promise.all([fetchDepartments(), fetchAllUsers()])
  } else {
    message.error(res.data.message || '员工调配失败')
  }
}

const handleBeforeUpload = (file: File) => {
  selectedImportFile.value = file
  return false
}

const doImportUsers = async () => {
  if (!importState.departmentId || !selectedImportFile.value) {
    message.warning('请选择目标部门和 Excel 文件')
    return
  }
  const res = await importUsers(
    { departmentId: importState.departmentId, defaultRole: importState.defaultRole },
    {},
    selectedImportFile.value,
  )
  if (res.data.code === 0) {
    message.success(`导入成功，本次返回 ${res.data.data?.length || 0} 条员工记录`)
    selectedImportFile.value = undefined
    await Promise.all([fetchDepartments(), fetchAllUsers()])
  } else {
    message.error(res.data.message || '导入失败')
  }
}

onMounted(async () => {
  await Promise.all([fetchDepartments(), fetchAllUsers()])
})
</script>
