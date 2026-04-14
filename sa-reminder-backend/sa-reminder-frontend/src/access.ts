import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import { hasAnyRole } from '@/utils/app'

let firstFetchLoginUser = true

router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }

  const requiresAuth = to.meta?.requiresAuth !== false
  const requiredRoles = (to.meta?.roles as string[] | undefined) || []
  const legacyRole = to.meta?.role as string | undefined
  const roleList = requiredRoles.length ? requiredRoles : legacyRole ? [legacyRole] : []

  if (requiresAuth && !loginUser?.id) {
    next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  if (roleList.length > 0 && !hasAnyRole(loginUser?.userRole, roleList)) {
    message.error('没有权限访问该页面')
    next('/')
    return
  }

  next()
})
