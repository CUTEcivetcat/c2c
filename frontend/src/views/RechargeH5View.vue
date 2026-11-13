<template>
  <div class="h5-recharge">
    <div class="h5-card">
      <div class="h5-title">扫码充值</div>
      <div class="h5-sub">青禾集市 · 演示充值（未接入真实支付）</div>

      <template v-if="!ticket">
        <el-alert type="warning" :closable="false" show-icon title="充值链接无效"
          description="未获取到充值票据，请返回电脑端重新扫码" />
      </template>

      <template v-else>
        <el-form @submit.prevent>
          <el-form-item label="充值金额（元）">
            <el-input-number v-model="amount" :min="1" :max="99999" :step="10" controls-position="right" style="width:100%" />
          </el-form-item>
        </el-form>
        <el-button type="primary" class="h5-submit" :loading="submitting" @click="submit">确认充值</el-button>
        <div class="h5-tip">票据 5 分钟内有效，一次性使用；充值即累加至账户余额</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { rechargeByTicket } from '@/api/wallet'

const route = useRoute()
const ticket = computed(() => String(route.query.ticket || ''))
const amount = ref(100)
const submitting = ref(false)

const submit = async () => {
  if (!amount.value || amount.value <= 0) { ElMessage.warning('请输入充值金额'); return }
  submitting.value = true
  try {
    await rechargeByTicket({ ticket: ticket.value, amount: amount.value })
    ElMessage.success('充值成功，余额已更新')
    amount.value = 100
  } catch (e) { /* 拦截器已提示（含票据失效） */ } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.h5-recharge { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--c2c-bg); padding: 20px; }
.h5-card { width: 100%; max-width: 380px; background: #fff; border-radius: 20px; padding: 28px 22px; box-shadow: 0 8px 28px rgba(0,0,0,0.08); }
.h5-title { font-size: 20px; font-weight: 800; color: var(--c2c-text); text-align: center; }
.h5-sub { font-size: 12px; color: var(--c2c-text-muted); text-align: center; margin: 6px 0 20px; }
.h5-submit { width: 100%; margin-top: 8px; }
.h5-tip { font-size: 11px; color: var(--c2c-text-muted); text-align: center; margin-top: 12px; line-height: 1.6; }
</style>
