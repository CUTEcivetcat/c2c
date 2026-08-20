<template>
  <div>
    <!-- 确认下单（从商品页点「立即购买」进入，route.query.productId 存在） -->
    <div v-if="checkoutProduct" class="page-container order-detail fade-in-up" style="max-width:700px">
      <page-back-bar title="确认订单" />

      <div class="info-card">
        <div style="display:flex;gap:16px;align-items:flex-start">
          <img :src="checkoutProduct.images?.[0]?.url" class="checkout-img" alt="" />
          <div style="flex:1">
            <div class="checkout-title">{{ checkoutProduct.title }}</div>
            <div class="price-highlight" style="margin:8px 0">¥{{ checkoutProduct.price }}</div>
            <div style="font-size:13px;color:#b2bec3">{{ checkoutProduct.conditionText }}</div>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="section-title">选择收货地址</div>
        <div v-if="addresses.length">
          <div v-for="a in addresses" :key="a.id"
               class="address-option" :class="{ active: selectedAddressId === a.id }"
               @click="selectedAddressId = a.id">
            <div class="address-line">
              <strong>{{ a.receiverName }}</strong>&nbsp;<span>{{ a.phone }}</span>
              <el-tag v-if="a.isDefault === 1" type="success" size="small" style="margin-left:8px">默认</el-tag>
            </div>
            <div class="address-line sub">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
          </div>
        </div>
        <div v-else style="padding:8px 0;font-size:13px;color:#909399">
          还没有收货地址，<router-link to="/address" style="color:#ff6b35">去添加 →</router-link>
        </div>
      </div>

      <div class="action-bar">
        <button class="btn-primary" :disabled="creating" @click="submitOrder">{{ creating ? '提交中…' : '确认下单' }}</button>
      </div>
    </div>

    <!-- 订单详情 -->
    <div v-else-if="order" class="page-container order-detail fade-in-up" style="max-width:700px">
      <page-back-bar title="订单详情" />

      <!-- 步骤条 -->
      <div class="steps-card">
        <el-steps :active="activeStep" align-center finish-status="success">
          <el-step title="待支付" />
          <el-step title="已支付" />
          <el-step title="已发货" />
          <el-step title="已收货" />
          <el-step title="已完成" />
        </el-steps>
      </div>

      <!-- 订单信息 -->
      <div class="info-card">
        <div class="info-row"><span class="info-label">订单号</span><span class="info-value">{{ order.orderNo }}</span></div>
        <div class="info-row"><span class="info-label">商品</span><span class="info-value">{{ order.productTitle }}</span></div>
        <div class="info-row"><span class="info-label">金额</span><span class="info-value price-highlight">¥{{ order.totalAmount }}</span></div>
        <div class="info-row"><span class="info-label">状态</span><el-tag :class="statusClass" size="large" effect="light">{{ statusMap[order.status] }}</el-tag></div>
        <div class="info-row" v-if="order.shipCompany"><span class="info-label">快递</span><span class="info-value">{{ order.shipCompany }} · {{ order.shipNo }}</span></div>
      </div>

      <!-- 操作 -->
      <div class="action-bar">
        <button v-if="order.status===0" class="btn-primary" @click="doPay">确认支付 ¥{{ order.totalAmount }}</button>
        <button v-if="order.status===1 && isSeller" class="btn-primary" @click="showShip=true">去发货</button>
        <button v-if="order.status===2 && !isSeller" class="btn-success" @click="doReceive">确认收货</button>
        <button v-if="order.status===4 && !buyerRated" class="btn-outline" @click="$router.push(`/rating/${order.id}`)">去评价</button>
        <button v-if="order.status===0" class="btn-danger-text" @click="doCancel">取消订单</button>
      </div>

      <!-- 发货弹窗 -->
      <el-dialog v-model="showShip" title="填写发货信息" width="400px" center>
        <el-input v-model="shipCompany" placeholder="快递公司（如：顺丰速运）" size="large" style="margin-bottom:12px" />
        <el-input v-model="shipNo" placeholder="快递单号" size="large" />
        <template #footer><el-button type="primary" size="large" @click="doShip" style="width:100%;border-radius:12px">确认发货</el-button></template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getOrderDetail, createOrder, payOrder, shipOrder, receiveOrder, cancelOrder } from '@/api/order'
import { getProductDetail } from '@/api/product'
import { getAddresses } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

// ==================== 订单详情 ====================
const order = ref(null)
const showShip = ref(false)
const shipCompany = ref('')
const shipNo = ref('')
const statusMap = {0:'待支付',1:'已支付',2:'已发货',3:'已收货',4:'已完成',5:'已取消'}
const statusClass = computed(() => ({0:'status-pending',1:'status-paid',2:'status-shipped',3:'status-shipped',4:'status-completed',5:'status-cancelled'}[order.value?.status]))
const activeStep = computed(() => order.value?.status === 5 ? -1 : Math.min(order.value?.status || 0, 4))
const isSeller = computed(() => order.value?.sellerId === store.userInfo?.id)
const buyerRated = computed(() => !isSeller.value ? order.value?.buyerRated : order.value?.sellerRated)

