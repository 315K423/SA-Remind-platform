<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card :bordered="false">
      <a-row justify="space-between" align="middle" :gutter="16">
        <a-col :xs="24" :lg="16">
          <h2 style="margin: 0 0 8px">欢迎使用企业日程与考勤智能提醒平台</h2>
          <div style="color: #8c8c8c; margin-bottom: 8px">
            围绕“日程—提醒—公告—定位签到—考勤管理”的闭环管理目标，支持管理员、部门经理与普通员工的分角色操作。
          </div>
          <a-space wrap>
            <a-tag color="blue">{{ getRoleLabel(loginUserStore.loginUser.userRole) }}</a-tag>
            <a-tag v-if="loginUserStore.loginUser.departmentName" color="geekblue">
              {{ loginUserStore.loginUser.departmentName }}
            </a-tag>
          </a-space>
        </a-col>
        <a-col :xs="24" :lg="8" style="text-align: right">
          <a-space wrap>
            <a-button @click="goTo('/schedule/manage')">日程管理</a-button>
            <a-button @click="goTo('/announcement/list')">公告通知</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

<!--    这里进行修改 - 改为月日程表-->
    <a-row :gutter="16">
      <a-col :xs="24" :md="6">
        <a-card><a-statistic title="我的日程" value="月历 / 日视图" /></a-card>
      </a-col>
      <a-col :xs="24" :md="6">
        <a-card><a-statistic title="日程管理" value="创建 / 冲突校验" /></a-card>
      </a-col>
      <a-col :xs="24" :md="6">
        <a-card><a-statistic title="提醒策略" value="重复提醒 / 定位签到" /></a-card>
      </a-col>
      <a-col :xs="24" :md="6">
        <a-card><a-statistic title="公告 / 考勤" value="部门公告 / 签到管理" /></a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="14">
        <a-card title="系统功能说明" :bordered="false">
          <a-timeline>
            <a-timeline-item>管理员可维护部门、员工归属、公告范围、考勤状态与跨部门协同日程。</a-timeline-item>
            <a-timeline-item>部门经理仅可查看本部门相关日程，并为本部门成员安排参与人。</a-timeline-item>
            <a-timeline-item>员工可在“我的日程”中查看自己创建和参与的日程，并进入当日 24 小时视图。</a-timeline-item>
            <a-timeline-item>考勤事项支持签到地点、签到半径和浏览器定位签到，签到成功后会自动完成提醒确认。</a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </a-space>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { getRoleLabel, isAdmin } from '@/utils/app'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const goTo = (path: string) => {
  router.push(path)
}
</script>
