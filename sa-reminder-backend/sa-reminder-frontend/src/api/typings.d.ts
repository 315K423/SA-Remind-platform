declare namespace API {
  type AnnouncementAddRequest = {
    title?: string
    content?: string
    scopeType?: string
    departmentIdList?: number[]
  }

  type AnnouncementQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    title?: string
    scopeType?: string
    status?: string
    departmentId?: number
  }

  type AnnouncementReadRateVO = {
    announcementId?: number
    title?: string
    scopeType?: string
    status?: string
    publisherId?: number
    publisherName?: string
    publishTime?: string
    receiverCount?: number
    readCount?: number
    unreadCount?: number
    readRate?: number
  }

  type AnnouncementUpdateRequest = {
    id?: number
    title?: string
    content?: string
    scopeType?: string
    status?: string
    departmentIdList?: number[]
  }

  type AnnouncementVO = {
    id?: number
    title?: string
    content?: string
    scopeType?: string
    status?: string
    publisherId?: number
    publishTime?: string
    departmentIdList?: number[]
    receiveStatus?: string
    readTime?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseDepartmentVO = {
    code?: number
    data?: DepartmentVO
    message?: string
  }

  type BaseResponseListAnnouncementVO = {
    code?: number
    data?: AnnouncementVO[]
    message?: string
  }

  type BaseResponseListScheduleEventVO = {
    code?: number
    data?: ScheduleEventVO[]
    message?: string
  }

  type BaseResponseListScheduleReminderPopupVO = {
    code?: number
    data?: ScheduleReminderPopupVO[]
    message?: string
  }

  type BaseResponseListUserVO = {
    code?: number
    data?: UserVO[]
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

  type BaseResponsePageAnnouncementReadRateVO = {
    code?: number
    data?: PageAnnouncementReadRateVO
    message?: string
  }

  type BaseResponsePageAnnouncementVO = {
    code?: number
    data?: PageAnnouncementVO
    message?: string
  }

  type BaseResponsePageDepartmentVO = {
    code?: number
    data?: PageDepartmentVO
    message?: string
  }

  type BaseResponsePageScheduleAttendanceRateVO = {
    code?: number
    data?: PageScheduleAttendanceRateVO
    message?: string
  }

  type BaseResponsePageScheduleAttendanceVO = {
    code?: number
    data?: PageScheduleAttendanceVO
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

  type BaseResponseScheduleAttendanceCheckInVO = {
    code?: number
    data?: ScheduleAttendanceCheckInVO
    message?: string
  }

  type BaseResponseScheduleCalendarDayVO = {
    code?: number
    data?: ScheduleCalendarDayVO
    message?: string
  }

  type BaseResponseScheduleEventSaveVO = {
    code?: number
    data?: ScheduleEventSaveVO
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

  type BaseResponseString = {
    code?: number
    data?: string
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

  type DepartmentAddRequest = {
    name?: string
    code?: string
    description?: string
  }

  type DepartmentAssignUsersRequest = {
    departmentId?: number
    userIdList?: number[]
  }

  type DepartmentQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    name?: string
    code?: string
  }

  type DepartmentTransferUsersRequest = {
    fromDepartmentId?: number
    toDepartmentId?: number
    userIdList?: number[]
  }

  type DepartmentUpdateRequest = {
    id?: number
    name?: string
    code?: string
    description?: string
  }

  type DepartmentVO = {
    id?: number
    name?: string
    code?: string
    description?: string
    userCount?: number
    createTime?: string
  }

  type exportUserExcelParams = {
    userQueryRequest: UserQueryRequest
  }

  type getById1Params = {
    id: number
  }

  type getByIdParams = {
    id: number
  }

  type getMyDayParams = {
    date: string
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

  type importUsersParams = {
    departmentId: number
    defaultRole?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    departmentId?: number
    departmentName?: string
    createTime?: string
    updateTime?: string
  }

  type PageAnnouncementReadRateVO = {
    records?: AnnouncementReadRateVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageAnnouncementVO = {
    records?: AnnouncementVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageDepartmentVO = {
    records?: DepartmentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageScheduleAttendanceRateVO = {
    records?: ScheduleAttendanceRateVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageScheduleAttendanceVO = {
    records?: ScheduleAttendanceVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
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

  type scanParams = {
    to: string
    subject: string
    content: string
  }

  type ScheduleAttendanceCheckInRequest = {
    taskId?: number
    latitude?: number
    longitude?: number
  }

  type ScheduleAttendanceCheckInVO = {
    success?: boolean
    withinRange?: boolean
    distanceMeters?: number
    attendanceStatus?: string
    message?: string
  }

  type ScheduleAttendanceQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    scheduleId?: number
    userId?: number
    scheduleTitle?: string
    userName?: string
    attendanceStatus?: string
  }

  type ScheduleAttendanceRateVO = {
    scheduleId?: number
    scheduleTitle?: string
    scheduleStartTime?: string
    scheduleEndTime?: string
    participantCount?: number
    checkedCount?: number
    uncheckedCount?: number
    attendanceRate?: number
    checkInAddress?: string
    checkInRadiusMeters?: number
  }

  type ScheduleAttendanceUpdateRequest = {
    participantId?: number
    attendanceStatus?: string
  }

  type ScheduleAttendanceVO = {
    participantId?: number
    scheduleId?: number
    scheduleTitle?: string
    scheduleStartTime?: string
    scheduleEndTime?: string
    userId?: number
    userName?: string
    participantRole?: string
    responseStatus?: string
    attendanceStatus?: string
    checkInTime?: string
    checkInAddress?: string
    checkInRadiusMeters?: number
    checkInDistanceMeters?: number
  }

  type ScheduleCalendarDayVO = {
    date?: string
    scheduleList?: ScheduleEventVO[]
  }

  type ScheduleCalendarQueryRequest = {
    year?: number
    month?: number
  }

  type ScheduleConflictVO = {
    scheduleId?: number
    title?: string
    startTime?: string
    endTime?: string
    conflictType?: string
    userId?: number
    userName?: string
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
    checkInEnabled?: number
    checkInAddress?: string
    checkInLatitude?: number
    checkInLongitude?: number
    checkInRadiusMeters?: number
    participantUserIdList?: number[]
    departmentIdList?: number[]
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
    departmentId?: number
    participantUserId?: number
  }

  type ScheduleEventSaveVO = {
    success?: boolean
    eventId?: number
    conflictDetected?: boolean
    conflictList?: ScheduleConflictVO[]
  }

  type ScheduleEventUpdateRequest = {
    id?: number
    title?: string
    content?: string
    location?: string
    startTime?: string
    endTime?: string
    allDay?: number
    scheduleType?: string
    status?: string
    visibility?: string
    checkInEnabled?: number
    checkInAddress?: string
    checkInLatitude?: number
    checkInLongitude?: number
    checkInRadiusMeters?: number
    participantUserIdList?: number[]
    departmentIdList?: number[]
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
    checkInEnabled?: number
    checkInAddress?: string
    checkInLatitude?: number
    checkInLongitude?: number
    checkInRadiusMeters?: number
    participantUserIdList?: number[]
    departmentIdList?: number[]
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
    attendanceCheckRequired?: boolean
    checkInAddress?: string
    checkInRadiusMeters?: number
    scheduleType?: string
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

  type uploadAvatarParams = {
    userAccount?: string
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    departmentId?: number
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
    departmentId?: number
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
    departmentId?: number
  }

  type UserRegisterRequest = {
    userAccount?: string
    userName?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    departmentId?: number
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    departmentId?: number
    departmentName?: string
    createTime?: string
  }
}
