<template>
  <div class="page-container" v-if="product">
    <page-back-bar title="商品详情" />
    <div class="detail-grid">
      <!-- 左侧图片 -->
      <div class="detail-gallery">
        <div class="main-image-wrap">
          <product-cover :src="activeImage" class="main-image" fit="contain" />
          <span class="img-badge" v-if="product.conditionText">{{ product.conditionText }}</span>
        </div>
        <div class="thumb-list" v-if="product.images?.length > 1">
          <div v-for="(img, i) in product.images" :key="i"
            :class="['thumb', { active: activeImage === img.url }]"
            @click="activeImage = img.url">
            <product-cover :src="img.url" />
          </div>
        </div>
      </div>

      <!-- 右侧信息 -->
      <div class="detail-info">
        <h1 class="detail-title">{{ product.title }}</h1>
        <div class="detail-price-row">
          <span class="detail-price">{{ product.price }}</span>
          <span class="detail-unit">元</span>
          <span class="detail-original" v-if="product.originalPrice && product.originalPrice > product.price">
            原价 ¥{{ product.originalPrice }}
          </span>
          <span class="discount-tag" v-if="product.originalPrice && product.originalPrice > product.price">
            {{ Math.round((1 - product.price / product.originalPrice) * 100) }}% OFF
          </span>
        </div>

        <!-- 属性 -->
        <div class="detail-attrs">
          <div class="attr-item">
            <span class="attr-label">成色</span>
            <span class="attr-val">{{ product.conditionText }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">运费</span>
            <span class="attr-val" :style="{color: product.freightType === 1 ? '#00b894' : '#636e72'}">
              {{ product.freightText }}
              <span v-if="product.freightAmount > 0">(¥{{ product.freightAmount }})</span>
            </span>
          </div>
          <div class="attr-item" v-if="product.location">
            <span class="attr-label">所在地</span>
            <span class="attr-val">{{ product.location }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">浏览</span>
            <span class="attr-val">{{ product.viewCount }} 次</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions" v-if="product.sellerId !== store.userInfo?.id">
          <button class="action-buy" @click="buyNow">立即购买</button>
          <button class="action-chat" @click="contactSeller">
            <el-icon><ChatDotRound /></el-icon> 联系卖家
          </button>
          <button class="action-chat" @click="showIntentDialog = true">
            <el-icon><Pointer /></el-icon> 我想要
          </button>
          <button class="action-fav" :class="{ favorited: isFav }" @click="toggleFav">
            <el-icon :size="20"><StarFilled v-if="isFav"/><Star v-else/></el-icon>
          </button>
        </div>
        <div class="detail-actions" v-else>
          <el-tag type="warning" size="large">这是你发布的商品</el-tag>
          <router-link :to="`/publish?edit=${product.id}`" style="margin-left:12px">
            <el-button size="large">编辑商品</el-button>
          </router-link>
        </div>

        <!-- 举报入口（非本人商品） -->
        <div class="report-entry" v-if="product.sellerId !== store.userInfo?.id">
          <el-button link type="danger" size="small" @click="showReportDialog = true">⚑ 举报该商品</el-button>
        </div>

        <!-- 违规下架整改卡片（本人商品） -->
        <div class="banned-card" v-if="product.sellerId === store.userInfo?.id && product.status === 5">
          <div class="banned-tip">
            <strong>⚠️ 您的商品已被违规下架</strong>
            <p v-if="product.reviewReason" class="banned-reason">原因：{{ product.reviewReason }}</p>
            <p>请按原因整改后提交申诉，审核通过后重新上架。</p>
          </div>
          <el-button type="danger" size="small" @click="showAppealDialog = true">整改申诉</el-button>
        </div>

        <!-- 卖家卡片 -->
        <div class="seller-card">
          <router-link :to="`/user/${product.sellerId}`" class="seller-link">
            <el-avatar :size="44" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:18px">
              {{ product.sellerName?.charAt(0) || '?' }}
            </el-avatar>
            <div class="seller-info">
              <strong>{{ product.sellerName || '匿名用户' }}</strong>
              <div class="seller-rep">
                <el-rate :model-value="Number(product.sellerReputation || 5)" disabled size="small" />
                <span>{{ product.sellerReputation || '5.0' }}</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 描述 -->
    <div class="detail-section">
      <h2 class="section-title">商品描述</h2>
      <div class="desc-content" v-if="product.description">
        <p>{{ product.description }}</p>
      </div>
      <div class="desc-content empty" v-else>
        <p>卖家很懒，什么都没写…</p>
      </div>
    </div>

    <!-- 评论 -->
    <div class="detail-section">
      <h2 class="section-title">商品评价（{{ commentTotal }}）</h2>
      <div class="comment-box">
        <div class="comment-input" v-if="store.isLoggedIn()">
          <el-input v-model="commentText" type="textarea" :rows="2" maxlength="500" placeholder="说说你的看法…" />
          <div class="comment-input-actions">
            <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
          </div>
        </div>
        <div v-else class="comment-login-tip">
          <router-link to="/login">登录后参与评论</router-link>
        </div>

        <div class="comment-list" v-if="comments.length">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <el-avatar :size="36" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);flex-shrink:0">
              {{ c.nickname?.charAt(0) || '?' }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <strong>{{ c.nickname || '匿名用户' }}</strong>
                <span class="comment-time">{{ fmtTime(c.createdAt) }}</span>
                <div class="comment-ops">
                  <el-button link type="primary" size="small" @click="replyTo = c.id">回复</el-button>
                  <el-button v-if="store.userInfo?.id === c.userId" link type="danger" size="small" @click="removeComment(c.id)">删除</el-button>
                </div>
              </div>
              <div class="comment-content">{{ c.content }}</div>
              <div v-if="replyTo === c.id" class="comment-reply">
                <el-input v-model="replyText" type="textarea" :rows="2" maxlength="500" :placeholder="`回复 ${c.nickname || '匿名用户'}…`" />
                <div class="comment-reply-actions">
                  <el-button size="small" @click="replyTo = null">取消</el-button>
                  <el-button size="small" type="primary" :loading="commentSubmitting" @click="submitReply(c.id)">回复</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="!loadingComments" class="comment-empty">还没有评论，来说两句吧</div>
      </div>
    </div>

    <!-- 猜你喜欢（同分类在售商品） -->
    <div class="detail-section" v-if="similar.length">
      <h2 class="section-title">猜你喜欢</h2>
      <div class="similar-grid">
        <div v-for="p in similar" :key="p.id" class="similar-card" @click="$router.push(`/product/${p.id}`)">
          <product-cover :src="p.images?.[0]?.url" class="similar-img" />
          <div class="similar-info">
            <div class="similar-title">{{ p.title }}</div>
            <div class="similar-price">¥{{ p.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 我想要弹窗 -->
    <el-dialog v-model="showIntentDialog" title="我想买" width="420px">
      <p style="font-size:14px;color:#636e72;margin:0 0 16px">对「<strong>{{ product.title }}</strong>」表达购买意向，卖家会收到并回复你。</p>
      <el-form label-position="top">
        <el-form-item label="期望价格（元，可选）">
          <el-input-number v-model="intentForm.expectedPrice" :min="0" :precision="2" :controls="false" placeholder="留空表示按标价" style="width:100%" />
        </el-form-item>
        <el-form-item label="想说的话（可选）">
          <el-input v-model="intentForm.message" type="textarea" :rows="3" maxlength="500" placeholder="例如：可以小刀吗？什么时候能发货？" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIntentDialog = false">取消</el-button>
        <el-button type="primary" :loading="intentSubmitting" @click="submitIntent">发送意向</el-button>
      </template>
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="showReportDialog" title="举报该商品" width="440px">
      <p style="font-size:13px;color:#b2bec3;margin:0 0 16px">举报后由审核员/管理员审核处理，我们会保护举报人信息。</p>
      <el-form label-position="top">
        <el-form-item label="举报类型">
          <el-select v-model="reportForm.reportType" style="width:100%">
            <el-option :value="1" label="违禁品" />
            <el-option :value="2" label="假冒伪劣" />
            <el-option :value="3" label="描述不符" />
            <el-option :value="4" label="欺诈" />
            <el-option :value="5" label="侵权" />
            <el-option :value="6" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报理由">
          <el-input v-model="reportForm.reason" type="textarea" :rows="4" maxlength="500" placeholder="请具体说明违规情况，便于审核…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="danger" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <!-- 整改申诉弹窗 -->
    <el-dialog v-model="showAppealDialog" title="整改申诉" width="440px">
      <p style="font-size:13px;color:#b2bec3;margin:0 0 16px">说明你的整改情况，审核通过后商品恢复上架。同一商品最多申诉 3 次。</p>
      <el-form label-position="top">
        <el-form-item label="整改说明">
          <el-input v-model="appealForm.appealReason" type="textarea" :rows="4" maxlength="1000" placeholder="说明整改措施，例如：已更换违规图片 / 已删除夸大描述…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAppealDialog = false">取消</el-button>
        <el-button type="primary" :loading="appealSubmitting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { getProductDetail, getComments, addComment, deleteComment, createIntent, searchProducts } from '@/api/product'
import { recordBrowse } from '@/utils/browseHistory'
import { addFavorite, removeFavorite, checkFavorite } from '@/api/favorite'
import { getOrCreateConversation } from '@/api/im'
import { createReport, createAppeal } from '@/api/review'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const product = ref(null)
const activeImage = ref('')
const isFav = ref(false)

// 评论
const comments = ref([])
const commentTotal = ref(0)
const loadingComments = ref(false)
const commentSubmitting = ref(false)
const commentText = ref('')
const replyTo = ref(null)
const replyText = ref('')

// 我想要
const showIntentDialog = ref(false)
const intentSubmitting = ref(false)
const intentForm = ref({ message: '', expectedPrice: null })

// 举报
const showReportDialog = ref(false)
const reportSubmitting = ref(false)
const reportForm = ref({ reportType: 1, reason: '' })

// 整改申诉
const showAppealDialog = ref(false)
const appealSubmitting = ref(false)
const appealForm = ref({ appealReason: '' })

onMounted(async () => {
  // 记录浏览历史
  recordBrowse(route.params.id)
  product.value = await getProductDetail(route.params.id)
  activeImage.value = product.value.images?.[0]?.url || ''
  if (store.isLoggedIn()) {
    try { const r = await checkFavorite(route.params.id); isFav.value = r.isFavorited } catch (e) { /* */ }
  }
  loadComments()
  loadSimilar()
})

// 猜你喜欢：同分类在售商品，排除当前
const similar = ref([])
const loadSimilar = async () => {
  if (!product.value?.categoryId) return
  try {
    const res = await searchProducts({ categoryId: product.value.categoryId, sort: 'hot', page: 1, size: 8 })
    similar.value = (res.records || []).filter(p => Number(p.id) !== Number(route.params.id)).slice(0, 8)
  } catch (e) { /* */ }
}

const loadComments = async () => {
  loadingComments.value = true
  try {
    const r = await getComments(route.params.id, { page: 1, size: 50 })
    comments.value = r.records || []
    commentTotal.value = r.total || 0
  } catch (e) { /* */ } finally {
    loadingComments.value = false
  }
}

const submitComment = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  if (!commentText.value.trim()) return ElMessage.warning('请输入评论内容')
  commentSubmitting.value = true
  try {
    await addComment({ productId: Number(route.params.id), content: commentText.value.trim() })
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) { /* 拦截器已提示 */ } finally {
    commentSubmitting.value = false
  }
}

const submitReply = async (parentId) => {
  if (!store.isLoggedIn()) return router.push('/login')
  if (!replyText.value.trim()) return ElMessage.warning('请输入回复内容')
  commentSubmitting.value = true
  try {
    await addComment({ productId: Number(route.params.id), parentId, content: replyText.value.trim() })
    replyText.value = ''
    replyTo.value = null
    ElMessage.success('回复成功')
    loadComments()
  } catch (e) { /* 拦截器已提示 */ } finally {
    commentSubmitting.value = false
  }
}

const removeComment = async (id) => {
  try {
    await deleteComment(id)
    ElMessage.success('已删除')
    loadComments()
  } catch (e) { /* 拦截器已提示 */ }
}

const submitIntent = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  intentSubmitting.value = true
  try {
    await createIntent(route.params.id, {
      message: intentForm.value.message?.trim() || '',
      expectedPrice: intentForm.value.expectedPrice
    })
    ElMessage.success('意向已发送，卖家会看到')
    showIntentDialog.value = false
    intentForm.value = { message: '', expectedPrice: null }
  } catch (e) { /* 拦截器已提示 */ } finally {
    intentSubmitting.value = false
  }
}

const submitReport = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  if (!reportForm.value.reason.trim()) return ElMessage.warning('请填写举报理由')
  reportSubmitting.value = true
  try {
    await createReport({
      productId: Number(route.params.id),
      reportType: reportForm.value.reportType,
      reason: reportForm.value.reason.trim()
    })
    ElMessage.success('举报已提交，等待审核处理')
    showReportDialog.value = false
    reportForm.value = { reportType: 1, reason: '' }
  } catch (e) { /* 拦截器已提示 */ } finally {
    reportSubmitting.value = false
  }
}

const submitAppeal = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  if (!appealForm.value.appealReason.trim()) return ElMessage.warning('请填写整改说明')
  appealSubmitting.value = true
  try {
    await createAppeal({
      productId: Number(route.params.id),
      appealReason: appealForm.value.appealReason.trim()
    })
    ElMessage.success('申诉已提交，等待审核')
    showAppealDialog.value = false
    appealForm.value = { appealReason: '' }
  } catch (e) { /* 拦截器已提示 */ } finally {
    appealSubmitting.value = false
  }
}

