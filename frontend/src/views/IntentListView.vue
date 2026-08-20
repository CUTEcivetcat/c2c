<template>
  <div class="page-container">
    <page-back-bar title="我的意向" />
    <el-tabs v-model="tab" style="margin-top:8px" @tab-change="load">
      <el-tab-pane :label="`我想要的（${myTotal}）`" name="buyer">
        <div v-if="myList.length" class="intent-list">
          <div v-for="it in myList" :key="it.id" class="intent-card" @click="goProduct(it)">
            <product-cover :src="it.productCover" class="intent-img" />
            <div class="intent-main">
              <div class="intent-title">{{ it.productTitle || ('商品 #' + it.productId) }}</div>
              <div class="intent-meta">
                <span>标价 ¥{{ it.productPrice ?? '--' }}</span>
                <el-tag :type="statusType(it.status)" size="small">{{ it.statusText }}</el-tag>
              </div>
              <div class="intent-msg" v-if="it.message">留言：{{ it.message }}</div>
              <div class="intent-msg" v-if="it.expectedPrice != null">期望价：¥{{ it.expectedPrice }}</div>
              <div class="intent-reply" v-if="it.sellerReply">卖家回复：{{ it.sellerReply }}</div>
              <div class="intent-time">{{ fmtTime(it.createdAt) }}</div>
            </div>
            <div class="intent-actions" @click.stop>
              <el-button v-if="it.status === 1 || it.status === 2" size="small" plain type="info" @click="close(it)">关闭意向</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="还没有发出过意向，去逛逛吧" />
      </el-tab-pane>

      <el-tab-pane :label="`收到的意向（${sellerTotal}）`" name="seller">
        <div v-if="sellerList.length" class="intent-list">
          <div v-for="it in sellerList" :key="it.id" class="intent-card" @click="goProduct(it)">
            <product-cover :src="it.productCover" class="intent-img" />
            <div class="intent-main">
              <div class="intent-title">{{ it.productTitle || ('商品 #' + it.productId) }}</div>
              <div class="intent-meta">
                <span>买家：{{ it.buyerNickname || ('用户' + it.buyerId) }}</span>
                <el-tag :type="statusType(it.status)" size="small">{{ it.statusText }}</el-tag>
              </div>
              <div class="intent-msg" v-if="it.message">留言：{{ it.message }}</div>
              <div class="intent-msg" v-if="it.expectedPrice != null">期望价：¥{{ it.expectedPrice }}</div>
              <div class="intent-reply" v-if="it.sellerReply">我的回复：{{ it.sellerReply }}</div>
              <div class="intent-time">{{ fmtTime(it.createdAt) }}</div>
            </div>
            <div class="intent-actions" @click.stop v-if="it.status === 1 || it.status === 2">
              <el-button size="small" type="primary" plain @click="openReply(it)">回复</el-button>
              <el-button size="small" type="success" plain @click="deal(it)">成交</el-button>
              <el-button size="small" plain type="info" @click="close(it)">关闭</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="还没有收到任何意向" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="replyDialog.visible" title="回复买家" width="420px">
      <el-input v-model="replyDialog.reply" type="textarea" :rows="4" maxlength="500" placeholder="回复买家，例如：可以小刀 10 元，支持面交" />
      <template #footer>
        <el-button @click="replyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyIntents, getSellerIntents, replyIntent, dealIntent, closeIntent } from '@/api/product'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const tab = ref('buyer')
const myList = ref([])
const myTotal = ref(0)
const sellerList = ref([])
const sellerTotal = ref(0)
const loading = ref(false)
const submitting = ref(false)
const replyDialog = ref({ visible: false, id: null, reply: '' })

const load = async () => {
  loading.value = true
  try {
    if (tab.value === 'buyer') {
      const r = await getMyIntents({ page: 1, size: 50 })
      myList.value = r.records || []
      myTotal.value = r.total || 0
    } else {
      const r = await getSellerIntents({ page: 1, size: 50 })
      sellerList.value = r.records || []
      sellerTotal.value = r.total || 0
    }
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

onMounted(load)

const goProduct = (it) => router.push(`/product/${it.productId}`)
const statusType = (s) => ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }[s] || 'info')
const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : ''

const openReply = (it) => { replyDialog.value = { visible: true, id: it.id, reply: it.sellerReply || '' } }
const submitReply = async () => {
  if (!replyDialog.value.reply.trim()) return ElMessage.warning('请输入回复内容')
  submitting.value = true
  try {
    await replyIntent(replyDialog.value.id, replyDialog.value.reply.trim())
    ElMessage.success('已回复')
    replyDialog.value.visible = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}
const deal = async (it) => {
  try {
    await ElMessageBox.confirm('确认与该买家成交？成交后意向将关闭。', '确认成交', { type: 'warning', confirmButtonText: '确认成交' })
  } catch (e) { return }
  try {
    await dealIntent(it.id)
    ElMessage.success('已成交')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}
const close = async (it) => {
  try {
    await ElMessageBox.confirm('确定关闭该意向吗？', '关闭意向', { type: 'info', confirmButtonText: '关闭' })
  } catch (e) { return }
  try {
    await closeIntent(it.id)
    ElMessage.success('已关闭')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}
</script>

<style scoped>
.intent-list { display: flex; flex-direction: column; gap: 14px; }
.intent-card {
  display: flex; gap: 14px; background: #fff; border: 1px solid #f0f2f5;
  border-radius: 16px; padding: 16px; cursor: pointer; transition: all 0.2s;
}
.intent-card:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.06); transform: translateY(-1px); }
.intent-img { width: 88px; height: 88px; border-radius: 12px; object-fit: cover; flex-shrink: 0; background: #f8f9fa; }
.intent-main { flex: 1; min-width: 0; }
.intent-title { font-size: 15px; font-weight: 600; color: #2d3436; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.intent-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.intent-meta span { font-size: 13px; color: #636e72; }
.intent-msg { font-size: 13px; color: #636e72; margin-top: 2px; word-break: break-word; }
.intent-reply { font-size: 13px; color: #ff6b35; margin-top: 4px; word-break: break-word; }
.intent-time { font-size: 12px; color: #b2bec3; margin-top: 6px; }
.intent-actions { display: flex; flex-direction: column; gap: 8px; justify-content: center; flex-shrink: 0; }
@media (max-width: 480px) {
  .intent-img { width: 68px; height: 68px; }
  .intent-actions { flex-direction: row; flex-wrap: wrap; }
}
</style>
