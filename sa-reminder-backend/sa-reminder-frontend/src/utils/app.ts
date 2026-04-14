export const ROLE_ADMIN = 'admin'
export const ROLE_MANAGER = 'manager'
export const ROLE_USER = 'user'

export const roleLabelMap: Record<string, string> = {
  [ROLE_ADMIN]: '系统管理员',
  [ROLE_MANAGER]: '部门经理',
  [ROLE_USER]: '普通员工',
}

export const announcementScopeLabelMap: Record<string, string> = {
  all: '全公司',
  company: '全公司',
  department: '指定部门',
}

export const scheduleTypeTextMap: Record<string, string> = {
  personal: '个人日程',
  meeting: '会议安排',
  attendance: '考勤事项',
}

export const scheduleTypeColorMap: Record<string, string> = {
  personal: 'blue',
  meeting: 'purple',
  attendance: 'orange',
}

export const scheduleStatusTextMap: Record<string, string> = {
  normal: '正常',
  cancelled: '已取消',
}

export const scheduleStatusColorMap: Record<string, string> = {
  normal: 'success',
  cancelled: 'error',
}

export const attendanceStatusTextMap: Record<string, string> = {
  checked_in: '考勤成功',
  not_checked: '未考勤',
}

export const attendanceStatusColorMap: Record<string, string> = {
  checked_in: 'success',
  not_checked: 'warning',
}

export const normalizeRole = (role?: string) => (role || '').toLowerCase()

export const getRoleLabel = (role?: string) => roleLabelMap[normalizeRole(role)] || role || '未知角色'

export const isAdmin = (role?: string) => normalizeRole(role) === ROLE_ADMIN

export const isManager = (role?: string) => normalizeRole(role) === ROLE_MANAGER

export const isUser = (role?: string) => normalizeRole(role) === ROLE_USER

export const hasAnyRole = (role: string | undefined, roles: string[]) => roles.includes(normalizeRole(role))

export const canEditSchedule = (loginUser: API.LoginUserVO, record: API.ScheduleEventVO) => {
  if (!loginUser?.id || !record?.id) {
    return false
  }
  if (isAdmin(loginUser.userRole) || isManager(loginUser.userRole)) {
    return true
  }
  return Number(loginUser.id) === Number(record.creatorId)
}

export const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

export const formatDate = (value?: string) => {
  if (!value) return '-'
  return value.slice(0, 10)
}

export const toMonthNumber = (value: string | number) => Number(value)
