<template>
  <div class="page-container" style="max-width:880px">
    <page-back-bar title="我的钱包" />
    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-icon">💰</div>
      <div class="balance-label">账户余额（元）</div>
      <div class="balance-num">{{ balance }}</div>
      <div class="balance-actions">
        <el-button type="primary" round @click="openRecharge('card')" :loading="recharging">充值</el-button>
        <el-button round class="ghost-btn" @click="openWithdraw">提现</el-button>
        <el-button round class="back-btn" @click="$router.push('/profile')">返回个人中心</el-button>
      </div>
    </div>

    <!-- 演示银行卡 -->
    <div class="section-head">
      <h3>演示银行卡</h3>
      <el-button size="small" plain @click="showCardForm = !showCardForm">{{ showCardForm ? '收起' : '添加银行卡' }}</el-button>
    </div>

    <el-card v-if="showCardForm" class="card-form" shadow="never">
      <el-form :model="cardForm" label-position="top">
        <el-form-item label="持卡人姓名">
          <el-input v-model="cardForm.cardHolder" placeholder="请输入持卡人姓名" />
        </el-form-item>
        <el-form-item label="银行卡号（8-30 位数字）">
          <el-input v-model="cardForm.cardNo" placeholder="演示卡号，未接入真实支付" />
        </el-form-item>
        <el-form-item label="开户行（可空）">
          <el-input v-model="cardForm.bankName" placeholder="如：工商银行" />
        </el-form-item>
        <el-button type="primary" :loading="savingCard" @click="submitCard">绑定银行卡</el-button>
      </el-form>
    </el-card>

    <div v-if="cards.length" class="card-list">
      <div v-for="c in cards" :key="c.id" class="bank-card" :class="{ default: c.isDefault === 1 }">
        <div class="bank-left">
          <div class="bank-name">{{ c.bankName || '演示银行' }} <el-tag v-if="c.isDefault === 1" type="success" size="small">默认</el-tag></div>
          <div class="bank-no">**** **** **** {{ last4(c.cardNo) }}</div>
          <div class="bank-holder">{{ c.cardHolder }}</div>
        </div>
        <div class="bank-right">
          <el-button v-if="c.isDefault !== 1" size="small" plain @click="pickDefault(c.id)">设为默认</el-button>
          <el-button size="small" plain type="danger" @click="removeCard(c)">解绑</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!showCardForm" description="还未绑定演示银行卡" :image-size="70" />

    <!-- 充值弹窗 -->
    <el-dialog v-model="showRecharge" title="充值" width="380px" @closed="onRechargeClosed">
      <el-tabs v-model="rechargeTab" @tab-change="onRechargeTab">
        <el-tab-pane label="银行卡充值" name="card" />
        <el-tab-pane label="扫码充值" name="qr" />
      </el-tabs>

      <!-- 银行卡充值 -->
      <div v-if="rechargeTab === 'card'">
        <el-alert v-if="!cards.length" type="warning" :closable="false" show-icon
          title="尚未绑定演示银行卡" description="请先在下方银行卡区绑定一张演示卡，再进行充值" />
        <el-form v-else style="margin-top:12px">
          <el-form-item label="选择银行卡">
            <el-radio-group v-model="rechargeCardId">
              <el-radio v-for="c in cards" :key="c.id" :value="c.id" class="card-radio">
                {{ c.bankName || '演示银行' }} 尾号 {{ last4(c.cardNo) }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="充值金额">
            <el-input-number v-model="rechargeAmount" :min="1" :max="99999" :step="10" style="width:100%" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 扫码充值 -->
      <div v-else class="qr-box">
        <div v-if="qrDataUrl" class="qr-img">
          <img :src="qrDataUrl" alt="充值二维码" />
        </div>
        <div v-else class="qr-loading">正在生成二维码…</div>
        <div class="qr-tip">请使用手机扫码，在 H5 页面填写金额完成充值</div>
        <div class="qr-link">链接：{{ qrLink }}</div>
        <el-button size="small" plain @click="genTicket">重新生成二维码（5 分钟内有效）</el-button>
      </div>

      <template #footer>
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button v-if="rechargeTab === 'card'" type="primary" :loading="recharging" :disabled="!cards.length" @click="doRechargeCard">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 提现弹窗 -->
    <el-dialog v-model="showWithdraw" title="提现到演示银行卡" width="380px">
      <el-alert v-if="!cards.length" type="warning" :closable="false" show-icon
        title="尚未绑定演示银行卡" description="请先绑定演示银行卡，再进行提现" />
      <el-form v-else style="margin-top:12px">
        <el-form-item label="到账银行卡">
          <el-radio-group v-model="withdrawCardId">
            <el-radio v-for="c in cards" :key="c.id" :value="c.id" class="card-radio">
              {{ c.bankName || '演示银行' }} 尾号 {{ last4(c.cardNo) }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提现金额">
          <el-input-number v-model="withdrawAmount" :min="1" :max="99999" :step="10" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWithdraw = false">取消</el-button>
        <el-button type="primary" :loading="withdrawing" :disabled="!cards.length" @click="doWithdraw">确认提现</el-button>
      </template>
    </el-dialog>

    <!-- 流水列表 -->
    <h3 class="log-title">交易流水</h3>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import {
  getWalletProfile, rechargeByCard, createRechargeTicket, withdraw,
  listCards, addCard, removeCard as removeCardApi, setDefaultCard
} from '@/api/wallet'

const balance = ref('0.00')
const logs = ref([])
const cards = ref([])

// 充值
const showRecharge = ref(false)
const rechargeTab = ref('card')
const rechargeAmount = ref(100)
const rechargeCardId = ref(null)
const recharging = ref(false)
const qrTicket = ref('')
const qrDataUrl = ref('')
const qrLink = ref('')

// 提现
const showWithdraw = ref(false)
const withdrawAmount = ref(100)
const withdrawCardId = ref(null)
const withdrawing = ref(false)

// 绑卡
const showCardForm = ref(false)
const savingCard = ref(false)
const cardForm = ref({ cardHolder: '', cardNo: '', bankName: '' })

const typeText = (t) => ({
  recharge: '充值', pay: '支付', refund: '退款', receive: '收款',
  withdraw: '提现', bonus: '余额券到账'
}[t] || t)
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 19) : ''
const last4 = (no) => no && String(no).length >= 4 ? String(no).slice(-4) : (no || '****')

const load = async () => {
  try {
    const res = await getWalletProfile()
    balance.value = res.balance
    logs.value = res.logs || []
  } catch (e) { /* 拦截器已提示 */ }
  loadCards()
}

const loadCards = async () => {
  try {
    cards.value = await listCards() || []
    const def = cards.value.find(c => c.isDefault === 1) || cards.value[0]
    rechargeCardId.value = def ? def.id : null
    withdrawCardId.value = def ? def.id : null
  } catch (e) { /* */ }
}

const openRecharge = (tab) => {
  rechargeTab.value = tab
  showRecharge.value = true
  if (tab === 'qr') genTicket()
}

const onRechargeTab = (name) => {
  if (name === 'qr' && !qrTicket.value) genTicket()
}

const openWithdraw = () => {
  showWithdraw.value = true
}

const genTicket = async () => {
  qrDataUrl.value = ''
  try {
    const res = await createRechargeTicket()
    qrTicket.value = res.ticket
    qrLink.value = `${location.origin}/wallet/recharge?ticket=${res.ticket}`
    qrDataUrl.value = await QRCode.toDataURL(qrLink.value, { width: 200, margin: 1 })
  } catch (e) { /* */ }
}

const doRechargeCard = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) { ElMessage.warning('请输入充值金额'); return }
  recharging.value = true
  try {
    await rechargeByCard({ amount: rechargeAmount.value, cardId: rechargeCardId.value })
    ElMessage.success('充值成功')
    showRecharge.value = false
    load()
  } catch (e) { /* */ } finally { recharging.value = false }
}

