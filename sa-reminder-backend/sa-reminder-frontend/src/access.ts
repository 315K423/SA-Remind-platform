import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'

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
  const requiredRole = to.meta?.role as string | undefined

  if (requiresAuth && !loginUser?.id) {
    next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  if (requiredRole && loginUser?.userRole !== requiredRole) {
    message.error('没有权限访问该页面')
    next('/')
    return
  }

  next()
})
