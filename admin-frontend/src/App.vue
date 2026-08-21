<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ collapsed }">
      <div class="sidebar-brand">
        <span class="brand-icon">🔄</span>
        <div class="brand-text" v-show="!collapsed">
          <strong>闲小鱼</strong>
          <small>管理后台</small>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item"><el-icon><DataAnalysis /></el-icon><span v-show="!collapsed">数据大屏</span></router-link>
        <router-link to="/users" class="nav-item"><el-icon><User /></el-icon><span v-show="!collapsed">用户管理</span></router-link>
        <router-link to="/permissions" class="nav-item"><el-icon><Lock /></el-icon><span v-show="!collapsed">权限管理</span></router-link>
        <router-link to="/review" class="nav-item"><el-icon><Checked /></el-icon><span v-show="!collapsed">审核管理</span></router-link>
        <router-link to="/products" class="nav-item"><el-icon><Goods /></el-icon><span v-show="!collapsed">商品管理</span></router-link>
        <router-link to="/orders" class="nav-item"><el-icon><Document /></el-icon><span v-show="!collapsed">订单管理</span></router-link>
        <router-link to="/banner" class="nav-item"><el-icon><Picture /></el-icon><span v-show="!collapsed">轮播图管理</span></router-link>
        <router-link to="/announcement" class="nav-item"><el-icon><Bell /></el-icon><span v-show="!collapsed">公告管理</span></router-link>
      </nav>
      <!-- 收起按钮（导航与退出之间，居中） -->
      <div class="sidebar-collapse">
        <button class="collapse-btn" @click="collapsed = !collapsed">
          <el-icon><ArrowLeft v-if="!collapsed" /><ArrowRight v-else /></el-icon>
        </button>
      </div>
      <div class="sidebar-footer">
        <div class="admin-user">
          <el-avatar :size="32" class="admin-avatar">{{ adminName.charAt(0) || 'A' }}</el-avatar>
          <div class="admin-info" v-show="!collapsed">
            <strong class="admin-nick">{{ adminName || '管理员' }}</strong>
            <small>在线</small>
          </div>
        </div>
        <button class="logout-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon><span v-show="!collapsed">退出后台</span>
        </button>
      </div>
    </aside>
    <main class="admin-main" :class="{ collapsed }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const collapsed = ref(true)
const adminName = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('admin_user') || '{}')
    return u.nickname || u.username || ''
  } catch { return '' }
})
const logout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_user')
  router.push('/login')
}
</script>