const fmtTime = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : ''

const toggleFav = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  isFav.value ? await removeFavorite(route.params.id) : await addFavorite(route.params.id)
  isFav.value = !isFav.value
}

const buyNow = () => {
  if (!store.isLoggedIn()) return router.push('/login')
  // 进入确认下单页（选择地址 → 创建订单）
  router.push({ name: 'OrderCheckout', query: { productId: route.params.id } })
}

const contactSeller = async () => {
  if (!store.isLoggedIn()) return router.push('/login')
  const conv = await getOrCreateConversation({ targetUserId: product.value.sellerId, productId: product.value.id })
  router.push(`/chat/${conv.id}`)
}
</script>

<style scoped>
.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 40px;
  margin-bottom: 40px;
}
@media (max-width: 768px) {
  .detail-grid { grid-template-columns: 1fr; gap: 20px; }
  .detail-gallery { position: static; }
  .main-image-wrap { aspect-ratio: 4/3; border-radius: 14px; }
  .detail-title { font-size: 18px; }
  .detail-price { font-size: 28px; }
  .detail-attrs { grid-template-columns: 1fr 1fr; }
  .detail-actions { flex-wrap: wrap; }
  .action-buy { flex: 1 1 100%; }
  .action-chat { flex: 1; }
}
@media (max-width: 480px) {
  .detail-grid { gap: 14px; }
  .main-image-wrap { aspect-ratio: 1/1; border-radius: 12px; }
  .detail-title { font-size: 16px; }
  .detail-price { font-size: 24px; }
  .detail-original { font-size: 12px; }
  .detail-attrs { grid-template-columns: 1fr; gap: 8px; padding: 12px 14px; }
  .detail-actions { gap: 8px; }
  .action-buy, .action-chat { height: 44px; font-size: 14px; border-radius: 12px; }
  .action-fav { width: 44px; height: 44px; }
  .thumb { width: 50px; height: 50px; }
  .section-title { font-size: 16px; }
  .desc-content { padding: 16px; font-size: 14px; }
  .seller-card { padding: 12px 14px; }
}

