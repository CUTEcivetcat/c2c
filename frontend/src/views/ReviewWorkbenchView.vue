<template>
  <div class="page-container review-workbench" style="max-width:760px">
    <page-back-bar title="审核工作台" />
    <p class="wb-tip">你是「审核员」，这里处理用户举报与卖家整改申诉。处理结果会通过站内信通知相关方。</p>

    <el-tabs v-model="tab">
      <!-- ==================== 举报审核 ==================== -->
      <el-tab-pane label="举报审核" name="report">
        <div class="filter-row">
          <el-select v-model="reportStatus" size="small" style="width:130px" @change="loadReports">
            <el-option :value="1" label="待处理" /><el-option :value="2" label="已违规下架" /><el-option :value="3" label="已驳回" />
          </el-select>
          <el-button size="small" type="primary" plain @click="loadReports" :loading="reportLoading">刷新</el-button>
        </div>

        <div v-loading="reportLoading" class="card-list">
          <div v-for="r in reports" :key="r.id" class="review-card">
            <div class="card-head">
              <el-image v-if="r.productCover" :src="r.productCover" class="card-cover" fit="cover" />
              <div class="card-title-box">
                <div class="card-title">{{ r.productTitle }}</div>
                <div class="card-sub">
                  <el-tag size="small" type="warning">{{ r.reportTypeText }}</el-tag>
                  <el-tag size="small" :type="reportStatusType(r.status)">{{ reportStatusText(r.status) }}</el-tag>
                </div>
              </div>
            </div>
            <div class="card-body">
              <div class="line">举报人：<b>{{ r.reporterNickname }}</b> ｜ 卖家：<b>{{ r.sellerNickname }}</b></div>
              <div class="line reason">理由：{{ r.reason }}</div>
              <div v-if="r.handleRemark" class="line remark">处理备注：{{ r.handleRemark }}</div>
            </div>
            <div class="card-foot">
              <span class="time">{{ fmtTime(r.createdAt) }}</span>
              <el-button size="small" type="primary" plain @click="openReport(r.id)">查看详情</el-button>
            </div>
          </div>
          <el-empty v-if="!reportLoading && !reports.length" description="暂无举报记录" />
        </div>
        <div v-if="reportTotal > 10" class="pager">
          <el-pagination layout="prev,pager,next" :total="reportTotal" :page-size="10" v-model:current-page="reportPage" @change="loadReports" />
        </div>
      </el-tab-pane>

      <!-- ==================== 整改申诉审核 ==================== -->
      <el-tab-pane label="整改申诉审核" name="appeal">
        <div class="filter-row">
          <el-select v-model="appealStatus" size="small" style="width:130px" @change="loadAppeals">
            <el-option :value="1" label="待审核" /><el-option :value="2" label="已通过" /><el-option :value="3" label="已驳回" />
          </el-select>
          <el-button size="small" type="primary" plain @click="loadAppeals" :loading="appealLoading">刷新</el-button>
        </div>

        <div v-loading="appealLoading" class="card-list">
          <div v-for="a in appeals" :key="a.id" class="review-card">
            <div class="card-head">
              <el-image v-if="a.productCover" :src="a.productCover" class="card-cover" fit="cover" />
              <div class="card-title-box">
                <div class="card-title">{{ a.productTitle }}</div>
                <div class="card-sub">
                  <el-tag size="small" type="info">第 {{ a.appealCount }}/3 次</el-tag>
                  <el-tag size="small" :type="appealStatusType(a.status)">{{ appealStatusText(a.status) }}</el-tag>
                </div>
              </div>
            </div>
            <div class="card-body">
              <div class="line">卖家：<b>{{ a.sellerNickname }}</b> ｜ 价格：<b>¥{{ a.productPrice }}</b></div>
              <div class="line reason">整改说明：{{ a.appealReason }}</div>
              <div v-if="a.reply" class="line remark">审核回复：{{ a.reply }}</div>
            </div>
            <div class="card-foot">
              <span class="time">{{ fmtTime(a.createdAt) }}</span>
              <el-button size="small" type="primary" plain @click="openAppeal(a.id)">查看详情</el-button>
            </div>
          </div>
          <el-empty v-if="!appealLoading && !appeals.length" description="暂无申诉记录" />
        </div>
        <div v-if="appealTotal > 10" class="pager">
          <el-pagination layout="prev,pager,next" :total="appealTotal" :page-size="10" v-model:current-page="appealPage" @change="loadAppeals" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 举报详情 ==================== -->
    <el-dialog v-model="reportDialog" title="举报详情" width="92%" style="max-width:560px">
      <template v-if="reportDetail">
        <div class="detail-product">
          <el-image v-if="reportDetail.productCover" :src="reportDetail.productCover" class="dp-cover" fit="cover" />
          <div class="dp-info">
            <div class="dp-title">{{ reportDetail.productTitle }}</div>
            <div class="dp-price">¥{{ reportDetail.productPrice }}</div>
            <div class="dp-tags">
              <el-tag size="small">{{ productStatusText(reportDetail.productStatus) }}</el-tag>
              <el-tag v-if="reportDetail.productReviewReason" size="small" type="danger">{{ reportDetail.productReviewReason }}</el-tag>
            </div>
          </div>
        </div>
        <div class="detail-block">
          <div class="line">举报人：<b>{{ reportDetail.reporterNickname }}</b> ｜ 卖家：<b>{{ reportDetail.sellerNickname }}</b></div>
          <div class="line">举报类型：<b>{{ reportDetail.reportTypeText }}</b></div>
          <div class="line reason">举报理由：{{ reportDetail.reason }}</div>
          <div v-if="reportDetail.handleRemark" class="line remark">处理备注：{{ reportDetail.handleRemark }}</div>
        </div>
        <div v-if="reportDetail.status === 1" class="handle-row">
          <el-button type="danger" :loading="handling" @click="doHandleReport('ban')">违规下架</el-button>
          <el-button type="info" :loading="handling" @click="doHandleReport('reject')">驳回</el-button>
          <el-button type="primary" plain :loading="handling" @click="contactSeller(reportDetail.sellerId)">联系卖家</el-button>
          <el-button type="primary" plain :loading="handling" @click="contactSeller(reportDetail.reporterId)">联系举报人</el-button>
        </div>
        <div v-else class="handle-row" style="justify-content:space-between">
          <span style="color:#b2bec3;font-size:13px">该举报已处理</span>
          <el-button type="primary" plain size="small" @click="contactSeller(reportDetail.sellerId)">联系卖家</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ==================== 申诉详情 ==================== -->
    <el-dialog v-model="appealDialog" title="整改申诉详情" width="92%" style="max-width:560px">
      <template v-if="appealDetail">
        <div class="detail-product">
          <el-image v-if="appealDetail.productCover" :src="appealDetail.productCover" class="dp-cover" fit="cover" />
          <div class="dp-info">
            <div class="dp-title">{{ appealDetail.productTitle }}</div>
            <div class="dp-price">¥{{ appealDetail.productPrice }}</div>
            <div class="dp-tags">
              <el-tag size="small">{{ productStatusText(appealDetail.productStatus) }}</el-tag>
              <el-tag v-if="appealDetail.productReviewReason" size="small" type="danger">下架：{{ appealDetail.productReviewReason }}</el-tag>
            </div>
          </div>
        </div>
        <div class="detail-block">
          <div class="line">卖家：<b>{{ appealDetail.sellerNickname }}</b> ｜ 第 {{ appealDetail.appealCount }}/3 次</div>
          <div class="line reason">整改说明：{{ appealDetail.appealReason }}</div>
          <div v-if="appealDetail.reply" class="line remark">审核回复：{{ appealDetail.reply }}</div>
        </div>
        <div v-if="appealDetail.status === 1" class="handle-row">
          <el-button type="success" :loading="handling" @click="doHandleAppeal('approve')">通过并恢复上架</el-button>
          <el-button type="danger" :loading="handling" @click="doHandleAppeal('reject')">驳回</el-button>
          <el-button type="primary" plain :loading="handling" @click="contactSeller(appealDetail.sellerId)">联系卖家</el-button>
        </div>
        <div v-else class="handle-row" style="justify-content:space-between">
          <span style="color:#b2bec3;font-size:13px">该申诉已处理</span>
          <el-button type="primary" plain size="small" @click="contactSeller(appealDetail.sellerId)">联系卖家</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getReports, getReportDetail, handleReport,
  getAppeals, getAppealDetail, handleAppeal
} from '@/api/review'
import { getOrCreateConversation } from '@/api/im'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const tab = ref('report')

