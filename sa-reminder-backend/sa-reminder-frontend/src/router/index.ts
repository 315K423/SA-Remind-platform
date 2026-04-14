import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import HomeView from '@/pages/HomeView.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import DepartmentManagePage from '@/pages/admin/DepartmentManagePage.vue'
import AnnouncementManagePage from '@/pages/admin/AnnouncementManagePage.vue'
import AttendanceManagePage from '@/pages/admin/AttendanceManagePage.vue'
import ScheduleManagePage from '@/pages/schedule/ScheduleManagePage.vue'
import MyScheduleCalendarPage from '@/pages/schedule/MyScheduleCalendarPage.vue'
import MyScheduleDayPage from '@/pages/schedule/MyScheduleDayPage.vue'
import ReminderRulePage from '@/pages/reminder/ReminderRulePage.vue'
import ReminderPopupPage from '@/pages/reminder/ReminderPopupPage.vue'
import AnnouncementListPage from '@/pages/announcement/AnnouncementListPage.vue'

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
          path: 'schedule/my',
          name: '我的日程',
          component: MyScheduleCalendarPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'schedule/my/day',
          name: '当日日程',
          component: MyScheduleDayPage,
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
          path: 'announcement/list',
          name: '公告通知',
          component: AnnouncementListPage,
          meta: { requiresAuth: true },
        },
        {
          path: 'admin/userManage',
          name: '用户管理',
          component: UserManagePage,
          meta: { requiresAuth: true, roles: ['admin'] },
        },
        {
          path: 'admin/departmentManage',
          name: '部门管理',
          component: DepartmentManagePage,
          meta: { requiresAuth: true, roles: ['admin'] },
        },
        {
          path: 'admin/announcementManage',
          name: '公告管理',
          component: AnnouncementManagePage,
          meta: { requiresAuth: true, roles: ['admin'] },
        },
        {
          path: 'admin/attendanceManage',
          name: '考勤管理',
          component: AttendanceManagePage,
          meta: { requiresAuth: true, roles: ['admin'] },
        },
      ],
    },
  ],
})

export default router