const doWithdraw = async () => {
  if (!withdrawAmount.value || withdrawAmount.value <= 0) { ElMessage.warning('请输入提现金额'); return }
  withdrawing.value = true
  try {
    await withdraw({ amount: withdrawAmount.value, cardId: withdrawCardId.value })
    ElMessage.success('提现成功')
    showWithdraw.value = false
    load()
  } catch (e) { /* */ } finally { withdrawing.value = false }
}

const submitCard = async () => {
  const f = cardForm.value
  if (!f.cardHolder.trim()) { ElMessage.warning('请输入持卡人姓名'); return }
  if (!/^\d{8,30}$/.test(f.cardNo.trim())) { ElMessage.warning('卡号须为 8-30 位数字'); return }
  savingCard.value = true
  try {
    await addCard({ cardHolder: f.cardHolder.trim(), cardNo: f.cardNo.trim(), bankName: (f.bankName || '').trim() })
    ElMessage.success('绑定成功')
    showCardForm.value = false
    cardForm.value = { cardHolder: '', cardNo: '', bankName: '' }
    loadCards()
  } catch (e) { /* */ } finally { savingCard.value = false }
}

const removeCard = async (card) => {
  try {
    await ElMessageBox.confirm(
      `确定要解绑尾号 ${last4(card.cardNo)} 的银行卡吗？解绑后需重新绑定才能使用。`,
      '解绑银行卡',
      { type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消' }
    )
  } catch (e) {
    return // 用户取消
  }
  try {
    await removeCardApi(card.id)
    ElMessage.success('已解绑')
    loadCards()
  } catch (e) { /* */ }
}

const pickDefault = async (id) => {
  try {
    await setDefaultCard(id)
    ElMessage.success('已设为默认卡')
    loadCards()
  } catch (e) { /* */ }
}

const onRechargeClosed = () => {
  qrTicket.value = ''
  qrDataUrl.value = ''
  qrLink.value = ''
}

onMounted(load)
</script>

<style scoped>
.balance-card { background: linear-gradient(135deg, var(--c2c-primary), var(--c2c-primary-light)); border-radius: 20px; padding: 32px; text-align: center; color: #fff; }
.balance-icon { font-size: 40px; margin-bottom: 4px; }
.balance-label { font-size: 13px; opacity: 0.85; }
.balance-num { font-size: 42px; font-weight: 800; margin: 8px 0 16px; font-variant-numeric: tabular-nums; }
.balance-actions { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
.balance-actions .el-button--primary { background: #fff; border-color: transparent; color: var(--c2c-primary); font-weight: 700; }
.balance-actions .el-button--primary:hover { background: #fff5f0; }
.ghost-btn { background: rgba(255,255,255,0.18); border-color: rgba(255,255,255,0.6); color: #fff; }
.ghost-btn:hover { background: rgba(255,255,255,0.3); color: #fff; }
.back-btn { background: rgba(255,255,255,0.18); border-color: rgba(255,255,255,0.6); color: #fff; }
.back-btn:hover { background: rgba(255,255,255,0.3); color: #fff; }

.section-head { display: flex; justify-content: space-between; align-items: center; margin: 22px 0 12px; }
.section-head h3 { font-size: 16px; color: var(--c2c-text); }
.card-form { margin-bottom: 12px; }
.card-list { display: flex; flex-direction: column; gap: 10px; }
.bank-card { display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid var(--c2c-line-soft); border-radius: 14px; padding: 16px; }
.bank-card.default { border-color: var(--c2c-primary); }
.bank-name { font-size: 15px; font-weight: 600; color: var(--c2c-text); }
.bank-no { font-size: 14px; letter-spacing: 2px; margin: 4px 0; color: var(--c2c-text-secondary); }
.bank-holder { font-size: 12px; color: var(--c2c-text-muted); }
.bank-right { display: flex; gap: 8px; }
.card-radio { display: block; margin-bottom: 8px; }

.qr-box { text-align: center; padding: 8px 0; }
.qr-img img { width: 200px; height: 200px; border: 1px solid var(--c2c-line-soft); border-radius: 12px; }
.qr-loading { height: 200px; display: flex; align-items: center; justify-content: center; color: var(--c2c-text-muted); }
.qr-tip { font-size: 13px; color: var(--c2c-text-secondary); margin: 12px 0 6px; }
.qr-link { font-size: 11px; color: var(--c2c-text-muted); word-break: break-all; margin-bottom: 10px; }

.log-title { margin: 22px 0 12px; font-size: 16px; color: var(--c2c-text); }
.log-card { border-radius: 14px; margin-bottom: 8px; }
.log-row { display: flex; justify-content: space-between; align-items: center; }
.log-left { display: flex; align-items: center; gap: 8px; }
.log-type { font-size: 11px; font-weight: 700; padding: 1px 8px; border-radius: 6px; }
.log-type.recharge { background: #e8f5e9; color: #2e7d32; }
.log-type.pay { background: #fff3e0; color: #e65100; }
.log-type.refund { background: #e3f2fd; color: #1565c0; }
.log-type.receive { background: #fce4ec; color: #c62828; }
.log-type.withdraw { background: #efebe9; color: #5d4037; }
.log-type.bonus { background: #e8f5e9; color: #2e7d32; }
.log-remark { font-size: 13px; color: var(--c2c-text); }
.log-amount { font-size: 16px; font-weight: 700; }
.log-amount.income { color: #2e7d32; }
.log-amount.expense { color: #c62828; }
.log-meta { display: flex; gap: 16px; margin-top: 4px; font-size: 11px; color: var(--c2c-text-muted); }
</style>
