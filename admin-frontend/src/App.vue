<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ collapsed }">
      <div class="sidebar-brand">
        <span class="brand-icon">🔄</span>
        <div class="brand-text" v-show="!collapsed">
          <strong>闲小鱼</strong>
          <small>管理后台</small>
        </div>
        <button class="toggle-btn" @click="collapsed = !collapsed">
          <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </button>
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
      <div class="sidebar-footer" v-show="!collapsed">
        <div class="admin-user">
          <el-avatar :size="32" class="admin-avatar">{{ adminName.charAt(0) || 'A' }}</el-avatar>
          <div class="admin-info">
            <strong class="admin-nick">{{ adminName || '管理员' }}</strong>
            <small>在线</small>
          </div>
        </div>
        <button class="logout-btn" @click="logout"><el-icon><SwitchButton /></el-icon> 退出后台</button>
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
.admin-sidebar{width:220px;background:linear-gradient(180deg,#1a1a2e,#16213e);color:#fff;display:flex;flex-direction:column;position:fixed;top:0;left:0;bottom:0;z-index:100;transition:width 0.25s ease}
.admin-sidebar.collapsed{width:60px}
.sidebar-brand{display:flex;align-items:center;gap:10px;padding:20px;border-bottom:1px solid rgba(255,255,255,0.08);position:relative}
.brand-icon{font-size:24px;flex-shrink:0}
.brand-text{display:flex;flex-direction:column;line-height:1.2;overflow:hidden}
.brand-text strong{font-size:17px;letter-spacing:1px;white-space:nowrap}
.brand-text small{font-size:11px;color:rgba(255,255,255,0.5);margin-top:2px;white-space:nowrap}
.toggle-btn{position:absolute;right:8px;top:50%;transform:translateY(-50%);background:none;border:none;color:rgba(255,255,255,0.5);cursor:pointer;padding:4px;border-radius:6px;display:flex;align-items:center;transition:all 0.2s}
.toggle-btn:hover{color:#fff;background:rgba(255,255,255,0.1)}
.admin-sidebar.collapsed .toggle-btn{right:50%;transform:translateY(-50%) translateX(50%)}
.sidebar-nav{flex:1;padding:12px 0;overflow-y:auto}
.nav-item{display:flex;align-items:center;gap:10px;padding:12px 20px;color:rgba(255,255,255,0.65);text-decoration:none;font-size:14px;transition:all 0.2s;border-left:3px solid transparent;white-space:nowrap}
.admin-sidebar.collapsed .nav-item{padding:14px 0;justify-content:center;border-left:none}
.nav-item:hover{color:#fff;background:rgba(255,255,255,0.06)}
.nav-item.router-link-active{color:#fff;background:rgba(255,255,255,0.1);border-left-color:#ff6b35}
.sidebar-footer{padding:14px 16px;border-top:1px solid rgba(255,255,255,0.08)}
.admin-user{display:flex;align-items:center;gap:10px;padding:6px 4px 12px}
.admin-avatar{background:linear-gradient(135deg,#ff6b35,#ff8c5a);flex-shrink:0}
.admin-info{display:flex;flex-direction:column;line-height:1.3;min-width:0}
.admin-nick{font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.admin-info small{font-size:11px;color:#00b894}
.logout-btn{width:100%;padding:9px;border:1px solid rgba(255,255,255,0.15);background:transparent;color:rgba(255,255,255,0.8);border-radius:8px;cursor:pointer;font-size:13px;display:flex;align-items:center;justify-content:center;gap:6px;transition:all 0.2s}
.logout-btn:hover{background:rgba(231,76,60,0.25);border-color:#e74c3c;color:#fff}
.admin-main{flex:1;margin-left:220px;padding:24px;transition:margin-left 0.25s ease}
.admin-main.collapsed{margin-left:60px}
</style>