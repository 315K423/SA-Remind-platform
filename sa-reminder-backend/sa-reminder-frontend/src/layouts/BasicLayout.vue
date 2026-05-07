<template>
  <a-layout class="basic-layout">
    <a-layout-sider :collapsed="collapsed" collapsible width="248" class="sider">
      <div class="brand">SA Reminder</div>
      <a-menu
        theme="dark"
        mode="inline"
        v-model:openKeys="openKeys"
        :selectedKeys="selectedKeys"
        :items="menuItems"
        @click="onMenuClick"
      />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header-wrapper">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content-wrapper">
        <GlobalReminderPopup />
        <router-view />
      </a-layout-content>
      <a-layout-footer>
        <GlobalFooter />
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import {
  HomeOutlined,
  CalendarOutlined,
  BellOutlined,
  TeamOutlined,
  NotificationOutlined,
  ApartmentOutlined,
  AuditOutlined,
  PieChartOutlined,
} from '@ant-design/icons-vue'
import { Modal, type MenuProps } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalReminderPopup from '@/components/GlobalReminderPopup.vue'
import { isAdmin } from '@/utils/app'
import { myList } from '@/api/announcementController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const openKeys = ref<string[]>([])

const getDefaultOpenKeys = (path: string) => {
  if (path.startsWith('/admin/announcement')) return ['admin-announcement']
  if (path.startsWith('/admin/attendance')) return ['admin-attendance']
  return []
}

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
    openKeys.value = getDefaultOpenKeys(newPath)
  },
  { immediate: true },
)

const commonItems: MenuProps['items'] = [
  { key: '/', icon: () => h(HomeOutlined), label: '工作台', title: '工作台' },
  // { key: '/schedule/my', icon: () => h(CalendarOutlined), label: '我的日程', title: '我的日程' },
  { key: '/schedule/manage', icon: () => h(CalendarOutlined), label: '日程管理', title: '日程管理' },
  { key: '/reminder/rule', icon: () => h(BellOutlined), label: '提醒策略', title: '提醒策略' },
  { key: '/reminder/popup', icon: () => h(BellOutlined), label: '弹窗提醒', title: '弹窗提醒' },
  {
    key: '/announcement/list',
    icon: () => h(NotificationOutlined),
    label: '公告通知',
    title: '公告通知',
  },
]

const adminItems: MenuProps['items'] = [
  { key: '/admin/userManage', icon: () => h(TeamOutlined), label: '用户管理', title: '用户管理' },
  {
    key: '/admin/departmentManage',
    icon: () => h(ApartmentOutlined),
    label: '部门管理',
    title: '部门管理',
  },
  {
    key: 'admin-announcement',
    icon: () => h(NotificationOutlined),
    label: '公告管理',
    title: '公告管理',
    children: [
      { key: '/admin/announcementManage', label: '公告列表', title: '公告列表' },
      { key: '/admin/announcementReadRate', label: '读取率统计', title: '读取率统计' },
    ],
  },
  {
    key: 'admin-attendance',
    icon: () => h(AuditOutlined),
    label: '考勤管理',
    title: '考勤管理',
    children: [
      { key: '/admin/attendanceManage', label: '考勤记录', title: '考勤记录' },
      { key: '/admin/attendanceRate', icon: () => h(PieChartOutlined), label: '考勤率统计', title: '考勤率统计' },
    ],
  },
]

const menuItems = computed(() => {
  if (isAdmin(loginUserStore.loginUser.userRole)) {
    return [...commonItems, ...adminItems]
  }
  return commonItems
})

const onMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}

const isUnreadAnnouncement = (item: API.AnnouncementVO) => {
  const status = (item.receiveStatus || '').toLowerCase()
  return status !== 'read'
}

const checkUnreadAnnouncements = async () => {
  if (!loginUserStore.loginUser.id) return

  const res = await myList()
  if (res.data.code !== 0) return

  const unreadCount = (res.data.data || []).filter(isUnreadAnnouncement).length
  if (unreadCount <= 0) return

  Modal.confirm({
    title: '公告未读提醒',
    content: `你有 ${unreadCount} 条未读公告，请及时查看。`,
    okText: '去查看',
    cancelText: '稍后',
    onOk: () => router.push('/announcement/list'),
  })
}

onMounted(checkUnreadAnnouncements)
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}
.sider {
  min-height: 100vh;
}
.brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.header-wrapper {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.content-wrapper {
  padding: 20px;
  background: #f5f7fa;
}
</style>
