<template>
    <a-card title="提醒策略管理" :bordered="false">
        <a-space direction="vertical" style="width: 100%" size="large">
            <a-alert
                    type="info"
                    show-icon
                    message="说明"
                    description="员工可按日程设置提前提醒时间、重复次数和重复间隔。保存后需要后端定时扫描或管理员手动扫描生成提醒任务。"
            />

            <a-form layout="inline" :model="searchParams" @finish="fetchData">
                <a-form-item label="日程">
                    <a-select
                            v-model:value="searchParams.scheduleId"
                            style="width: 240px"
                            allow-clear
                            show-search
                            placeholder="请选择日程"
                            :options="scheduleOptions"
                            :filter-option="filterScheduleOption"
                    />
                </a-form-item>
                <a-form-item label="状态">
                    <a-select v-model:value="searchParams.status" allow-clear style="width: 160px">
                        <a-select-option value="enabled">enabled</a-select-option>
                        <a-select-option value="disabled">disabled</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item>
                    <a-space>
                        <a-button type="primary" html-type="submit">查询</a-button>
                        <a-button @click="openAddModal">新增策略</a-button>
                        <a-button v-if="isAdmin" @click="doScanNow">立即扫描</a-button>
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
                    <template v-if="column.dataIndex === 'popupEnabled'">
                        <a-tag :color="record.popupEnabled ? 'green' : 'default'">
                            {{ record.popupEnabled ? '开启' : '关闭' }}
                        </a-tag>
                    </template>

                    <template v-else-if="column.dataIndex === 'status'">
                        <a-badge
                                :status="record.status === 'enabled' ? 'success' : 'default'"
                                :text="record.status"
                        />
                    </template>

                    <template v-else-if="column.key === 'action'">
                        <a-space>
                            <a-button type="link" @click="openEditModal(record)">编辑</a-button>
                            <a-popconfirm title="确认删除该策略吗？" @confirm="doDelete(record.id)">
                                <a-button type="link" danger>删除</a-button>
                            </a-popconfirm>
                        </a-space>
                    </template>
                </template>
            </a-table>
        </a-space>

        <a-modal
                :open="modalVisible"
                :title="isEdit ? '编辑提醒策略' : '新增提醒策略'"
                @ok="submitForm"
                @cancel="closeModal"
                width="720px"
                destroy-on-close
        >
            <a-form layout="vertical" :model="formState">
                <a-form-item label="选择日程" required>
                    <a-select
                            v-model:value="formState.scheduleId"
                            show-search
                            placeholder="请选择日程"
                            :options="scheduleOptions"
                            :filter-option="filterScheduleOption"
                    />
                </a-form-item>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="提前提醒时间（分钟）" required>
                            <a-input-number
                                    v-model:value="formState.remindOffsetMinutes"
                                    :min="1"
                                    style="width: 100%"
                            />
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="重复次数">
                            <a-input-number
                                    v-model:value="formState.repeatCount"
                                    :min="0"
                                    style="width: 100%"
                            />
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="重复时间间隔（分钟）">
                            <a-input-number
                                    v-model:value="formState.repeatIntervalMinutes"
                                    :min="1"
                                    style="width: 100%"
                            />
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="弹窗提醒">
                            <a-switch v-model:checked="popupEnabledBool" />
                        </a-form-item>
                    </a-col>

                    <a-col :span="24">
                        <a-form-item label="状态">
                            <a-radio-group v-model:value="formState.status">
                                <a-radio value="enabled">enabled</a-radio>
                                <a-radio value="disabled">disabled</a-radio>
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>
            </a-form>
        </a-modal>
    </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteRule, listRuleByPage, saveRule, scanNow } from '@/api/scheduleReminderController'
