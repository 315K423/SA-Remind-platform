<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-card :bordered="false">
      <a-row justify="space-between" align="middle">
        <a-col>
          <h2 style="margin: 0 0 8px">欢迎使用企业日程与考勤智能提醒平台</h2>
          <div style="color: #8c8c8c">
            面向管理员与员工的统一后台，支持日程管理、提醒策略配置和弹窗提醒处理。
          </div>
        </a-col>
        <a-col>
          <a-space>
            <a-button type="primary" @click="goTo('/schedule/manage')">进入日程管理</a-button>
            <a-button @click="goTo('/reminder/rule')">设置提醒策略</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :md="8">
        <a-card>
          <a-statistic title="日程管理" value="事件创建 / 修改 / 删除" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card>
          <a-statistic title="提醒策略" value="提前提醒 / 重复提醒" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card>
          <a-statistic title="弹窗提醒" value="全局轮询 / 已读处理" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="14">
        <a-card title="系统功能说明" :bordered="false">
          <a-timeline>
            <a-timeline-item>员工可创建并维护个人日程，支持地点、全天事件、参与人等信息。</a-timeline-item>
            <a-timeline-item>员工可为指定日程配置提醒时间、重复次数和重复间隔。</a-timeline-item>
            <a-timeline-item>后端定时生成提醒任务，前端全局轮询后自动弹窗提示。</a-timeline-item>
            <a-timeline-item>管理员可在用户管理页面统一查看和维护用户信息。</a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="快捷入口" :bordered="false">
          <a-list :data-source="quickEntries" bordered>
            <template #renderItem="{ item }">
              <a-list-item>
                <a-button type="link" @click="goTo(item.path)">{{ item.title }}</a-button>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </a-space>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const quickEntries = [
  { title: '进入日程管理', path: '/schedule/manage' },
  { title: '配置提醒策略', path: '/reminder/rule' },
  { title: '查看弹窗提醒', path: '/reminder/popup' },
  { title: '管理员用户管理', path: '/admin/userManage' },
]

const goTo = (path: string) => {
  router.push(path)
}
</script>
