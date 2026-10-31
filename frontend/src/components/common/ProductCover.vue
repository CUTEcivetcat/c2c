<template>
  <div class="product-cover">
    <!-- 有图：加载失败时降级为服务端占位图（带商品名） -->
    <el-image v-if="src" :src="src" :fit="fit" class="pc-img" :preview-src-list="preview ? [src] : undefined">
      <template #error>
        <img class="pc-img" :src="placeholderUrl" alt="商品占位图" />
      </template>
    </el-image>
    <!-- 无图：服务端占位图（把商品名渲染进图片，无需上传也有封面） -->
    <img v-else class="pc-img" :src="placeholderUrl" alt="商品占位图" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
/**
 * 商品图片组件：
 * - 有图正常展示，加载失败降级为占位图
 * - 无图时展示服务端 SVG 占位图（后端 /placeholder.svg 按 text 参数渲染商品名）
 * props:
 *   src     - 图片地址，为空则显示占位图
 *   title   - 商品标题（用于占位图上显示的文字）
 *   fit     - 图片填充方式（cover/contain），默认 cover
 *   preview - 是否支持点击放大预览，默认 false
 */
const props = defineProps({
  src: { type: String, default: '' },
  title: { type: String, default: '' },
  fit: { type: String, default: 'cover' },
  preview: { type: Boolean, default: false }
})

const placeholderUrl = computed(() => {
  const text = (props.title || '青禾集市').trim().slice(0, 20) || '青禾集市'
  return `/api/v1/placeholder.svg?text=${encodeURIComponent(text)}`
})
</script>

<style scoped>
.product-cover {
  width: 100%; height: 100%; overflow: hidden;
  background: #f2f3f5;
}
.pc-img { width: 100%; height: 100%; display: block; object-fit: cover; }
</style>
