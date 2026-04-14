# 数据库初始化

-- 创建库
create database if not exists sa_reminder;

-- 切换库
use sa_reminder;

-- 用户表
-- 以下是建表语句

    -- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;



-- 日程管理相关表（与 user 表同库 sa_reminder）
-- 注意：实体使用 snowflakeId 生成主键，因此这里不使用 auto_increment。

create table if not exists schedule_event
(
    id           bigint                                 not null comment 'id' primary key,
    title        varchar(256)                           not null comment '标题',
    content      varchar(2048)                          null comment '内容/备注',
    location     varchar(256)                           null comment '地点',
    startTime    datetime                               not null comment '开始时间',
    endTime      datetime                               not null comment '结束时间',
    allDay       tinyint      default 0                 not null comment '是否全天 0/1',
    scheduleType varchar(64)  default 'personal'        not null comment '日程类型：personal/company',
    visibility   varchar(64)  default 'private'         not null comment '可见性：private/public',
    status       varchar(64)  default 'normal'          not null comment '状态：normal/cancelled',
    creatorId    bigint                                 not null comment '创建人 userId',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    INDEX idx_creatorId (creatorId),
    INDEX idx_startTime (startTime),
    INDEX idx_endTime (endTime)
) comment '日程事件' collate = utf8mb4_unicode_ci;

-- 日程提醒策略与弹窗提醒相关表
create table if not exists schedule_participant
(
    id              bigint                                 not null comment 'id' primary key,
    scheduleId      bigint                                 not null comment '日程ID',
    userId          bigint                                 not null comment '参与人用户ID',
    participantRole varchar(64)  default 'participant'     not null comment '角色：owner/participant',
    responseStatus  varchar(64)  default 'pending'         not null comment '响应：pending/accepted/declined',
    joinTime        datetime     default CURRENT_TIMESTAMP not null comment '加入时间',
    createTime      datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_schedule_user (scheduleId, userId),
    INDEX idx_userId (userId)
) comment '日程参与人' collate = utf8mb4_unicode_ci;


create table if not exists schedule_reminder_rule
(
    id                    bigint                                 not null comment 'id' primary key,
    scheduleId            bigint                                 not null comment '日程ID',
    userId                bigint                                 not null comment '提醒所属用户ID',
    remindOffsetMinutes   int          default 30                not null comment '首次提醒提前分钟数，例如 30 表示开始前30分钟',
    repeatCount           int          default 0                 not null comment '重复次数（额外重复次数，0 表示仅提醒1次）',
    repeatIntervalMinutes int          default 5                 not null comment '重复提醒间隔分钟数',
    popupEnabled          tinyint      default 1                 not null comment '是否启用网页弹窗 0/1',
    status                varchar(64)  default 'enabled'         not null comment '状态：enabled/disabled',
    lastGenerateTime      datetime                               null comment '最近一次生成提醒任务时间',
    createTime            datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime            datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete              tinyint      default 0                 not null comment '是否删除',
    unique key uk_schedule_user (scheduleId, userId),
    index idx_userId_status (userId, status),
    index idx_scheduleId (scheduleId)
) comment '日程提醒策略' collate = utf8mb4_unicode_ci;

create table if not exists schedule_reminder_task
(
    id                 bigint                                 not null comment 'id' primary key,
    ruleId             bigint                                 not null comment '提醒策略ID',
    scheduleId         bigint                                 not null comment '日程ID',
    userId             bigint                                 not null comment '提醒用户ID',
    remindIndex        int                                    not null comment '第几次提醒，从0开始',
    plannedRemindTime  datetime                               not null comment '计划提醒时间',
    actualRemindTime   datetime                               null comment '实际触发时间',
    taskStatus         varchar(64)  default 'pending'         not null comment '任务状态：pending/sent/read/expired',
    popupTitle         varchar(256)                           not null comment '弹窗标题',
    popupContent       varchar(1024)                          not null comment '弹窗内容',
    readTime           datetime                               null comment '已读时间',
    createTime         datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime         datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete           tinyint      default 0                 not null comment '是否删除',
    unique key uk_rule_index (ruleId, remindIndex),
    index idx_user_status_time (userId, taskStatus, plannedRemindTime),
    index idx_scheduleId (scheduleId)
) comment '日程提醒任务' collate = utf8mb4_unicode_ci;

