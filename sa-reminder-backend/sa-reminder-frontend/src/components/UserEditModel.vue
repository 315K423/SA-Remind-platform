<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    width="640px"
    destroy-on-close
    :confirm-loading="submitLoading"
    @ok="submitForm"
    @cancel="handleCancel"
  >
    <a-form layout="vertical" :model="formState">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="用户名" required>
            <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="账号" :required="isAddMode">
            <a-input
              v-model:value="formState.userAccount"
              :disabled="!isAddMode"
              placeholder="请输入账号"
            />
          </a-form-item>
        </a-col>

        <template v-if="!isProfileMode">
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
        </template>

        <a-col :span="24">
          <a-form-item label="头像">
            <a-space align="center">
              <a-avatar :size="64" :src="formState.userAvatar">
                {{ (formState.userName || formState.userAccount || 'U').slice(0, 1) }}
              </a-avatar>
              <a-upload
                accept="image/*"
                :show-upload-list="false"
                :before-upload="beforeAvatarUpload"
                :custom-request="handleAvatarUpload"
              >
                <a-button :loading="avatarUploading">上传头像</a-button>
              </a-upload>
            </a-space>
            <div class="avatar-tip">
              支持 JPG、PNG、GIF、WEBP 格式。上传成功后会自动回填头像地址。
            </div>
          </a-form-item>
        </a-col>

        <a-col :span="24">
          <a-form-item label="头像地址">
            <a-input v-model:value="formState.userAvatar" disabled placeholder="上传后自动生成头像访问地址" />
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
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import { addUser, updateMyUser, updateUser, uploadAvatar } from '@/api/userController'
import { message } from 'ant-design-vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    mode: 'add' | 'edit' | 'profile'
    user?: API.UserVO | API.LoginUserVO | null
    departmentOptions?: { label: string; value: number | string }[]
  }>(),
  {
    user: null,
    departmentOptions: () => [],
  },
)

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success', value?: API.LoginUserVO | boolean): void
}>()

const submitLoading = ref(false)
const avatarUploading = ref(false)

const formState = reactive<API.UserAddRequest & API.UserUpdateRequest & { userAccount?: string }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
  departmentId: undefined,
})

const isAddMode = computed(() => props.mode === 'add')
const isEditMode = computed(() => props.mode === 'edit')
const isProfileMode = computed(() => props.mode === 'profile')

const modalTitle = computed(() => {
  if (isProfileMode.value) return '编辑个人信息'
  return isEditMode.value ? '编辑用户' : '新增用户'
})

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

const fillForm = () => {
  resetForm()
  if (!props.user) return
  Object.assign(formState, {
    id: props.user.id as any,
    userAccount: props.user.userAccount || '',
    userName: props.user.userName || '',
    userAvatar: props.user.userAvatar || '',
    userProfile: props.user.userProfile || '',
    userRole: props.user.userRole || 'user',
    departmentId: props.user.departmentId as any,
  })
}

watch(
  () => [props.open, props.user, props.mode],
  () => {
    if (props.open) {
      fillForm()
    }
  },
  { immediate: true },
)

const closeModal = () => {
  emit('update:open', false)
  resetForm()
}

const handleCancel = () => {
  closeModal()
}

const beforeAvatarUpload = (file: File) => {
  const validType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!validType) {
    message.error('仅支持 JPG、PNG、GIF、WEBP 格式图片')
    return false
  }

  const validSize = file.size / 1024 / 1024 < 5
  if (!validSize) {
    message.error('头像大小不能超过 5MB')
    return false
  }

  return true
}

const handleAvatarUpload = async (options: any) => {
  if (!formState.userAccount) {
    message.warning('请先填写账号，再上传头像')
    options?.onError?.(new Error('请先填写账号'))
    return
  }

  const file = options.file as File
  const formData = new FormData()
  formData.append('file', file)

  avatarUploading.value = true
  try {
    const res = await uploadAvatar(
      { userAccount: formState.userAccount },
      formData as any,
      {
        // 这里必须覆盖掉生成接口里的 application/json，让浏览器按 FormData 自动生成 multipart 边界。
        headers: {},
      },
    )

    if (res.data.code === 0 && res.data.data) {
      formState.userAvatar = res.data.data
      message.success('头像上传成功')
      options?.onSuccess?.(res.data, file)
    } else {
      const error = new Error(res.data.message || '头像上传失败')
      message.error(error.message)
      options?.onError?.(error)
    }
  } catch (error) {
    message.error('头像上传失败')
    options?.onError?.(error)
  } finally {
    avatarUploading.value = false
  }
}

const submitForm = async () => {
  if (!formState.userName) {
    message.warning('请输入用户名')
    return
  }

  if (isAddMode.value && !formState.userAccount) {
    message.warning('请输入账号')
    return
  }

  submitLoading.value = true
  try {
    if (isAddMode.value) {
      const res = await addUser({
        userAccount: formState.userAccount,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
        departmentId: formState.departmentId || undefined,
      } as API.UserAddRequest)

      if (res.data.code !== 0) {
        message.error(res.data.message || '新增失败')
        return
      }

      message.success('新增成功')
      emit('success', true)
      closeModal()
      return
    }

    if (isProfileMode.value) {
      const res = await updateMyUser({
        id: formState.id as any,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
      } as API.UserUpdateRequest)

      if (res.data.code !== 0) {
        message.error(res.data.message || '更新失败')
        return
      }

      message.success('更新成功')
      emit('success', res.data.data)
      closeModal()
      return
    }

    const res = await updateUser({
      id: formState.id as any,
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
      userRole: formState.userRole,
      departmentId: formState.departmentId || undefined,
    } as API.UserUpdateRequest)

    if (res.data.code !== 0) {
      message.error(res.data.message || '更新失败')
      return
    }

    message.success('更新成功')
    emit('success', true)
    closeModal()
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.avatar-tip {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
