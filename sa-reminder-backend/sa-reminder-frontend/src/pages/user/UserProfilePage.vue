<template>
  <a-card title="个人中心" :bordered="false">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-card>
        <a-space align="center" size="large">
          <a-avatar :size="88" :src="loginUser.userAvatar">
            {{ (loginUser.userName || loginUser.userAccount || 'U').slice(0, 1) }}
          </a-avatar>
          <div>
            <div class="profile-name">{{ loginUser.userName || '-' }}</div>
            <div class="profile-account">账号：{{ loginUser.userAccount || '-' }}</div>
            <a-space style="margin-top: 8px">
              <a-tag color="blue">{{ getRoleLabel(loginUser.userRole) }}</a-tag>
              <a-tag v-if="loginUser.departmentName" color="geekblue">{{ loginUser.departmentName }}</a-tag>
            </a-space>
          </div>
        </a-space>
      </a-card>

      <a-descriptions bordered :column="1">
        <a-descriptions-item label="账号">{{ loginUser.userAccount || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户名">{{ loginUser.userName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="角色">{{ getRoleLabel(loginUser.userRole) }}</a-descriptions-item>
        <a-descriptions-item label="所属部门">{{ loginUser.departmentName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="个人简介">{{ loginUser.userProfile || '暂无简介' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ loginUser.createTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-space>
        <a-button type="primary" @click="openEditModal">编辑个人信息</a-button>
        <a-button @click="refreshLoginUser">刷新信息</a-button>
      </a-space>
    </a-space>

    <UserEditModal
      v-model:open="profileModalVisible"
      mode="profile"
      :user="loginUser"
      @success="handleProfileSaved"
    />
  </a-card>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs} from "pinia"
import { message } from 'ant-design-vue'
import UserEditModal from '@/components/UserEditModel.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getRoleLabel } from '@/utils/app'

const loginUserStore = useLoginUserStore()
const profileModalVisible = ref(false)

const loginUser = computed(() => loginUserStore.loginUser)

const openEditModal = () => {
  profileModalVisible.value = true
}

const refreshLoginUser = async () => {
  await loginUserStore.fetchLoginUser()
  message.success('个人信息已刷新')
}

const handleProfileSaved = async (newLoginUser?: API.LoginUserVO | boolean) => {
  if (newLoginUser && typeof newLoginUser === 'object') {
    loginUserStore.setLoginUser(newLoginUser)
  } else {
    await loginUserStore.fetchLoginUser()
  }
}

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<style scoped>
.profile-name {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
}

.profile-account {
  margin-top: 6px;
  color: #8c8c8c;
}
</style>
