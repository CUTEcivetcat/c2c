// 主题切换：亮色 / 暗色，localStorage 持久化，html.dark 驱动 CSS 变量与 Element 暗色
// @author wzh
import { ref, computed } from 'vue'

const STORAGE_KEY = 'c2c_theme'

function readStored() {
  try { return localStorage.getItem(STORAGE_KEY) || '' } catch (e) { return '' }
}

const theme = ref(readStored() === 'dark' ? 'dark' : 'light')

function apply(t) {
  const el = document.documentElement
  el.classList.toggle('dark', t === 'dark')
  el.style.colorScheme = t === 'dark' ? 'dark' : 'light'
  try { localStorage.setItem(STORAGE_KEY, t) } catch (e) { /* 隐私模式下忽略 */ }
}

// 模块加载即应用一次，避免首帧闪烁（index.html 内联脚本已先置 class）
apply(theme.value)

export function useTheme() {
  const isDark = computed(() => theme.value === 'dark')
  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    apply(theme.value)
  }
  return { theme, isDark, toggleTheme }
}
