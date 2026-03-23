<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="header">
        <img class="logo" src="@/assets/logo.png" alt="logo" />
        <div>
          <h2>用户登录</h2>
          <p>欢迎进入企业日程与考勤智能提醒平台</p>
        </div>
      </div>
      <a-form :model="formState" layout="vertical" autocomplete="off" @finish="handleSubmit">
        <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" />
        </a-form-item>
        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large" />
        </a-form-item>
        <div class="tips">没有账号？<RouterLink to="/user/register">去注册</RouterLink></div>
        <a-button type="primary" html-type="submit" block size="large">登录</a-button>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogin } from '@/api/userController'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } else {
    message.error(`登录失败，${res.data.message}`)
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef5ff 0%, #f7f9fc 100%);
}
.auth-card {
  width: 420px;
  padding: 32px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(31, 35, 41, 0.08);
}
.header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 24px;
}
.logo {
  width: 48px;
  height: 48px;
}
.header h2 {
  margin: 0 0 4px;
}
.header p {
  margin: 0;
  color: #8c8c8c;
}
.tips {
  text-align: right;
  margin-bottom: 16px;
  color: #8c8c8c;
}
</style>
