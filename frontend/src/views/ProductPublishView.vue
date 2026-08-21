<template>
  <div class="page-container publish-page fade-in-up">
    <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
    <div class="publish-layout">
      <!-- 左侧：表单 -->
      <div class="publish-left">
        <div class="publish-card">
          <el-form :model="form" label-position="top" size="large">
            <el-form-item label="商品标题">
              <el-input v-model="form.title" placeholder="例如：高等数学 第七版 同济大学 9成新" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="商品描述">
              <el-input v-model="form.description" type="textarea" :rows="5" placeholder="描述商品的使用情况、购买时间、有无笔记…" maxlength="2000" show-word-limit />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="8"><el-form-item label="售价 (元)"><el-input v-model.number="form.price" placeholder="0.00" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="原价 (元)"><el-input v-model.number="form.originalPrice" placeholder="选填" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="分类">
                <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
                  <el-option-group v-for="group in catGroups" :key="group.name" :label="group.name">
                    <el-option v-for="c in group.children" :key="c.id" :label="c.name" :value="c.id" />
                  </el-option-group>
                </el-select>
              </el-form-item></el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8"><el-form-item label="成色"><el-select v-model="form.condition" style="width:100%"><el-option :value="1" label="全新"/><el-option :value="2" label="几乎全新"/><el-option :value="3" label="轻微使用"/><el-option :value="4" label="明显使用"/></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="运费"><el-select v-model="form.freightType" style="width:100%"><el-option :value="1" label="包邮"/><el-option :value="2" label="买家自付"/></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="所在校区/城市"><el-input v-model="form.location" placeholder="如：南校区 / 深圳" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="商品图片（第一张为封面）">
              <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px">
                <div v-for="(img, i) in existingImages" :key="'old-'+i" class="img-preview-item">
                  <product-cover :src="img.url" /><el-icon class="img-del" @click="removeExisting(i)"><CircleClose /></el-icon>
                  <span v-if="i===0" class="img-cover-tag">封面</span>
                </div>
              </div>
              <el-upload action="/api/v1/upload/image" :headers="uploadHeaders" list-type="picture-card" :on-success="onUploadSuccess" :on-remove="onRemove" :file-list="uploadFiles">
                <el-icon><Plus /></el-icon>
              </el-upload>
              <p class="upload-tip">支持 jpg/png/webp，单张最大 10MB，最多 9 张</p>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" @click="submit" :loading="loading" style="width:200px;height:48px;border-radius:14px;font-size:15px">
                {{ loading ? '提交中...' : (isEdit ? '保存修改' : '发布商品') }}
              </el-button>
              <el-button v-if="isEdit" size="large" style="margin-left:12px" @click="$router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧：公告面板（show_on_publish=1 的公告） -->
      <div class="publish-right" v-if="publishAnn.length">
        <div class="right-panel">
          <h3 class="panel-title">📢 平台公告</h3>
          <div v-for="a in publishAnn" :key="a.id" class="ann-item">
            <div class="ann-item-head" @click="toggleAnn(a.id)">
              <el-tag size="small" :type="annTag(a.type)">{{ annText(a.type) }}</el-tag>
              <span class="ann-item-title">{{ a.title }}</span>
              <el-icon :class="{ rotated: expanded === a.id }"><ArrowDown /></el-icon>
            </div>
            <div v-if="expanded === a.id" class="ann-item-body">{{ a.content }}</div>
          </div>
          <router-link to="/announcement" class="panel-more">查看全部公告 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCategories, publishProduct, updateProduct, getProductDetail } from '@/api/product'
import { getAnnouncementPublish } from '@/api/announcement'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isEdit = computed(() => !!route.query.edit)
const productId = computed(() => route.query.edit)
const cats = ref([])
const loading = ref(false)
const images = ref([])
const existingImages = ref([])
const uploadFiles = ref([])
const form = ref({ title:'', description:'', categoryId:null, price:null, originalPrice:null, condition:2, freightType:1, location:'', images:[] })
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

// 公告面板
const publishAnn = ref([])
const expanded = ref(null)
const toggleAnn = (id) => { expanded.value = expanded.value === id ? null : id }
const annText = (t) => ({ 1:'公告', 2:'平台公约', 3:'通知' }[t] || '公告')
const annTag = (t) => ({ 1:'', 2:'warning', 3:'info' }[t] || '')

