<template>
  <div class="page-container profile-page fade-in-up" style="max-width:600px">
    <page-back-bar title="个人中心" />
    <!-- 头像卡片 -->
    <div class="profile-header-card">
      <el-avatar :size="72" style="background:linear-gradient(135deg,#ff6b35,#ff8c5a);font-size:30px;font-weight:700">
        {{ store.userInfo?.nickname?.charAt(0) || 'U' }}
      </el-avatar>
      <h3>{{ store.userInfo?.nickname || '未设置昵称' }}</h3>
      <div class="rep-badge">
        <el-rate :model-value="Number(store.userInfo?.reputationScore || 5)" disabled size="small" />
        <span>{{ store.userInfo?.reputationScore || '5.0' }}</span>
      </div>
    </div>

    <!-- 功能入口 -->
    <div class="profile-menu">
      <router-link to="/my/report" class="menu-item">
        <span class="menu-ico">⚑</span><span>我的举报</span>
      </router-link>
      <router-link to="/my/appeal" class="menu-item">
        <span class="menu-ico">🔁</span><span>我的整改申诉</span>
      </router-link>
      <router-link v-if="store.userInfo?.role === 2" to="/review" class="menu-item">
        <span class="menu-ico">🛡️</span><span>审核工作台</span>
      </router-link>
    </div>

    <!-- 我的商品 -->
    <div class="my-goods-section">
      <div class="section-head">
        <h3>我的商品</h3>
        <router-link to="/publish" class="publish-link">＋ 发布商品</router-link>
      </div>
      <div v-loading="loading" class="goods-list">
        <div v-for="p in list" :key="p.id" class="goods-card">
          <router-link :to="`/product/${p.id}`" class="goods-cover-wrap">
            <el-image :src="coverOf(p)" class="goods-cover" fit="cover">
              <template #error><div class="cover-err">无图</div></template>
            </el-image>
          </router-link>
          <div class="goods-main">
            <div class="goods-title">
              <span class="title-text">{{ p.title }}</span>
              <el-tag size="small" :type="statusType(p.status)">{{ p.statusText }}</el-tag>
            </div>
            <div class="goods-price">¥{{ p.price }}</div>
            <!-- 被退回整改的状态说明 -->
            <div v-if="p.status === 5" class="banned-reason">
              <span class="banned-label">整改原因：</span>{{ p.reviewReason || '商品违规下架，请整改后提交申诉' }}
            </div>
            <div class="goods-ops">
              <!-- 在售：下架 -->
              <el-button v-if="p.status === 1" size="small" type="danger" plain @click="offShelf(p)">下架</el-button>
              <!-- 已下架：重新上架 -->
              <el-button v-else-if="p.status === 4" size="small" type="primary" plain @click="relist(p)">重新上架</el-button>
              <!-- 违规下架：整改申诉 -->
              <el-button v-else-if="p.status === 5" size="small" type="danger" @click="openAppeal(p)">整改申诉</el-button>
              <!-- 已预订 / 已售：查看详情 -->
              <el-button v-else size="small" plain @click="view(p)">查看</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="还没有发布商品" :image-size="60" />
      </div>
      <div v-if="total > 10" class="pager">
        <el-pagination layout="prev,pager,next" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </div>

    <!-- 整改申诉弹窗（违规下架商品） -->
    <el-dialog v-model="appealDialog" title="整改申诉" width="440px">
      <p style="font-size:13px;color:#b2bec3;margin:0 0 16px">说明你的整改情况，审核通过后商品恢复上架。同一商品最多申诉 3 次。</p>
      <el-form label-position="top">
        <el-form-item label="整改说明">
          <el-input v-model="appealReason" type="textarea" :rows="4" maxlength="1000" placeholder="说明整改措施，例如：已更换违规图片 / 已删除夸大描述…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialog = false">取消</el-button>
        <el-button type="danger" :loading="appealing" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 编辑表单 -->
    <div class="profile-form-card">
      <el-form :model="form" label-position="top" size="large">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="设置你的昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio-button :value="1">男</el-radio-button>
            <el-radio-button :value="2">女</el-radio-button>
            <el-radio-button :value="0">保密</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="save" style="width:100%;height:48px;border-radius:14px;font-size:15px">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { updateProfile } from '@/api/user'
import { getMyPublished, offShelfProduct, updateStatus } from '@/api/product'
import { createAppeal } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useUserStore()
const router = useRouter()
const form = ref({ nickname: '', gender: 0 })

// ============ 我的商品 ============
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const appealDialog = ref(false)
const appealReason = ref('')
const appealing = ref(false)
const appealTarget = ref(null)

// 商品状态标签：1 在售 / 2 已预订 / 3 已售 / 4 已下架 / 5 违规下架
const statusType = (s) => ({ 1: 'success', 2: 'warning', 3: 'info', 4: 'info', 5: 'danger' }[s] || 'info')