import { listPage as listSchedulePage } from '@/api/scheduleEventController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const columns = [
    { title: 'ID', dataIndex: 'id' },
    { title: '日程 ID', dataIndex: 'scheduleId' },
    { title: '日程标题', dataIndex: 'scheduleTitle' },
    { title: '提前提醒(分)', dataIndex: 'remindOffsetMinutes' },
    { title: '重复次数', dataIndex: 'repeatCount' },
    { title: '重复间隔(分)', dataIndex: 'repeatIntervalMinutes' },
    { title: '弹窗提醒', dataIndex: 'popupEnabled' },
    { title: '状态', dataIndex: 'status' },
    { title: '操作', key: 'action' },
]

const data = ref<API.ScheduleReminderRuleVO[]>([])
const total = ref(0)
const modalVisible = ref(false)
const isEdit = ref(false)
const popupEnabledBool = ref(true)

const scheduleOptions = ref<{ label: string; value: number }[]>([])

const searchParams = reactive<API.ScheduleReminderRuleQueryRequest>({
    pageNum: 1,
    pageSize: 10,
    sortField: 'createTime',
    sortOrder: 'desc',
})

const formState = reactive<API.ScheduleReminderRuleSaveRequest>({
    id: undefined,
    scheduleId: undefined,
    remindOffsetMinutes: 5,
    repeatCount: 0,
    repeatIntervalMinutes: 5,
    popupEnabled: 1,
    status: 'enabled',
})

const syncPopupEnabled = () => {
    formState.popupEnabled = popupEnabledBool.value ? 1 : 0
}

const formatDisplayTime = (time?: string) => {
    if (!time) return ''
    return time.replace('T', ' ')
}

const loadScheduleOptions = async () => {
    const res = await listSchedulePage({
        pageNum: 1,
        pageSize: 100,
        sortField: 'startTime',
        sortOrder: 'asc',
    } as API.ScheduleEventQueryRequest)

    if (res.data.code === 0 && res.data.data) {
        const records = res.data.data.records ?? []
        scheduleOptions.value = records.map((item: API.ScheduleEventVO) => ({
            value: item.id as number,
            label: `${item.title || '未命名日程'}${item.startTime ? `（${formatDisplayTime(item.startTime)}）` : ''}`,
        }))
    } else {
        message.error(res.data.message || '获取日程列表失败')
    }
}

const filterScheduleOption = (input: string, option: any) => {
    return (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

const fetchData = async () => {
    const res = await listRuleByPage({ ...searchParams })
    if (res.data.code === 0 && res.data.data) {
        data.value = res.data.data.records ?? []
        total.value = res.data.data.totalRow ?? 0
    } else {
        message.error(res.data.message || '获取提醒策略失败')
    }
}

const resetForm = () => {
    Object.assign(formState, {
        id: undefined,
        scheduleId: undefined,
        remindOffsetMinutes: 5,
        repeatCount: 0,
        repeatIntervalMinutes: 5,
        popupEnabled: 1,
        status: 'enabled',
    })
    popupEnabledBool.value = true
}

const openAddModal = () => {
    resetForm()
    isEdit.value = false
    modalVisible.value = true
}

const openEditModal = (record: API.ScheduleReminderRuleVO) => {
    Object.assign(formState, record)
    popupEnabledBool.value = record.popupEnabled === 1
    isEdit.value = true
    modalVisible.value = true
}

const closeModal = () => {
    modalVisible.value = false
    resetForm()
}

const submitForm = async () => {
    syncPopupEnabled()

    if (!formState.scheduleId) {
        message.warning('请选择日程')
        return
    }

    const res = await saveRule({ ...formState })
    if (res.data.code === 0) {
        message.success(isEdit.value ? '更新策略成功' : '新增策略成功')
        closeModal()
        fetchData()
    } else {
        message.error(res.data.message || '保存失败')
    }
}

const doDelete = async (id?: number) => {
    if (!id) return
    const res = await deleteRule({ id })
    if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
    } else {
        message.error(res.data.message || '删除失败')
    }
}

const doScanNow = async () => {
    const res = await scanNow()
    if (res.data.code === 0) {
        message.success('已触发提醒扫描')
    } else {
        message.error(res.data.message || '扫描触发失败')
    }
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

onMounted(async () => {
    await loadScheduleOptions()
    await fetchData()
})
</script>
