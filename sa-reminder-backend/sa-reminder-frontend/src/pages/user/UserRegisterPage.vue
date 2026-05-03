<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="header">
        <img class="logo" src="@/assets/logo.png" alt="logo" />
        <div>
          <h2>用户注册</h2>
          <p>创建账号后即可进入平台配置个人日程与提醒策略</p>
        </div>
      </div>
      <a-form :model="formState" layout="vertical" autocomplete="off" @finish="handleSubmit">
        <a-form-item label="账号" name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" />
        </a-form-item>
        <a-form-item label="用户名" name="userName" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="formState.userName" placeholder="请输入用户名" size="large" />
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
        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" size="large" />
        </a-form-item>
        <div class="tips">已有账号？<RouterLink to="/user/login">去登录</RouterLink></div>
        <a-button type="primary" html-type="submit" block size="large">注册</a-button>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController'
import { message } from 'ant-design-vue'

const router = useRouter()
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userName: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (_rule: unknown, value: string) => {
  if (value && value !== formState.userPassword) {
    return Promise.reject('两次输入密码不一致')
  }
  return Promise.resolve()
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push('/user/login')
  } else {
    message.error(`注册失败，${res.data.message}`)
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
  width: 440px;
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
