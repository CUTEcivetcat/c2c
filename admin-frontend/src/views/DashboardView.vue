<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">📊 数据大屏</h2>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-num">{{ stats.totalUsers }}</div><div class="stat-label">累计用户</div></div>
      <div class="stat-card"><div class="stat-num green">{{ stats.totalProducts }}</div><div class="stat-label">总商品数</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ stats.onSaleProducts }}</div><div class="stat-label">在售商品</div></div>
      <div class="stat-card"><div class="stat-num red">{{ stats.todayOrders }}</div><div class="stat-label">今日订单</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#8e44ad">{{ stats.totalAnnouncements }}</div><div class="stat-label">公告总数</div></div>
    </div>

    <!-- 待处理卡片（点击跳转对应审核页） -->
    <el-row :gutter="20" class="pending-row">
      <el-col :span="8">
        <el-card shadow="hover" class="pending-card" @click="$router.push('/review?tab=report')">
          <div class="pending-inner">
            <span class="pending-ico">⚑</span>
            <div class="pending-text"><strong>{{ stats.pendingReports }}</strong><span>待处理举报</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="pending-card" @click="$router.push('/review?tab=appeal')">
          <div class="pending-inner">
            <span class="pending-ico">🔁</span>
            <div class="pending-text"><strong>{{ stats.pendingAppeals }}</strong><span>待审核申诉</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="pending-card" @click="$router.push('/review?tab=nickname')">
          <div class="pending-inner">
            <span class="pending-ico">👤</span>
            <div class="pending-text"><strong>{{ stats.pendingNicknames }}</strong><span>待审昵称</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 折线图 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-head">
          <h3>{{ userRangeLabel }}用户新增趋势</h3>
          <div class="chart-range">
            <el-select v-model="userRange" @change="loadUserTrend" size="small" style="width:100px">
              <el-option label="近 7 天" value="7d" />
              <el-option label="近 30 天" value="30d" />
              <el-option label="自定义" value="custom" />
            </el-select>
            <template v-if="userRange === 'custom'">
              <el-date-picker v-model="userStart" type="date" placeholder="开始" value-format="YYYY-MM-DD" @change="loadUserTrend" size="small" style="width:120px" />
              <span style="color:#b2bec3">~</span>
              <el-date-picker v-model="userEnd" type="date" placeholder="结束" value-format="YYYY-MM-DD" @change="loadUserTrend" size="small" style="width:120px" />
            </template>
          </div>
        </div>
        <div ref="userChartRef" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">
          <h3>{{ orderRangeLabel }}订单与交易额</h3>
          <div class="chart-range">
            <el-select v-model="orderRange" @change="loadOrderTrend" size="small" style="width:100px">
              <el-option label="近 7 天" value="7d" />
              <el-option label="近 30 天" value="30d" />
              <el-option label="自定义" value="custom" />
            </el-select>
            <template v-if="orderRange === 'custom'">
              <el-date-picker v-model="orderStart" type="date" placeholder="开始" value-format="YYYY-MM-DD" @change="loadOrderTrend" size="small" style="width:120px" />
              <span style="color:#b2bec3">~</span>
              <el-date-picker v-model="orderEnd" type="date" placeholder="结束" value-format="YYYY-MM-DD" @change="loadOrderTrend" size="small" style="width:120px" />
            </template>
          </div>
        </div>
        <div ref="orderChartRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <el-card style="border-radius:14px;margin-top:20px">
      <h3 style="margin-bottom:12px">快捷操作</h3>
      <div style="display:flex;gap:12px;flex-wrap:wrap">
        <el-button type="primary" @click="$router.push('/users')">👥 用户管理</el-button>
        <el-button type="primary" @click="$router.push('/products')">📦 商品管理</el-button>
        <el-button type="primary" @click="$router.push('/orders')">📋 订单管理</el-button>
        <el-button type="primary" plain @click="$router.push('/review')">🛡️ 审核管理</el-button>
        <el-button type="primary" plain @click="$router.push('/announcement')">📢 公告管理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { adminApi } from '@/api/request'
import * as echarts from 'echarts'

const stats = ref({ totalUsers: 0, totalProducts: 0, onSaleProducts: 0, todayOrders: 0 })
const userChartRef = ref(null)
const orderChartRef = ref(null)
let userChart = null
let orderChart = null
let ro = null

// 用户图表独立选择
const userRange = ref('7d')
const userStart = ref('')
const userEnd = ref('')
const userRangeLabel = computed(() => userRange.value === '30d' ? '近 30 日' : userRange.value === 'custom' ? '自定义' : '近 7 日')

// 订单图表独立选择
const orderRange = ref('7d')
const orderStart = ref('')
const orderEnd = ref('')
const orderRangeLabel = computed(() => orderRange.value === '30d' ? '近 30 日' : orderRange.value === 'custom' ? '自定义' : '近 7 日')