<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;background:#f0f2f5}
.admin-layout{display:flex;min-height:100vh}
.admin-sidebar{width:220px;background:linear-gradient(180deg,#1a1a2e,#16213e);color:#fff;display:flex;flex-direction:column;position:fixed;top:0;left:0;bottom:0;z-index:100;transition:width 0.25s cubic-bezier(0.4,0,0.2,1)}
.admin-sidebar.collapsed{width:60px}
.sidebar-brand{display:flex;align-items:center;gap:10px;padding:20px;border-bottom:1px solid rgba(255,255,255,0.08)}
.brand-icon{font-size:24px;flex-shrink:0}
.brand-text{display:flex;flex-direction:column;line-height:1.2;overflow:hidden}
.brand-text strong{font-size:17px;letter-spacing:1px;white-space:nowrap}
.brand-text small{font-size:11px;color:rgba(255,255,255,0.5);margin-top:2px;white-space:nowrap}
.sidebar-nav{flex:1;padding:12px 0;overflow:hidden}
.nav-item{display:flex;align-items:center;gap:10px;padding:12px 20px;color:rgba(255,255,255,0.65);text-decoration:none;font-size:14px;transition:all 0.2s;border-left:3px solid transparent;white-space:nowrap}
.admin-sidebar.collapsed .nav-item{padding:14px 0;justify-content:center;border-left:none}
.nav-item:hover{color:#fff;background:rgba(255,255,255,0.06)}
.nav-item.router-link-active{color:#fff;background:rgba(255,255,255,0.1);border-left-color:#ff6b35}
.sidebar-footer{padding:14px 16px;border-top:1px solid rgba(255,255,255,0.08);position:relative}
.admin-sidebar.collapsed .sidebar-footer{padding:14px 8px}
.admin-user{display:flex;align-items:center;gap:10px;padding:6px 4px 12px}
.admin-sidebar.collapsed .admin-user{justify-content:center;padding:6px 0 12px}
.admin-avatar{background:linear-gradient(135deg,#ff6b35,#ff8c5a);flex-shrink:0}
.admin-info{display:flex;flex-direction:column;line-height:1.3;min-width:0}
.admin-nick{font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.admin-info small{font-size:11px;color:#00b894}
.logout-btn{width:100%;padding:9px;border:1px solid rgba(255,255,255,0.15);background:transparent;color:rgba(255,255,255,0.8);border-radius:8px;cursor:pointer;font-size:13px;display:flex;align-items:center;justify-content:center;gap:6px;transition:all 0.2s}
.admin-sidebar.collapsed .logout-btn span{display:none}
.logout-btn:hover{background:rgba(231,76,60,0.25);border-color:#e74c3c;color:#fff}

/* 收起按钮容器（居中） */
.sidebar-collapse {
  display: flex; justify-content: center; padding: 8px 0;
  border-top: 1px solid rgba(255,255,255,0.06);
}

/* 收起按钮 */
.collapse-btn {
  width: 36px; height: 36px; border: none; border-radius: 10px;
  background: rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.5); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; transition: all 0.25s ease;
}
.collapse-btn:hover {
  background: rgba(255,255,255,0.18); color: #ff6b35;
  transform: scale(1.1);
}
.collapse-btn:active { transform: scale(0.95); }
.admin-sidebar.collapsed .collapse-btn {
  background: rgba(255,255,255,0.06);
}

.admin-main{flex:1;margin-left:220px;padding:24px;overflow-x:hidden;transition:margin-left 0.25s cubic-bezier(0.4,0,0.2,1)}
.admin-main.collapsed{margin-left:60px}

/* ===== 全局 uiverse 风格覆盖 ===== */

/* 卡片统一样式 */
.admin-main .el-card {
  border-radius: 16px !important;
  border: 1px solid #f0f2f5 !important;
  transition: all 0.25s ease !important;
}
.admin-main .el-card:hover {
  box-shadow: 0 8px 28px rgba(0,0,0,0.07) !important;
}

/* 表格统一样式 */
.admin-main .el-table {
  border-radius: 14px;
  overflow: hidden;
}
.admin-main .el-table th.el-table__cell {
  background: #fafbfc !important;
  font-weight: 700 !important;
  color: #2d3436 !important;
}
.admin-main .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: #f8f9fa !important;
}

/* 输入框统一样式 */
.admin-main .el-input__wrapper {
  border-radius: 12px !important;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02) !important;
  transition: all 0.2s ease !important;
}
.admin-main .el-input__wrapper:focus-within {
  box-shadow: 0 0 0 3px rgba(255,107,53,0.1) !important;
  border-color: #ff6b35 !important;
}
.admin-main .el-select .el-input__wrapper {
  border-radius: 12px !important;
}

/* 按钮统一样式 */
.admin-main .el-button {
  border-radius: 10px !important;
  font-weight: 600 !important;
  transition: all 0.2s ease !important;
}
.admin-main .el-button--primary {
  background: linear-gradient(135deg, #ff6b35, #ff8c5a) !important;
  border: none !important;
  color: #fff !important;
}
.admin-main .el-button--primary:hover {
  background: linear-gradient(135deg, #e55a2b, #ff6b35) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(255,107,53,0.3) !important;
}
.admin-main .el-button--primary:active {
  transform: translateY(0);
}
.admin-main .el-button--success {
  background: linear-gradient(135deg, #67c23a, #85ce61) !important;
  border: none !important;
  color: #fff !important;
}
.admin-main .el-button--success:hover {
  box-shadow: 0 4px 14px rgba(103,194,58,0.3) !important;
  transform: translateY(-1px);
}
.admin-main .el-button--warning {
  background: linear-gradient(135deg, #e6a23c, #f0b84d) !important;
  border: none !important;
  color: #fff !important;
}
.admin-main .el-button--warning:hover {
  box-shadow: 0 4px 14px rgba(230,162,60,0.3) !important;
  transform: translateY(-1px);
}
.admin-main .el-button--danger {
  background: linear-gradient(135deg, #e74c3c, #f06b5a) !important;
  border: none !important;
  color: #fff !important;
}
.admin-main .el-button--danger:hover {
  box-shadow: 0 4px 14px rgba(231,76,60,0.3) !important;
  transform: translateY(-1px);
}
.admin-main .el-button.is-plain {
  background: transparent !important;
  border: 1.5px solid currentColor !important;
}
.admin-main .el-button.is-plain:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06) !important;
}

/* 分页统一样式 */
.admin-main .el-pagination {
  font-weight: 500;
  padding: 12px 0 !important;
}
.admin-main .el-pagination button {
  border-radius: 8px !important;
}
.admin-main .el-pager li {
  border-radius: 8px !important;
  font-weight: 600 !important;
}

/* 标签统一样式 */
.admin-main .el-tag {
  border-radius: 8px !important;
  font-weight: 600 !important;
  border: none !important;
}

/* 弹窗统一样式 */
.admin-main .el-dialog {
  border-radius: 18px !important;
}
.admin-main .el-dialog__header {
  padding: 24px 24px 0 !important;
  font-weight: 700 !important;
}
.admin-main .el-dialog__body {
  padding: 20px 24px !important;
}
.admin-main .el-dialog__footer {
  padding: 0 24px 20px !important;
}

/* 日期选择器统一样式 */
.admin-main .el-date-editor .el-input__wrapper {
  border-radius: 12px !important;
}
.admin-main .el-picker-panel {
  border-radius: 14px !important;
}
</style>