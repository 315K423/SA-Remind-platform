<template>
  <div class="global-header">
    <div class="left">
      <a-space :size="8">
        <img class="logo" src="@/assets/logo.png" alt="logo" />
        <div class="title-wrapper">
          <div class="title">企业日程与考勤智能提醒平台</div>
          <div class="sub-title">Schedule & Attendance Reminder Admin</div>
        </div>
      </a-space>
    </div>
    <div class="right">
      <a-space size="middle">
        <a-tag color="blue">{{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '员工' }}</a-tag>
        <a-dropdown>
          <a-space class="user-trigger">
            <a-avatar :src="loginUserStore.loginUser.userAvatar">
              {{ (loginUserStore.loginUser.userName || 'U').slice(0, 1) }}
            </a-avatar>
            <span>{{ loginUserStore.loginUser.userName || '未登录' }}</span>
          </a-space>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="goHome">返回工作台</a-menu-item>
              <a-menu-item danger @click="doLogout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const goHome = () => {
  router.push('/')
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error(`退出登录失败，${res.data.message}`)
  }
}
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 8px 24px;
  background: #fff;
  box-sizing: border-box;
}

.left,
.right {
  display: flex;
  align-items: center;
}

.logo {
  width: 36px;
  height: 36px;
  object-fit: cover;
  flex-shrink: 0;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.2;
  margin: 0;
}

.sub-title {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.1;
  margin-top: 2px;
}

.user-trigger {
  cursor: pointer;
}
</style>
