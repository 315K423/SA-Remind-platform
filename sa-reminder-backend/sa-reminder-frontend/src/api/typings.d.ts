declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseListScheduleReminderPopupVO = {
    code?: number
    data?: ScheduleReminderPopupVO[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageScheduleEventVO = {
    code?: number
    data?: PageScheduleEventVO
    message?: string
  }

  type BaseResponsePageScheduleReminderRuleVO = {
    code?: number
    data?: PageScheduleReminderRuleVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseScheduleEventVO = {
    code?: number
    data?: ScheduleEventVO
    message?: string
  }

  type BaseResponseScheduleReminderRuleVO = {
    code?: number
    data?: ScheduleReminderRuleVO
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type DeleteRequest = {
    id?: number
  }

  type getByIdParams = {
    id: number
  }

  type getRuleByScheduleIdParams = {
    scheduleId: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageScheduleEventVO = {
    records?: ScheduleEventVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageScheduleReminderRuleVO = {
    records?: ScheduleReminderRuleVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ScheduleEventAddRequest = {
    title?: string
    content?: string
    location?: string
    startTime?: string
    endTime?: string
    allDay?: number
    scheduleType?: string
    visibility?: string
    participantUserIdList?: number[]
  }

  type ScheduleEventQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    title?: string
    scheduleType?: string
    startTimeFrom?: string
    startTimeTo?: string
    creatorId?: number
  }

  type ScheduleEventUpdateRequest = {
    id?: number
    title?: string
    content?: string
    location?: string
    startTime?: string
    endTime?: string
    allDay?: number
    status?: string
    visibility?: string
    participantUserIdList?: number[]
  }

  type ScheduleEventVO = {
    id?: number
    title?: string
    content?: string
    location?: string
    startTime?: string
    endTime?: string
    allDay?: number
    scheduleType?: string
    visibility?: string
    status?: string
    creatorId?: number
    participantUserIdList?: number[]
    createTime?: string
    updateTime?: string
  }

  type ScheduleReminderPopupReadAllRequest = {
    taskIdList?: number[]
  }

  type ScheduleReminderPopupVO = {
    id?: number
    scheduleId?: number
    ruleId?: number
    remindIndex?: number
    popupTitle?: string
    popupContent?: string
    plannedRemindTime?: string
    actualRemindTime?: string
    taskStatus?: string
  }

  type ScheduleReminderRuleQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    scheduleId?: number
    userId?: number
    status?: string
  }

  type ScheduleReminderRuleSaveRequest = {
    id?: number
    scheduleId?: number
    remindOffsetMinutes?: number
    repeatCount?: number
    repeatIntervalMinutes?: number
    popupEnabled?: number
    status?: string
  }

  type ScheduleReminderRuleVO = {
    id?: number
    scheduleId?: number
    userId?: number
    scheduleTitle?: string
    scheduleStartTime?: string
    remindOffsetMinutes?: number
    repeatCount?: number
    repeatIntervalMinutes?: number
    popupEnabled?: number
    status?: string
    createTime?: string
    updateTime?: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}
