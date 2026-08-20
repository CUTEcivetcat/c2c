import request from './request'

// ==================== 举报（用户端） ====================
// 提交举报（body: productId / reportType / reason / images）
export const createReport = (data) => request.post('/report', data)
// 我的举报列表
export const getMyReports = (params) => request.get('/report/my', { params })

// ==================== 整改申诉（卖家端） ====================
// 提交整改申诉（body: productId / appealReason / images）
export const createAppeal = (data) => request.post('/appeal', data)
// 我的整改申诉列表
export const getMyAppeals = (params) => request.get('/appeal/my', { params })

// ==================== 审核工作台（审核员 role=2 或管理员） ====================
// 举报列表 / 详情 / 处理
export const getReports = (params) => request.get('/review/reports', { params })
export const getReportDetail = (id) => request.get(`/review/reports/${id}`)
export const handleReport = (id, data) => request.post(`/review/reports/${id}/handle`, data)
// 整改申诉列表 / 详情 / 处理
export const getAppeals = (params) => request.get('/review/appeals', { params })
export const getAppealDetail = (id) => request.get(`/review/appeals/${id}`)
export const handleAppeal = (id, data) => request.post(`/review/appeals/${id}/handle`, data)