const reports = ref([])
const reportTotal = ref(0)
const reportPage = ref(1)
const reportStatus = ref(1)
const reportLoading = ref(false)
const reportDialog = ref(false)
const reportDetail = ref(null)

const appeals = ref([])
const appealTotal = ref(0)
const appealPage = ref(1)
const appealStatus = ref(1)
const appealLoading = ref(false)
const appealDialog = ref(false)
const appealDetail = ref(null)

const handling = ref(false)

const reportStatusMap = { 1: '待处理', 2: '已违规下架', 3: '已驳回' }
const reportStatusText = (s) => reportStatusMap[s] || '未知'
const reportStatusType = (s) => ({ 1: 'warning', 2: 'danger', 3: 'info' }[s] || '')
const appealStatusMap = { 1: '待审核', 2: '已通过', 3: '已驳回' }
const appealStatusText = (s) => appealStatusMap[s] || '未知'
const appealStatusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || '')
const productStatusMap = { 1: '在售', 2: '已预定', 3: '已售', 4: '下架', 5: '违规下架' }
const productStatusText = (s) => productStatusMap[s] || '未知'
const fmtTime = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')

const loadReports = async () => {
  reportLoading.value = true
  try {
    const res = await getReports({ status: reportStatus.value, page: reportPage.value, size: 10 })
    reports.value = res.records || []
    reportTotal.value = res.total || 0
  } catch (e) { /* 拦截器已提示 */ } finally { reportLoading.value = false }
}