// ==================== 确认下单 ====================
const checkoutProduct = ref(null)
const addresses = ref([])
const selectedAddressId = ref(null)
const creating = ref(false)

onMounted(async () => {
  if (route.query.productId) {
    await initCheckout()
  } else if (route.params.id) {
    order.value = await getOrderDetail(route.params.id)
  } else {
    ElMessage.error('参数错误')
  }
})

const initCheckout = async () => {
  try {
    checkoutProduct.value = await getProductDetail(route.query.productId)
    addresses.value = await getAddresses()
    const def = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
    if (def) selectedAddressId.value = def.id
  } catch (e) { /* 拦截器已提示 */ }
}

const submitOrder = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址'); return
  }
  creating.value = true
  try {
    const res = await createOrder({ productId: Number(route.query.productId), addressId: selectedAddressId.value })
    const orderId = res.orderId
    // 切到订单详情视图，并更新 URL，之后刷新页面也能直接打开
    checkoutProduct.value = null
    router.replace(`/order/${orderId}`)
    order.value = await getOrderDetail(orderId)
    ElMessage.success('下单成功，请尽快支付')
  } catch (e) { /* 拦截器已提示 */ } finally {
    creating.value = false
  }
}

const doPay = async () => { await payOrder(order.value.id); order.value.status = 1; ElMessage.success('支付成功') }
const doShip = async () => { await shipOrder(order.value.id, { shipCompany: shipCompany.value, shipNo: shipNo.value }); order.value.status = 2; showShip.value = false; ElMessage.success('发货成功') }
const doReceive = async () => { await receiveOrder(order.value.id); order.value.status = 4; ElMessage.success('已确认收货') }
const doCancel = async () => {
  try { await ElMessageBox.confirm('确定取消订单？', '提示', { confirmButtonText: '确定', cancelButtonText: '返回' }) }
  catch { return }
  await cancelOrder(order.value.id, '用户取消'); order.value.status = 5; ElMessage.info('订单已取消')
}
</script>

<style scoped>
.order-detail { animation: fadeInUp 0.4s ease-out; }
.steps-card { background: #fff; border-radius: 20px; padding: 28px 32px; border: 1px solid #f0f2f5; margin-bottom: 20px; }
.info-card { background: #fff; border-radius: 20px; padding: 24px; border: 1px solid #f0f2f5; margin-bottom: 20px; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f8f9fa; }
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 14px; color: #b2bec3; }
.info-value { font-size: 15px; font-weight: 600; color: #2d3436; }
.price-highlight { font-size: 20px; color: #e74c3c; }
.action-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.btn-primary {
  flex: 1; height: 48px; border: none; background: linear-gradient(135deg,#ff6b35,#ff8c5a);
  color: #fff; font-size: 15px; font-weight: 700; border-radius: 14px; cursor: pointer;
  transition: all 0.3s;
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255,107,53,0.3); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-success {
  flex: 1; height: 48px; border: none; background: #00b894; color: #fff;
  font-size: 15px; font-weight: 700; border-radius: 14px; cursor: pointer;
}
.btn-outline {
  flex: 1; height: 48px; border: 2px solid #ff6b35; background: #fff;
  color: #ff6b35; font-size: 15px; font-weight: 700; border-radius: 14px; cursor: pointer;
}
.btn-danger-text {
  padding: 12px 20px; border: none; background: none; color: #e74c3c;
  font-size: 14px; cursor: pointer; font-weight: 500;
}
/* 确认下单页 */
.checkout-img { width: 96px; height: 96px; object-fit: cover; border-radius: 12px; background: #f8f9fa }
.checkout-title { font-size: 15px; font-weight: 600; color: #2d3436 }
.section-title { font-size: 15px; font-weight: 600; color: #2d3436; margin-bottom: 12px }
.address-option { border: 1px solid #eef0f3; border-radius: 12px; padding: 12px 14px; margin-bottom: 10px; cursor: pointer; transition: all 0.2s }
.address-option:hover { border-color: #ff8c5a }
.address-option.active { border-color: #ff6b35; background: #fff7f3 }
.address-line { font-size: 14px; color: #2d3436 }
.address-line.sub { font-size: 13px; color: #909399; margin-top: 4px }
@media (max-width: 480px) { .steps-card { padding: 16px; } .info-card { padding: 14px; } .btn-primary, .btn-success, .btn-outline { height: 42px; font-size: 14px; } }
</style>