// 封面图：取 isCover 的图片，否则第一张
const coverOf = (p) => (p.images || []).find(i => i.isCover)?.url || (p.images || [])[0]?.url || ''

const load = async () => {
  loading.value = true
  try {
    const res = await getMyPublished({ page: page.value, size: size.value })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

const view = (p) => router.push(`/product/${p.id}`)

// 在售 → 下架（DELETE /product/{id}）
const offShelf = async (p) => {
  try {
    await ElMessageBox.confirm(`确认下架「${p.title}」？下架后买家将无法购买。`, '下架商品', { type: 'warning' })
  } catch (e) { return }
  try {
    await offShelfProduct(p.id)
    ElMessage.success('已下架')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

// 已下架 → 重新上架（PUT /product/{id}/status，X-Status:1）
const relist = async (p) => {
  try {
    await updateStatus(p.id, 1)
    ElMessage.success('已重新上架')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

// 违规下架 → 整改申诉
const openAppeal = (p) => {
  appealTarget.value = p
  appealReason.value = ''
  appealDialog.value = true
}
const submitAppeal = async () => {
  if (!appealReason.value.trim()) return ElMessage.warning('请填写整改说明')
  appealing.value = true
  try {
    await createAppeal({ productId: appealTarget.value.id, appealReason: appealReason.value.trim() })
    ElMessage.success('申诉已提交，等待审核')
    appealDialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { appealing.value = false }
}

onMounted(async () => {
  await store.fetchProfile()
  form.value = {
    nickname: store.userInfo?.nickname || '',
    gender: store.userInfo?.gender || 0
  }
  load()
})

const save = async () => {
  await updateProfile({ nickname: form.value.nickname, gender: form.value.gender })
  await store.fetchProfile()
  ElMessage.success('保存成功')
}
</script>

<style scoped>
.profile-header-card {
  background: #fff; border-radius: 20px; padding: 32px;
  text-align: center; border: 1px solid #f0f2f5; margin-bottom: 20px;
}
.profile-header-card h3 { font-size: 20px; font-weight: 700; margin: 12px 0 6px; color: #2d3436; }
.rep-badge { display: flex; align-items: center; justify-content: center; gap: 8px; }
.rep-badge span { font-size: 14px; font-weight: 600; color: #ff6b35; }
.profile-form-card { background: #fff; border-radius: 20px; padding: 28px; border: 1px solid #f0f2f5; }
.profile-menu {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px;
  margin-bottom: 20px;
}
.menu-item {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  background: #fff; border: 1px solid #f0f2f5; border-radius: 14px;
  padding: 18px 10px; text-decoration: none; color: #2d3436;
  font-size: 14px; font-weight: 600; transition: all 0.2s;
}
.menu-item:hover { border-color: #ff6b35; color: #ff6b35; background: #fff8f5; }
.menu-ico { font-size: 20px; }

/* 我的商品 */
.my-goods-section {
  background: #fff; border-radius: 20px; padding: 20px;
  border: 1px solid #f0f2f5; margin-bottom: 20px;
}
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.section-head h3 { margin: 0; font-size: 16px; font-weight: 700; color: #2d3436; }
.publish-link {
  font-size: 13px; font-weight: 600; color: #ff6b35; text-decoration: none;
  background: #fff5f0; border-radius: 8px; padding: 6px 12px; transition: all 0.2s;
}
.publish-link:hover { background: #ffe9de; }
.goods-list { display: flex; flex-direction: column; gap: 12px; min-height: 80px; }
.goods-card {
  display: flex; gap: 12px; padding: 12px;
  background: #fafbfc; border: 1px solid #f0f2f5; border-radius: 14px;
}
.goods-cover-wrap { flex-shrink: 0; }
.goods-cover { width: 72px; height: 72px; border-radius: 10px; display: block; }
.cover-err {
  width: 72px; height: 72px; border-radius: 10px; display: flex;
  align-items: center; justify-content: center;
  background: #f0f2f5; color: #b2bec3; font-size: 12px;
}
.goods-main { flex: 1; min-width: 0; }
.goods-title { display: flex; align-items: center; gap: 8px; }
.title-text {
  font-size: 14px; font-weight: 600; color: #2d3436;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.goods-price { margin-top: 4px; font-size: 15px; font-weight: 700; color: #e55a2b; }
.banned-reason {
  margin-top: 6px; padding: 6px 10px; border-radius: 8px;
  background: #fdf0f0; border: 1px solid #fbd5d5;
  font-size: 12px; line-height: 1.6; color: #c0392b;
}
.banned-label { font-weight: 700; }
.goods-ops { margin-top: 8px; display: flex; gap: 8px; }
.pager { text-align: center; margin-top: 14px; }
:deep(.el-radio-button__inner) { border-radius: 10px !important; }
</style>
@media (max-width: 480px) { .profile-header-card { padding: 20px; } .profile-form-card { padding: 16px; } }
