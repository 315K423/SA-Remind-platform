<template>
  <a-layout class="basic-layout">
    <a-layout-sider :collapsed="collapsed" collapsible width="232" class="sider">
      <div class="brand">SA Reminder</div>
      <a-menu
        theme="dark"
        mode="inline"
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
import { computed, h, ref, watch } from 'vue'
import { HomeOutlined, CalendarOutlined, BellOutlined, TeamOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalReminderPopup from '@/components/GlobalReminderPopup.vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
  },
  { immediate: true },
)

const originItems: MenuProps['items'] = [
  { key: '/', icon: () => h(HomeOutlined), label: '工作台', title: '工作台' },
  { key: '/schedule/manage', icon: () => h(CalendarOutlined), label: '日程管理', title: '日程管理' },
  { key: '/reminder/rule', icon: () => h(BellOutlined), label: '提醒策略', title: '提醒策略' },
  { key: '/reminder/popup', icon: () => h(BellOutlined), label: '弹窗提醒', title: '弹窗提醒' },
  { key: '/admin/userManage', icon: () => h(TeamOutlined), label: '用户管理', title: '用户管理' },
]

const menuItems = computed(() => {
  if (loginUserStore.loginUser.userRole === 'admin') {
    return originItems
  }
  return originItems.filter((item) => item?.key !== '/admin/userManage')
})

const onMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}
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