ALTER TABLE `user`
    ADD COLUMN departmentId BIGINT NULL COMMENT '所属部门ID' AFTER userRole,
    ADD INDEX idx_departmentId (departmentId);

create table if not exists department
(
    id          bigint                                 not null comment 'id' primary key,
    name        varchar(128)                           not null comment '部门名称',
    code        varchar(128)                           null comment '部门编码',
    description varchar(512)                           null comment '部门描述',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除',
    unique key uk_department_name (name)
) comment '部门表' collate = utf8mb4_unicode_ci;

create table if not exists schedule_department
(
    id           bigint                                 not null comment 'id' primary key,
    scheduleId   bigint                                 not null comment '日程ID',
    departmentId bigint                                 not null comment '部门ID',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    unique key uk_schedule_department (scheduleId, departmentId),
    index idx_departmentId (departmentId)
) comment '日程-部门关联表' collate = utf8mb4_unicode_ci;

create table if not exists announcement
(
    id          bigint                                 not null comment 'id' primary key,
    title       varchar(256)                           not null comment '公告标题',
    content     varchar(4096)                          not null comment '公告内容',
    scopeType   varchar(64)                            not null comment '通知范围 all/department',
    status      varchar(64)  default 'published'       not null comment '状态',
    publisherId bigint                                 not null comment '发布人',
    publishTime datetime     default CURRENT_TIMESTAMP not null comment '发布时间',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除'
) comment '公告表' collate = utf8mb4_unicode_ci;

create table if not exists announcement_department
(
    id             bigint                                 not null comment 'id' primary key,
    announcementId bigint                                 not null comment '公告ID',
    departmentId   bigint                                 not null comment '部门ID',
    createTime     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint      default 0                 not null comment '是否删除',
    unique key uk_announcement_department (announcementId, departmentId)
) comment '公告-部门关联表' collate = utf8mb4_unicode_ci;

create table if not exists announcement_receiver
(
    id             bigint                                 not null comment 'id' primary key,
    announcementId bigint                                 not null comment '公告ID',
    userId         bigint                                 not null comment '接收用户ID',
    receiveStatus  varchar(64)  default 'unread'         not null comment '接收状态 unread/read',
    readTime       datetime                               null comment '阅读时间',
    createTime     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint      default 0                 not null comment '是否删除',
    unique key uk_announcement_user (announcementId, userId),
    index idx_userId_receiveStatus (userId, receiveStatus)
) comment '公告接收表' collate = utf8mb4_unicode_ci;


ALTER TABLE schedule_event
    ADD COLUMN checkInEnabled tinyint DEFAULT 0 NOT NULL COMMENT '是否启用定位签到 0/1' AFTER creatorId,
    ADD COLUMN checkInAddress varchar(256) NULL COMMENT '签到地点描述' AFTER checkInEnabled,
    ADD COLUMN checkInLatitude decimal(10,7) NULL COMMENT '签到目标纬度' AFTER checkInAddress,
    ADD COLUMN checkInLongitude decimal(10,7) NULL COMMENT '签到目标经度' AFTER checkInLatitude,
    ADD COLUMN checkInRadiusMeters int DEFAULT 200 NOT NULL COMMENT '签到半径(米)' AFTER checkInLongitude;

ALTER TABLE schedule_participant
    ADD COLUMN attendanceStatus varchar(64) DEFAULT 'not_checked' NOT NULL COMMENT '考勤状态 not_checked/checked_in' AFTER responseStatus,
    ADD COLUMN checkInTime datetime NULL COMMENT '签到时间' AFTER attendanceStatus,
    ADD COLUMN checkInLatitude decimal(10,7) NULL COMMENT '签到时纬度' AFTER checkInTime,
    ADD COLUMN checkInLongitude decimal(10,7) NULL COMMENT '签到时经度' AFTER checkInLatitude,
    ADD COLUMN checkInDistanceMeters decimal(10,2) NULL COMMENT '签到距离米数' AFTER checkInLongitude,
    ADD INDEX idx_attendance_status (attendanceStatus),
    ADD INDEX idx_check_in_time (checkInTime);

UPDATE schedule_event
SET checkInEnabled = 0,
    checkInRadiusMeters = 200
WHERE checkInEnabled IS NULL OR checkInRadiusMeters IS NULL;

UPDATE schedule_participant
SET attendanceStatus = 'not_checked'
WHERE attendanceStatus IS NULL OR attendanceStatus = '';