// 后端返回树形结构（顶级分类含 children），直接使用，无需扁平化
const catGroups = computed(() =>
  (cats.value || []).map(c => ({ name: c.name, children: c.children || [] }))
)

onMounted(async () => {
  cats.value = await getCategories() || []
  if (isEdit.value) {
    const product = await getProductDetail(productId.value)
    form.value = {
      title: product.title || '',
      description: product.description || '',
      categoryId: product.categoryId,
      price: product.price,
      originalPrice: product.originalPrice,
      condition: product.condition,
      freightType: product.freightType,
      location: product.location || '',
      images: (product.images || []).map(i => i.url)
    }
    existingImages.value = product.images || []
    images.value = (product.images || []).map(i => i.url)
  }
  // 加载发布页公告
  getAnnouncementPublish().then(r => { publishAnn.value = r || [] }).catch(() => {})
})

const onUploadSuccess = (res) => { if (res.code===200) { images.value.push(res.data.url); form.value.images = images.value } }
const onRemove = (file) => { images.value = images.value.filter(u => u !== file.response?.data?.url); form.value.images = images.value }
const removeExisting = (i) => { existingImages.value.splice(i, 1); images.value = existingImages.value.map(im => im.url); form.value.images = images.value }

const submit = async () => {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return }
  if (!form.value.price) { ElMessage.warning('请输入价格'); return }
  if (!form.value.categoryId) { ElMessage.warning('请选择分类'); return }
  loading.value = true
  try {
    if (isEdit.value) {
      await updateProduct(productId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await publishProduct(form.value)
      ElMessage.success('发布成功')
    }
    router.push('/')
  } catch (e) { /* */ }
  loading.value = false
}
</script>

<style scoped>
.publish-page { max-width: 960px; }
.publish-layout { display: flex; gap: 20px; align-items: flex-start; }
.publish-left { flex: 1; min-width: 0; }
.publish-card { background: #fff; border-radius: 20px; padding: 32px; border: 1px solid #f0f2f5; }
.upload-tip { font-size: 12px; color: #b2bec3; margin-top: 6px; }
:deep(.el-form-item__label) { font-weight: 600; color: #2d3436; }
.img-preview-item { position:relative; width:100px; height:100px; border-radius:8px; overflow:hidden; display:inline-block }
.img-preview-item img { width:100%; height:100%; object-fit:cover }
.img-del { position:absolute; top:2px; right:2px; color:#e74c3c; cursor:pointer; background:#fff; border-radius:50% }
.img-cover-tag { position:absolute; bottom:2px; left:2px; background:rgba(0,0,0,0.5);color:#fff;font-size:10px;padding:1px 6px;border-radius:4px }

/* 右侧公告面板 */
.publish-right { width: 280px; flex-shrink: 0; position: sticky; top: 76px; }
.right-panel { background: #fff; border-radius: 16px; padding: 20px; border: 1px solid #f0f2f5; }
.panel-title { font-size: 15px; font-weight: 700; color: #2d3436; margin-bottom: 12px; }
.ann-item { border-bottom: 1px solid #f5f5f5; padding: 8px 0; }
.ann-item:last-child { border-bottom: none; }
.ann-item-head { display: flex; align-items: center; gap: 6px; cursor: pointer; user-select: none; }
.ann-item-title { flex: 1; font-size: 13px; color: #2d3436; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ann-item-head .el-icon { font-size: 12px; color: #b2bec3; transition: transform 0.2s; }
.ann-item-head .el-icon.rotated { transform: rotate(180deg); }
.ann-item-body { margin-top: 8px; font-size: 12px; color: #636e72; line-height: 1.8; white-space: pre-wrap; word-break: break-word; background: #fafafa; border-radius: 8px; padding: 10px 12px; max-height: 200px; overflow-y: auto; }
.panel-more { display: block; margin-top: 10px; font-size: 12px; color: #ff6b35; text-align: center; text-decoration: none; }
.panel-more:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .publish-layout { flex-direction: column; }
  .publish-right { width: 100%; position: static; }
}
@media (max-width: 480px) { .publish-card { padding: 16px 12px; } }
</style>