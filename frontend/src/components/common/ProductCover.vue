<template>
  <div class="product-cover">
    <!-- 有图：加载失败时展示"暂无图片"占位 -->
    <el-image v-if="src" :src="src" :fit="fit" class="pc-img" :preview-src-list="preview ? [src] : undefined">
      <template #error>
        <div class="pc-fallback"><span>🖼️</span><em>暂无图片</em></div>
      </template>
    </el-image>
    <!-- 无图：直接展示占位 -->
    <div v-else class="pc-fallback"><span>🖼️</span><em>暂无图片</em></div>
  </div>
</template>

<script setup>
/**
 * 商品图片组件：无图或图片加载失败时统一展示"暂无图片"占位，避免破图。
 * 尺寸由外层容器控制（组件占满父容器 100%×100%）。
 * props:
 *   src     - 图片地址，为空则显示占位
 *   fit     - 图片填充方式（cover/contain），默认 cover
 *   preview - 是否支持点击放大预览，默认 false
 */
defineProps({
  src: { type: String, default: '' },
  fit: { type: String, default: 'cover' },
  preview: { type: Boolean, default: false }
})
</script>

<style scoped>
.product-cover {
  width: 100%; height: 100%; overflow: hidden;
  background: #f2f3f5;
}
.pc-img { width: 100%; height: 100%; display: block; }
.pc-fallback {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  background: linear-gradient(135deg, #f7f8fa, #eceef2);
  color: #b0b7c3; font-size: 12px; user-select: none;
}
.pc-fallback span { font-size: 22px; line-height: 1; }
.pc-fallback em { font-style: normal; }
</style>