/* 图片画廊 */
.detail-gallery { position: sticky; top: 80px; }
.main-image-wrap {
  position: relative; border-radius: 20px; overflow: hidden;
  background: #f8f9fa; aspect-ratio: 1;
}
.main-image { width: 100%; height: 100%; object-fit: contain; }
.img-badge {
  position: absolute; top: 12px; left: 12px;
  background: rgba(0,0,0,0.5); color: #fff;
  padding: 4px 14px; border-radius: 12px; font-size: 13px;
  backdrop-filter: blur(4px);
}
.thumb-list { display: flex; gap: 10px; margin-top: 12px; }
.thumb {
  width: 68px; height: 68px; border-radius: 12px; overflow: hidden;
  cursor: pointer; border: 2px solid transparent;
  transition: all 0.2s; opacity: 0.6;
}
.thumb:hover { opacity: 0.9; }
.thumb.active { border-color: #ff6b35; opacity: 1; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }

/* 详情信息 */
.detail-title { font-size: 22px; font-weight: 700; color: #2d3436; line-height: 1.4; margin-bottom: 16px; }
.detail-price-row { display: flex; align-items: baseline; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
.detail-price { font-size: 36px; font-weight: 800; color: #e74c3c; font-family: "SF Pro Display", sans-serif; }
.detail-unit { font-size: 16px; color: #e74c3c; }
.detail-original { font-size: 14px; color: #b2bec3; text-decoration: line-through; margin-left: 8px; }
.discount-tag {
  font-size: 12px; font-weight: 700; color: #fff;
  background: #e74c3c; padding: 3px 10px; border-radius: 10px;
}

/* 属性 */
.detail-attrs {
  background: #f8f9fa; border-radius: 14px; padding: 16px 20px;
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  margin-bottom: 24px;
}
.attr-item { display: flex; flex-direction: column; gap: 2px; }
.attr-label { font-size: 12px; color: #b2bec3; }
.attr-val { font-size: 14px; font-weight: 600; color: #2d3436; }

/* 操作按钮 */
.detail-actions { display: flex; gap: 10px; margin-bottom: 20px; }
.action-buy {
  flex: 1; height: 52px; border: none;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff; font-size: 16px; font-weight: 700;
  border-radius: 16px; cursor: pointer;
  transition: all 0.3s;
}
.action-buy:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,107,53,0.35); }
.action-chat {
  height: 52px; border: 2px solid #dfe6e9; background: #fff;
  color: #2d3436; font-size: 14px; font-weight: 600;
  padding: 0 20px; border-radius: 16px; cursor: pointer;
  display: flex; align-items: center; gap: 6px;
  transition: all 0.25s;
}
.action-chat:hover { border-color: #ff6b35; color: #ff6b35; background: #fff8f5; }
.action-fav {
  width: 52px; height: 52px; border: 2px solid #dfe6e9;
  background: #fff; border-radius: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s; color: #b2bec3;
}
.action-fav:hover { border-color: #ff6b35; background: #fff8f5; }
.action-fav.favorited { border-color: #ff6b35; color: #ff6b35; background: #fff5f0; }

/* 举报入口 / 违规整改卡片 */
.report-entry { margin: -8px 0 14px; text-align: right; }
.banned-card {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: #fef2f0; border: 1px solid #fde0da; border-radius: 14px;
  padding: 14px 18px; margin-bottom: 18px;
}
.banned-tip strong { color: #e74c3c; font-size: 14px; display: block; margin-bottom: 4px; }
.banned-tip p { margin: 0; font-size: 13px; color: #636e72; line-height: 1.6; }
.banned-tip .banned-reason { color: #e74c3c; font-weight: 600; }

/* 卖家卡片 */
.seller-card {
  background: #fff; border: 1px solid #f0f2f5; border-radius: 16px;
  padding: 16px 20px;
}
.seller-link { display: flex; align-items: center; gap: 12px; text-decoration: none; }
.seller-info strong { font-size: 15px; color: #2d3436; display: block; }
.seller-rep { display: flex; align-items: center; gap: 6px; margin-top: 2px; }
.seller-rep span { font-size: 13px; color: #ff6b35; font-weight: 600; }

/* 描述区 */
.detail-section { margin-bottom: 32px; }
.section-title {
  font-size: 18px; font-weight: 700; color: #2d3436;
  margin-bottom: 16px; padding-left: 14px; border-left: 4px solid #ff6b35;
}
.desc-content {
  background: #fff; border-radius: 16px; padding: 24px;
  border: 1px solid #f0f2f5; font-size: 15px; line-height: 1.8;
  color: #636e72; white-space: pre-wrap;
}
.desc-content.empty { color: #b2bec3; text-align: center; padding: 40px; }

/* 评论区 */
.comment-box { background: #fff; border-radius: 16px; padding: 24px; border: 1px solid #f0f2f5; }
.comment-input { margin-bottom: 20px; }
.comment-input-actions { text-align: right; margin-top: 10px; }
.comment-login-tip { text-align: center; padding: 24px; color: #b2bec3; font-size: 14px; }
.comment-login-tip a { color: #ff6b35; font-weight: 600; }
.comment-list { display: flex; flex-direction: column; gap: 20px; }
.comment-item { display: flex; gap: 12px; }
.comment-body { flex: 1; min-width: 0; }
.comment-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.comment-meta strong { font-size: 14px; color: #2d3436; }
.comment-time { font-size: 12px; color: #b2bec3; }
.comment-ops { margin-left: auto; }
.comment-content { font-size: 14px; color: #2d3436; line-height: 1.7; margin-top: 4px; word-break: break-word; }
.comment-reply { margin-top: 10px; padding-top: 12px; border-top: 1px dashed #f0f2f5; }
.comment-reply-actions { text-align: right; margin-top: 8px; }
.comment-empty { text-align: center; color: #b2bec3; padding: 32px; font-size: 14px; }

/* 猜你喜欢 */
.similar-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
@media (max-width: 768px) { .similar-grid { grid-template-columns: repeat(2, 1fr); } }
.similar-card { background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #f0f2f5; cursor: pointer; transition: all 0.25s; }
.similar-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
.similar-img { width: 100%; height: 130px; }
.similar-info { padding: 8px 10px; }
.similar-title { font-size: 13px; font-weight: 600; color: #2d3436; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.similar-price { font-size: 15px; font-weight: 700; color: #ff6b35; margin-top: 2px; }
@media (max-width: 480px) {
  .comment-box { padding: 16px; }
}
</style>