const buildParams = (range, start, end) => {
  const params = { range }
  if (range === 'custom' && start && end) { params.start = start; params.end = end }
  return params
}

const loadUserTrend = async () => {
  try {
    const trends = await adminApi.getTrends(buildParams(userRange.value, userStart.value, userEnd.value))
    if (trends) {
      userChart?.dispose(); userChart = null
      nextTick(() => initUserChart(trends))
    }
  } catch (e) { /* */ }
}

const loadOrderTrend = async () => {
  try {
    const trends = await adminApi.getTrends(buildParams(orderRange.value, orderStart.value, orderEnd.value))
    if (trends) {
      orderChart?.dispose(); orderChart = null
      nextTick(() => initOrderChart(trends))
    }
  } catch (e) { /* */ }
}

onMounted(async () => {
  try {
    const [summary, userTrends, orderTrends] = await Promise.all([
      adminApi.getSummary(),
      adminApi.getTrends({ range: '7d' }),
      adminApi.getTrends({ range: '7d' })
    ])
    stats.value = {
      totalUsers: summary.totalUsers || 0,
      totalProducts: summary.totalProducts || 0,
      onSaleProducts: summary.onSaleProducts || 0,
      todayOrders: summary.todayOrders || 0,
      totalAnnouncements: summary.totalAnnouncements || 0,
      pendingReports: summary.pendingReports || 0,
      pendingAppeals: summary.pendingAppeals || 0,
      pendingNicknames: summary.pendingNicknames || 0
    }
    nextTick(() => {
      if (userTrends) initUserChart(userTrends)
      if (orderTrends) initOrderChart(orderTrends)
    })
  } catch (e) { /* */ }

  window.addEventListener('resize', resizeCharts)
  ro = new ResizeObserver(() => resizeCharts())
  if (userChartRef.value) ro.observe(userChartRef.value)
  if (orderChartRef.value) ro.observe(orderChartRef.value)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  ro?.disconnect()
  userChart?.dispose()
  orderChart?.dispose()
})

const resizeCharts = () => { userChart?.resize(); orderChart?.resize() }

const initUserChart = (trends) => {
  const { dates, newUsers } = trends
  if (!userChartRef.value) return
  userChart = echarts.init(userChartRef.value)
  userChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ name: '新增用户', type: 'line', smooth: true, data: newUsers, lineStyle: { color: '#409eff', width: 3 }, itemStyle: { color: '#409eff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(64,158,255,0.35)'},{offset:1,color:'rgba(64,158,255,0.02)'}]) } }]
  })
}

const initOrderChart = (trends) => {
  const { dates, orders, revenue } = trends
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '交易额'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: [{ type: 'value', name: '订单', minInterval: 1 }, { type: 'value', name: '元' }],
    series: [
      { name: '订单数', type: 'bar', data: orders, itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#ff6b35'},{offset:1,color:'#ffaa7a'}]), borderRadius: [6,6,0,0] }, barWidth: '30%' },
      { name: '交易额', type: 'line', smooth: true, yAxisIndex: 1, data: revenue, lineStyle: { color: '#00b894', width: 3 }, itemStyle: { color: '#00b894' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,184,148,0.3)'},{offset:1,color:'rgba(0,184,148,0.02)'}]) } }
    ]
  })
}
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr) } }
.stat-card { background: #fff; border-radius: 14px; padding: 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); text-align: center; transition: transform 0.2s }
.stat-card:hover { transform: translateY(-2px) }
.stat-num { font-size: 36px; font-weight: 700; color: #409eff }
.stat-num.green { color: #67c23a } .stat-num.orange { color: #e6a23c } .stat-num.red { color: #e74c3c }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px }

.pending-row { margin-bottom: 20px; }
.pending-card { border-radius: 14px !important; cursor: pointer; transition: all 0.25s; }
.pending-card:hover { transform: translateY(-3px); }
.pending-inner { display: flex; align-items: center; gap: 14px; }
.pending-ico { font-size: 28px; }
.pending-card:nth-child(1) .pending-ico { color: #ff6b35; }
.pending-card:nth-child(2) .pending-ico { color: #e67e22; }
.pending-card:nth-child(3) .pending-ico { color: #d35400; }
.pending-text { display: flex; flex-direction: column; line-height: 1.3; }
.pending-text strong { font-size: 26px; font-weight: 800; color: #2d3436; }
.pending-text span { font-size: 13px; color: #909399; }

.chart-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.chart-range { display: flex; align-items: center; gap: 8px; }
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px }
@media (max-width: 900px) { .chart-row { grid-template-columns: 1fr } }
.chart-card { background: #fff; border-radius: 14px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.04) }
.chart-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; gap: 8px; flex-wrap: wrap; }
.chart-head h3 { font-size: 14px; color: #2d3436; margin: 0; }
.chart-range { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.chart-box { width: 100%; height: 300px }
</style>
