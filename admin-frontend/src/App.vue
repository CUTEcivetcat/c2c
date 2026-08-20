<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <span>🔄</span>
        <strong>C2C Admin</strong>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item"><el-icon><DataAnalysis /></el-icon><span>数据大屏</span></router-link>
        <router-link to="/users" class="nav-item"><el-icon><User /></el-icon><span>用户管理</span></router-link>
        <router-link to="/permissions" class="nav-item"><el-icon><Lock /></el-icon><span>权限管理</span></router-link>
        <router-link to="/review" class="nav-item"><el-icon><Checked /></el-icon><span>审核管理</span></router-link>
        <router-link to="/products" class="nav-item"><el-icon><Goods /></el-icon><span>商品管理</span></router-link>
        <router-link to="/orders" class="nav-item"><el-icon><Document /></el-icon><span>订单管理</span></router-link>
        <router-link to="/announcement" class="nav-item"><el-icon><Bell /></el-icon><span>公告管理</span></router-link>
      </nav>
      <div class="sidebar-footer">
        <button @click="logout">退出后台</button>
      </div>
    </aside>
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const logout = () => { localStorage.removeItem('admin_token'); router.push('/login') }
</script>

<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;background:#f0f2f5}
.admin-layout{display:flex;min-height:100vh}
.admin-sidebar{width:220px;background:#1a1a2e;color:#fff;display:flex;flex-direction:column;position:fixed;top:0;left:0;bottom:0;z-index:100}
.sidebar-brand{display:flex;align-items:center;gap:8px;padding:20px;font-size:18px;border-bottom:1px solid rgba(255,255,255,0.1)}
.sidebar-nav{flex:1;padding:12px 0}
.nav-item{display:flex;align-items:center;gap:10px;padding:12px 20px;color:rgba(255,255,255,0.65);text-decoration:none;font-size:14px;transition:all 0.2s}
.nav-item:hover,.nav-item.router-link-active{color:#fff;background:rgba(255,255,255,0.08)}
.sidebar-footer{padding:16px 20px;border-top:1px solid rgba(255,255,255,0.1)}
.sidebar-footer button{width:100%;padding:8px;border:1px solid rgba(255,255,255,0.2);background:transparent;color:#fff;border-radius:8px;cursor:pointer;font-size:13px}
.admin-main{flex:1;margin-left:220px;padding:24px}
@media(max-width:768px){.admin-sidebar{width:60px}.admin-sidebar .nav-item span,.admin-sidebar .sidebar-brand strong,.admin-sidebar .sidebar-footer button span{display:none}.admin-main{margin-left:60px;padding:16px}.nav-item{justify-content:center;padding:14px 0}}
</style>
