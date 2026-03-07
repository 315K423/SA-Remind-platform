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
