import request from './request'

// ==================== 低碳循环（我的环保贡献） ====================
export const getMyEcoStats = () => request.get('/eco/user/stats')