const loadAppeals = async () => {
  appealLoading.value = true
  try {
    const res = await getAppeals({ status: appealStatus.value, page: appealPage.value, size: 10 })
    appeals.value = res.records || []
    appealTotal.value = res.total || 0
  } catch (e) { /* 拦截器已提示 */ } finally { appealLoading.value = false }
}

const openReport = async (id) => {
  try {
    reportDetail.value = await getReportDetail(id)
    reportDialog.value = true
  } catch (e) { /* 拦截器已提示 */ }
}

const openAppeal = async (id) => {
  try {
    appealDetail.value = await getAppealDetail(id)
    appealDialog.value = true
  } catch (e) { /* 拦截器已提示 */ }
}

const doHandleReport = async (action) => {
  const d = reportDetail.value
  let reason = ''
  if (action === 'ban') {
    try {
      const { value } = await ElMessageBox.prompt(
        `请填写「${d.productTitle}」的违规下架原因，卖家会看到该原因。`, '违规下架',
        { inputType: 'textarea', inputPlaceholder: '例如：涉嫌违禁品 / 假冒商品', confirmButtonText: '确认下架', cancelButtonText: '取消' }
      )
      reason = (value || '').trim()
    } catch (e) { return }
    if (!reason) { ElMessage.warning('请填写违规原因'); return }
  } else {
    try {
      const { value } = await ElMessageBox.prompt('请填写驳回说明，举报人可看到。', '驳回举报',
        { inputType: 'textarea', inputPlaceholder: '说明为何不予处理', confirmButtonText: '确认驳回', cancelButtonText: '取消' })
      reason = (value || '').trim()
    } catch (e) { return }
    if (!reason) { ElMessage.warning('请填写驳回说明'); return }
  }
  handling.value = true
  try {
    await handleReport(d.id, { action, reason })
    ElMessage.success(action === 'ban' ? '已违规下架并通知卖家' : '已驳回举报')
    reportDialog.value = false
    loadReports()
  } catch (e) { /* 拦截器已提示 */ } finally { handling.value = false }
}

const doHandleAppeal = async (action) => {
  const d = appealDetail.value
  let reply = ''
  if (action === 'approve') {
    try {
      await ElMessageBox.confirm(
        `确定通过「${d.productTitle}」的整改申诉并恢复上架吗？`, '通过申诉',
        { type: 'warning', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
      )
    } catch (e) { return }
  } else {
    try {
      const { value } = await ElMessageBox.prompt('请填写驳回说明，卖家可看到。', '驳回申诉',
        { inputType: 'textarea', inputPlaceholder: '说明仍需整改的内容', confirmButtonText: '确认驳回', cancelButtonText: '取消' })
      reply = (value || '').trim()
    } catch (e) { return }
    if (!reply) { ElMessage.warning('请填写驳回说明'); return }
  }
  handling.value = true
  try {
    await handleAppeal(d.id, { action, reason: reply })
    ElMessage.success(action === 'approve' ? '已通过并恢复上架' : '已驳回申诉')
    appealDialog.value = false
    loadAppeals()
  } catch (e) { /* 拦截器已提示 */ } finally { handling.value = false }
}

const contactSeller = async (userId) => {
  const productId = reportDetail.value?.productId || appealDetail.value?.productId
  try {
    const conv = await getOrCreateConversation({ targetUserId: userId, productId })
    router.push(`/chat/${conv.id}`)
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => { loadReports(); loadAppeals() })
</script>

<style scoped>
.wb-tip { font-size: 13px; color: #636e72; background: #fff8f5; border: 1px solid #ffe4d6; border-radius: 12px; padding: 10px 14px; margin-bottom: 16px; }
.filter-row { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.card-list { display: flex; flex-direction: column; gap: 12px; min-height: 120px; }
.review-card { background: #fff; border-radius: 16px; padding: 14px 16px; border: 1px solid #f0f2f5; }
.card-head { display: flex; gap: 12px; align-items: center; }
.card-cover { width: 64px; height: 64px; border-radius: 12px; flex-shrink: 0; }
.card-title-box { flex: 1; min-width: 0; }
.card-title { font-weight: 700; color: #2d3436; font-size: 15px; margin-bottom: 6px; word-break: break-all; }
.card-sub { display: flex; gap: 6px; flex-wrap: wrap; }
.card-body { margin-top: 10px; background: #f8f9fa; border-radius: 10px; padding: 10px 12px; }
.line { font-size: 13px; color: #636e72; line-height: 1.7; }
.line b { color: #2d3436; }
.line.reason { color: #2d3436; }
.line.remark { color: #e74c3c; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.time { font-size: 12px; color: #b2bec3; }
.pager { text-align: center; margin-top: 16px; }
.detail-product { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
.dp-cover { width: 96px; height: 96px; border-radius: 12px; flex-shrink: 0; }
.dp-info { flex: 1; min-width: 0; }
.dp-title { font-weight: 700; color: #2d3436; font-size: 16px; word-break: break-all; }
.dp-price { color: #e74c3c; font-size: 20px; font-weight: 800; margin: 4px 0; }
.dp-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.detail-block { background: #f8f9fa; border-radius: 12px; padding: 12px 14px; }
.handle-row { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; justify-content: flex-end; }
</style>
