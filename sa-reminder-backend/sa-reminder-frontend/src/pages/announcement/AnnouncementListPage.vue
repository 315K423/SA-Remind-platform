<template>
  <a-card title="公告通知" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-alert
        type="info"
        show-icon
        message="公告接收说明"
        description="这里会展示你收到的全公司公告和部门公告。未读公告可点击“标记已读”，列表会同步显示接收状态。"
      />

      <a-list :data-source="data" item-layout="vertical" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item>
            <template #actions>
              <a-tag :color="item.scopeType === 'department' ? 'purple' : 'blue'">
                {{ announcementScopeLabelMap[item.scopeType || 'all'] || item.scopeType || '-' }}
              </a-tag>
              <a-tag :color="item.receiveStatus === 'READ' || item.receiveStatus === 'read' ? 'green' : 'orange'">
                {{ item.receiveStatus === 'READ' || item.receiveStatus === 'read' ? '已读' : '未读' }}
              </a-tag>
              <span>发布时间：{{ formatDateTime(item.publishTime) }}</span>
            </template>
            <a-list-item-meta :description="item.content">
              <template #title>
                <a-space>
                  <span>{{ item.title }}</span>
                  <a-button
                    v-if="item.receiveStatus !== 'READ' && item.receiveStatus !== 'read'"
                    type="link"
                    @click="markRead(item.id)"
                  >
                    标记已读
                  </a-button>
                </a-space>
              </template>
            </a-list-item-meta>
            <div v-if="item.departmentIdList?.length">
              接收部门：{{ item.departmentIdList.join('、') }}
            </div>
            <div v-if="item.readTime" style="color: #8c8c8c">已读时间：{{ formatDateTime(item.readTime) }}</div>
          </a-list-item>
        </template>
      </a-list>
    </a-space>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { myList, read } from '@/api/announcementController'
import { message } from 'ant-design-vue'
import { announcementScopeLabelMap, formatDateTime } from '@/utils/app'

const data = ref<API.AnnouncementVO[]>([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await myList()
    if (res.data.code === 0) {
      data.value = res.data.data ?? []
    } else {
      message.error(res.data.message || '获取公告失败')
    }
  } finally {
    loading.value = false
  }
}

const markRead = async (id?: number) => {
  if (!id) return
  const res = await read({ id })
  if (res.data.code === 0) {
    message.success('已标记为已读')
    await fetchData()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

onMounted(fetchData)
</script>
