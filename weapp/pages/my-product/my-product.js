const api = require('../../utils/api.js')
Page({
  data:{ list:[], statuses:{1:'在售',2:'已下架',3:'违规下架',4:'已售出'} },
  onShow:function(){ this.load() },
  load:function(){ var t=this; api.getMyPublished({page:1,size:50}).then(function(r){ t.setData({list:r.records||[]}) }) },
  edit:function(e){ wx.navigateTo({url:'/pages/publish/publish?id='+e.currentTarget.dataset.id}) },
  off:function(e){ var t=this,id=e.currentTarget.dataset.id; api.offShelf(id).then(function(){ t.load() }) },
  go:function(e){ wx.navigateTo({url:'/pages/product/product?id='+e.currentTarget.dataset.id}) }
})