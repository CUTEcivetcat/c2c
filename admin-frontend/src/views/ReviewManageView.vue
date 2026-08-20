<template>
  <div>
    <h2 style="font-size:22px;margin-bottom:20px;color:#2d3436">🛡️ 审核管理</h2>

    <el-tabs v-model="tab" style="margin-bottom:16px">
      <!-- ==================== 举报审核 ==================== -->
      <el-tab-pane label="举报审核" name="report">
        <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
          <el-select v-model="reportStatus" placeholder="状态筛选" clearable style="width:150px" @change="loadReports">
            <el-option :value="1" label="待处理" /><el-option :value="2" label="已违规下架" /><el-option :value="3" label="已驳回" />
          </el-select>
          <el-button type="primary" @click="loadReports" :loading="reportLoading">刷新</el-button>
        </div>
        <el-card style="border-radius:14px">
          <el-table :data="reports" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="productTitle" label="商品" min-width="180"><template #default="{row}">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image v-if="row.productCover" :src="row.productCover" style="width:40px;height:40px;border-radius:8px;flex-shrink:0" fit="cover" />
                <span style="font-weight:600">{{ row.productTitle }}</span>
              </div>
            </template></el-table-column>
            <el-table-column prop="reporterNickname" label="举报人" width="110" />
            <el-table-column prop="sellerNickname" label="卖家" width="110" />
            <el-table-column prop="reportTypeText" label="类型" width="90" />
            <el-table-column prop="reason" label="举报理由" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{row}">
              <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusText(row.status) }}</el-tag>
            </template></el-table-column>
            <el-table-column label="举报时间" width="160"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{row}">
              <el-button type="primary" size="small" plain @click="openReport(row.id)">查看</el-button>
            </template></el-table-column>
          </el-table>
          <div style="margin-top:16px;text-align:center">
            <el-pagination layout="prev,pager,next" :total="reportTotal" :page-size="10" v-model:current-page="reportPage" @change="loadReports" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== 整改申诉审核 ==================== -->
      <el-tab-pane label="整改申诉审核" name="appeal">
        <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
          <el-select v-model="appealStatus" placeholder="状态筛选" clearable style="width:150px" @change="loadAppeals">
            <el-option :value="1" label="待审核" /><el-option :value="2" label="已通过" /><el-option :value="3" label="已驳回" />
          </el-select>
          <el-button type="primary" @click="loadAppeals" :loading="appealLoading">刷新</el-button>
        </div>
        <el-card style="border-radius:14px">
          <el-table :data="appeals" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="productTitle" label="商品" min-width="180"><template #default="{row}">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image v-if="row.productCover" :src="row.productCover" style="width:40px;height:40px;border-radius:8px;flex-shrink:0" fit="cover" />
                <span style="font-weight:600">{{ row.productTitle }}</span>
              </div>
            </template></el-table-column>
            <el-table-column prop="sellerNickname" label="卖家" width="110" />
            <el-table-column prop="appealReason" label="整改说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="appealCount" label="第几次" width="80" />
            <el-table-column label="状态" width="100"><template #default="{row}">
              <el-tag :type="appealStatusType(row.status)" size="small">{{ appealStatusText(row.status) }}</el-tag>
            </template></el-table-column>
            <el-table-column label="申诉时间" width="160"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{row}">
              <el-button type="primary" size="small" plain @click="openAppeal(row.id)">查看</el-button>
            </template></el-table-column>
          </el-table>
          <div style="margin-top:16px;text-align:center">
            <el-pagination layout="prev,pager,next" :total="appealTotal" :page-size="10" v-model:current-page="appealPage" @change="loadAppeals" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== 昵称审核 ==================== -->
      <el-tab-pane label="昵称审核" name="nickname">
        <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
          <el-select v-model="nicknameStatus" placeholder="状态筛选" clearable style="width:150px" @change="loadNicknames">
            <el-option :value="0" label="待审核" /><el-option :value="1" label="已通过" /><el-option :value="2" label="已拒绝" />
          </el-select>
          <el-button type="primary" @click="loadNicknames" :loading="nicknameLoading">刷新</el-button>
        </div>
        <el-card style="border-radius:14px">
          <el-table :data="nicknames" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userName" label="申请人" width="130" />
            <el-table-column prop="oldNickname" label="原昵称" width="130"><template #default="{row}">
              <span style="color:#b2bec3;text-decoration:line-through">{{ row.oldNickname || '-' }}</span>
            </template></el-table-column>
            <el-table-column prop="newNickname" label="新昵称" min-width="140"><template #default="{row}">
              <span style="font-weight:600;color:#e55a2b">{{ row.newNickname }}</span>
            </template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{row}">
              <el-tag :type="nicknameStatusType(row.status)" size="small">{{ nicknameStatusText(row.status) }}</el-tag>
            </template></el-table-column>
            <el-table-column prop="reason" label="拒绝原因" min-width="140" show-overflow-tooltip />
            <el-table-column label="申请时间" width="160"><template #default="{row}">{{ fmtTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="操作" width="150" fixed="right"><template #default="{row}">
              <template v-if="row.status === 0">
                <el-button type="success" size="small" plain @click="approveNickname(row)">通过</el-button>
                <el-button type="danger" size="small" plain @click="rejectNickname(row)">拒绝</el-button>
              </template>
              <span v-else style="color:#b2bec3;font-size:12px">已处理</span>
            </template></el-table-column>
          </el-table>
          <div style="margin-top:16px;text-align:center">
            <el-pagination layout="prev,pager,next" :total="nicknameTotal" :page-size="10" v-model:current-page="nicknamePage" @change="loadNicknames" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 举报详情 ==================== -->
    <el-dialog v-model="reportDialog" title="举报详情" width="620px">
      <template v-if="reportDetail">
        <el-descriptions title="被举报商品" :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="封面" :span="2">
            <el-image v-if="reportDetail.productCover" :src="reportDetail.productCover" style="width:100px;height:100px;border-radius:10px" fit="cover" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="标题">{{ reportDetail.productTitle }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ reportDetail.productPrice }}</el-descriptions-item>
          <el-descriptions-item label="商品状态">{{ productStatusText(reportDetail.productStatus) }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ reportDetail.sellerNickname }}</el-descriptions-item>
          <el-descriptions-item label="违规原因" :span="2">
            <span v-if="reportDetail.productReviewReason" style="color:#e74c3c">{{ reportDetail.productReviewReason }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="举报信息" :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="举报人">{{ reportDetail.reporterNickname }}</el-descriptions-item>
          <el-descriptions-item label="举报类型">{{ reportDetail.reportTypeText }}</el-descriptions-item>
          <el-descriptions-item label="举报理由" :span="2">{{ reportDetail.reason }}</el-descriptions-item>
          <el-descriptions-item label="附图" :span="2">
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-image v-for="(u, i) in splitImages(reportDetail.images)" :key="i" :src="u" :preview-src-list="splitImages(reportDetail.images)" style="width:80px;height:80px;border-radius:8px" fit="cover" />
              <span v-if="!reportDetail.images">-</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2"><span style="color:#636e72">{{ reportDetail.handleRemark || '-' }}</span></el-descriptions-item>
        </el-descriptions>

        <div v-if="reportDetail.status === 1" style="display:flex;gap:10px;justify-content:flex-end">
          <el-button type="danger" :loading="handling" @click="handleReport('ban')">违规下架（填原因）</el-button>
          <el-button type="info" :loading="handling" @click="handleReport('reject')">驳回（填说明）</el-button>
        </div>
        <div v-else style="text-align:right;color:#b2bec3">该举报已处理</div>
      </template>
    </el-dialog>

    <!-- ==================== 申诉详情 ==================== -->
    <el-dialog v-model="appealDialog" title="整改申诉详情" width="620px">
      <template v-if="appealDetail">
        <el-descriptions title="商品信息" :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="封面" :span="2">
            <el-image v-if="appealDetail.productCover" :src="appealDetail.productCover" style="width:100px;height:100px;border-radius:10px" fit="cover" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="标题">{{ appealDetail.productTitle }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ appealDetail.productPrice }}</el-descriptions-item>
          <el-descriptions-item label="商品状态">{{ productStatusText(appealDetail.productStatus) }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ appealDetail.sellerNickname }}</el-descriptions-item>
          <el-descriptions-item label="下架原因" :span="2">
            <span v-if="appealDetail.productReviewReason" style="color:#e74c3c">{{ appealDetail.productReviewReason }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="申诉信息" :column="2" border>
          <el-descriptions-item label="整改说明" :span="2">{{ appealDetail.appealReason }}</el-descriptions-item>
          <el-descriptions-item label="附图" :span="2">
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-image v-for="(u, i) in splitImages(appealDetail.images)" :key="i" :src="u" :preview-src-list="splitImages(appealDetail.images)" style="width:80px;height:80px;border-radius:8px" fit="cover" />
              <span v-if="!appealDetail.images">-</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="第几次申诉">{{ appealDetail.appealCount }} / 3</el-descriptions-item>
          <el-descriptions-item label="状态">{{ appealStatusText(appealDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="审核回复" :span="2"><span style="color:#636e72">{{ appealDetail.reply || '-' }}</span></el-descriptions-item>
        </el-descriptions>

        <div v-if="appealDetail.status === 1" style="display:flex;gap:10px;justify-content:flex-end;margin-top:16px">
          <el-button type="success" :loading="handling" @click="handleAppeal('approve')">通过并恢复上架</el-button>
          <el-button type="danger" :loading="handling" @click="handleAppeal('reject')">驳回（填说明）</el-button>
        </div>
        <div v-else style="text-align:right;color:#b2bec3;margin-top:16px">该申诉已处理</div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tab = ref('report')

// ---------- 举报 ----------
const reports = ref([])
const reportTotal = ref(0)
const reportPage = ref(1)
const reportStatus = ref(1)
const reportLoading = ref(false)
const reportDialog = ref(false)
const reportDetail = ref(null)

// ---------- 申诉 ----------
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
const fmtTime = (s) => (s ? String(s).replace('T', ' ').slice(0, 19) : '')
const splitImages = (s) => (s ? s.split(',').map(x => x.trim()).filter(Boolean) : [])

const loadReports = async () => {
  reportLoading.value = true
  try {
    const res = await adminApi.getReports({ status: reportStatus.value, page: reportPage.value, size: 10 })
    reports.value = res.records; reportTotal.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally { reportLoading.value = false }
}

const loadAppeals = async () => {
  appealLoading.value = true
  try {
    const res = await adminApi.getAppeals({ status: appealStatus.value, page: appealPage.value, size: 10 })
    appeals.value = res.records; appealTotal.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally { appealLoading.value = false }
}

// ==================== 昵称审核 ====================
const nicknames = ref([])
const nicknameTotal = ref(0)
const nicknamePage = ref(1)
const nicknameStatus = ref(0)
const nicknameLoading = ref(false)
const nicknameStatusMap = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
const nicknameStatusText = (s) => nicknameStatusMap[s] || '未知'
const nicknameStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || '')

const loadNicknames = async () => {
  nicknameLoading.value = true
  try {
    const res = await adminApi.getNicknameAudits({ status: nicknameStatus.value, page: nicknamePage.value, size: 10 })
    nicknames.value = res.records; nicknameTotal.value = res.total
  } catch (e) { /* 拦截器已提示 */ } finally { nicknameLoading.value = false }
}

const approveNickname = async (row) => {
  try {
    await ElMessageBox.confirm(`通过「${row.newNickname}」的昵称申请？通过后立即生效。`, '昵称审核', { type: 'info' })
  } catch { return }
  try {
    await adminApi.handleNicknameAudit(row.id, { approve: true })
    ElMessage.success('已通过，昵称已生效')
    loadNicknames()
  } catch (e) { /* */ }
}

const rejectNickname = async (row) => {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(`拒绝「${row.newNickname}」的昵称申请，请填写原因（用户可见）。`, '昵称审核',
      { inputType: 'textarea', inputPlaceholder: '例如：昵称含广告信息', confirmButtonText: '确认拒绝', cancelButtonText: '取消' })
    reason = (value || '').trim()
  } catch { return }
  if (!reason) { ElMessage.warning('请填写拒绝原因'); return }
  try {
    await adminApi.handleNicknameAudit(row.id, { approve: false, reason })
    ElMessage.success('已拒绝')
    loadNicknames()
  } catch (e) { /* */ }
}

const openReport = async (id) => {
  try {
    reportDetail.value = await adminApi.getReportDetail(id)
    reportDialog.value = true
  } catch (e) { /* 拦截器已提示 */ }
}

const openAppeal = async (id) => {
  try {
    appealDetail.value = await adminApi.getAppealDetail(id)
    appealDialog.value = true
  } catch (e) { /* 拦截器已提示 */ }
}

const handleReport = async (action) => {
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
    await adminApi.handleReport(d.id, { action, reason })
    ElMessage.success(action === 'ban' ? '已违规下架并通知卖家' : '已驳回举报')
    reportDialog.value = false
    loadReports()
  } catch (e) { /* 拦截器已提示 */ } finally { handling.value = false }
}

const handleAppeal = async (action) => {
  const d = appealDetail.value
  let reply = ''
  if (action === 'approve') {
    try {
      await ElMessageBox.confirm(
        `确定通过「${d.productTitle}」的整改申诉并恢复上架吗？恢复后买家即可看到。`, '通过申诉',
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
    await adminApi.handleAppeal(d.id, { action, reason: reply })
    ElMessage.success(action === 'approve' ? '已通过并恢复上架' : '已驳回申诉')
    appealDialog.value = false
    loadAppeals()
  } catch (e) { /* 拦截器已提示 */ } finally { handling.value = false }
}

onMounted(() => { loadReports(); loadAppeals(); loadNicknames() })
</script>
