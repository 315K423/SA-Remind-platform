import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import HomeView from '@/pages/HomeView.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import ScheduleManagePage from '@/pages/schedule/ScheduleManagePage.vue'
import ReminderRulePage from '@/pages/reminder/ReminderRulePage.vue'
import ReminderPopupPage from '@/pages/reminder/ReminderPopupPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
      meta: { requiresAuth: false },
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: '工作台',
          component: HomeView,
          meta: { requiresAuth: true },
        },
        {
          path: 'schedule/manage',
          name: '日程管理',
          component: ScheduleManagePage,
          meta: { requiresAuth: true },
        },
        {
          path: 'reminder/rule',
          name: '提醒策略',
          component: ReminderRulePage,
          meta: { requiresAuth: true },
        },
        {
          path: 'reminder/popup',
          name: '弹窗提醒',
          component: ReminderPopupPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'admin/userManage',
          name: '用户管理',
          component: UserManagePage,
          meta: { requiresAuth: true, role: 'admin' },
        },
      ],
    },
  ],
})

export default router
