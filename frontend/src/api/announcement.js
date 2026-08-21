import request from './request'

export const getAnnouncementList = (params) => request.get('/announcement/list', { params })
export const getAnnouncementLatest = (limit = 3) => request.get('/announcement/latest', { params: { limit } })
export const getAnnouncementForce = () => request.get('/announcement/force')
export const getAnnouncementPublish = () => request.get('/announcement/publish')
