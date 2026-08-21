<template>
  <div class="page-container" style="max-width:600px">
    <page-back-bar title="我的钱包" />
    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-icon">💰</div>
      <div class="balance-label">账户余额（元）</div>
      <div class="balance-num">{{ balance }}</div>
      <div class="balance-actions">
        <el-button type="primary" round @click="showRecharge = true" :loading="recharging">充值</el-button>
        <el-button round @click="$router.push('/profile')">返回个人中心</el-button>
      </div>
    </div>
    <!-- 充值弹窗 -->
    <el-dialog v-model="showRecharge" title="模拟充值" width="360px">
      <el-form>
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="1" :max="99999" :step="10" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>
    <!-- 流水列表 -->
    <h3 style="margin: 20px 0 12px; font-size: 16px; color: #2d3436">交易流水</h3>
    <el-card v-for="log in logs" :key="log.id" class="log-card" shadow="hover">
      <div class="log-row">
        <div class="log-left">
          <span class="log-type" :class="log.type">{{ typeText(log.type) }}</span>
          <span class="log-remark">{{ log.remark }}</span>
        </div>
        <div class="log-right">
          <span class="log-amount" :class="{ income: log.amount > 0, expense: log.amount < 0 }">
            {{ log.amount > 0 ? '+' : '' }}{{ log.amount }}
          </span>
        </div>
      </div>
      <div class="log-meta">
        <span>{{ fmtTime(log.createdAt) }}</span>
        <span>余额：{{ log.balanceBefore }} → {{ log.balanceAfter }}</span>
      </div>
    </el-card>
    <el-empty v-if="!logs.length" description="暂无交易记录" />
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { } from '@/api/user'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const balance = ref('0.00')
const logs = ref([])
const showRecharge = ref(false)
const rechargeAmount = ref(100)
const recharging = ref(false)

const typeText = (t) => ({ recharge: '充值', pay: '支付', refund: '退款', receive: '收款' }[t] || t)
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''

const load = async () => {
  try {
    const res = await request.get('/user/wallet/profile')
    balance.value = res.balance
    logs.value = res.logs || []
  } catch (e) { /* */ }
}

const doRecharge = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) { ElMessage.warning('请输入充值金额'); return }
  recharging.value = true
  try {
    await request.post('/user/wallet/recharge', { amount: rechargeAmount.value })
    ElMessage.success('充值成功')
    showRecharge.value = false
    load()
  } catch (e) { /* */ } finally { recharging.value = false }
}

onMounted(load)
</script>
<style scoped>
.balance-card { background: linear-gradient(135deg, #ff6b35, #ff8c5a); border-radius: 20px; padding: 32px; text-align: center; color: #fff; }
.balance-icon { font-size: 40px; margin-bottom: 4px; }
.balance-label { font-size: 13px; opacity: 0.85; }
.balance-num { font-size: 42px; font-weight: 800; margin: 8px 0 16px; font-variant-numeric: tabular-nums; }
.balance-actions { display: flex; gap: 12px; justify-content: center; }
.balance-actions .el-button { color: #fff; border-color: rgba(255,255,255,0.5); }
.balance-actions .el-button--primary { background: rgba(255,255,255,0.2); border-color: transparent; }
.log-card { border-radius: 14px; margin-bottom: 8px; }
.log-row { display: flex; justify-content: space-between; align-items: center; }
.log-left { display: flex; align-items: center; gap: 8px; }
.log-type { font-size: 11px; font-weight: 700; padding: 1px 8px; border-radius: 6px; }
.log-type.recharge { background: #e8f5e9; color: #2e7d32; }
.log-type.pay { background: #fff3e0; color: #e65100; }
.log-type.refund { background: #e3f2fd; color: #1565c0; }
.log-type.receive { background: #fce4ec; color: #c62828; }
.log-remark { font-size: 13px; color: #2d3436; }
.log-amount { font-size: 16px; font-weight: 700; }
.log-amount.income { color: #2e7d32; }
.log-amount.expense { color: #c62828; }
.log-meta { display: flex; gap: 16px; margin-top: 4px; font-size: 11px; color: #b2bec3; }
</style>