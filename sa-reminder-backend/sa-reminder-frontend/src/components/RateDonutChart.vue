<template>
  <div ref="chartRef" class="rate-donut-chart" :style="chartStyle" />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(
  defineProps<{
    rate?: number
    size?: number
    title?: string
  }>(),
  {
    rate: 0,
    size: 88,
    title: '完成率',
  },
)

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const normalizedRate = computed(() => {
  const value = Number(props.rate || 0)
  if (Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, Number(value.toFixed(2))))
})

const chartStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
}))

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const rate = normalizedRate.value
  chart.setOption({
    title: {
      text: `${rate}%`,
      subtext: props.title,
      left: 'center',
      top: 'center',
      textStyle: {
        fontSize: props.size >= 120 ? 22 : 14,
        fontWeight: 700,
      },
      subtextStyle: {
        fontSize: props.size >= 120 ? 12 : 10,
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%',
    },
    series: [
      {
        type: 'pie',
        radius: ['72%', '88%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: {
          show: false,
        },
        labelLine: {
          show: false,
        },
        data: [
          { value: rate, name: '已完成' },
          { value: Number((100 - rate).toFixed(2)), name: '未完成' },
        ],
      },
    ],
  })
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  void renderChart()
  window.addEventListener('resize', handleResize)
})

watch(() => [props.rate, props.size, props.title], renderChart)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.rate-donut-chart {
  display: inline-block;
}
</style>
