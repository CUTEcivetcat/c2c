import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAvailableCoupons, getMyCoupons } from '@/api/coupon'
import { getCategories } from '@/api/product'

/**
 * 券相关前端共享状态：
 * - 分类祖先链映射：前端持券过滤与后端 calculate 口径一致（父类券可用于子类商品）
 * - available / 我的未用券会话级缓存，避免首页与详情页重复请求；领取/登出后失效
 */
export const useCouponStore = defineStore('coupon', () => {
  // 分类 id -> parentId 映射
  const parentMap = ref({})
  const categoryLoaded = ref(false)

  const walkTree = (list, map) => {
    ;(list || []).forEach(c => {
      map[c.id] = c.parentId || 0
      if (c.children?.length) walkTree(c.children, map)
    })
  }

  const ensureCategories = async () => {
    if (categoryLoaded.value) return
    try {
      const tree = await getCategories()
      hydrateCategoryTree(tree)
    } catch (e) { /* 分类拉取失败回退同级过滤，不阻塞券栏 */ }
  }

  // 用已拿到的分类树填充 parentId 映射，避免重复请求
  const hydrateCategoryTree = (tree) => {
    const map = {}
    walkTree(tree || [], map)
    parentMap.value = map
    categoryLoaded.value = true
  }

  // 收集商品分类自身及全部祖先 ID（防环、深链兜底）
  const ancestorChain = (catId) => {
    const chain = new Set()
    let cur = catId
    let guard = 0
    while (cur != null && Number(cur) > 0 && !chain.has(Number(cur)) && guard++ < 10) {
      chain.add(Number(cur))
      const pid = parentMap.value[cur]
      if (pid == null || Number(pid) === 0) break
      cur = pid
    }
    return chain
  }

  // 券是否适用于商品分类：通用券(categoryId=0)，或券分类落在商品分类祖先链上
  const isApplicable = (couponCategoryId, productCategoryId) => {
    if (!productCategoryId) return true
    if (!couponCategoryId || Number(couponCategoryId) === 0) return true
    return ancestorChain(productCategoryId).has(Number(couponCategoryId))
  }

  // available 缓存：键为 categoryId（空串=首页通用）
  const availCache = ref({})
  // 我的未用券缓存
  const mineUnused = ref([])
  const mineLoaded = ref(false)

  const fetchAvailable = async (categoryId, force = false) => {
    const key = categoryId ? String(categoryId) : ''
    if (!force && availCache.value[key]) return availCache.value[key]
    const params = { page: 1, size: 3 }
    if (categoryId) params.categoryId = categoryId
    const res = await getAvailableCoupons(params)
    const records = res.records || []
    availCache.value[key] = records
    return records
  }

  const fetchMine = async (force = false) => {
    if (!force && mineLoaded.value) return mineUnused.value
    const mine = await getMyCoupons(0)
    mineUnused.value = mine || []
    mineLoaded.value = true
    return mineUnused.value
  }

  // 领取成功 / 登出后清空缓存，下次重新拉取
  const invalidate = () => {
    availCache.value = {}
    mineUnused.value = []
    mineLoaded.value = false
  }

  return {
    parentMap, ensureCategories, hydrateCategoryTree, isApplicable, ancestorChain,
    fetchAvailable, fetchMine, invalidate
  }
})
