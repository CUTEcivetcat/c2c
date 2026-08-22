const api = require('../../../utils/api.js')
const app = getApp()
Page({
  data:{ o:null, meId:'', statuses:{0:'待支付',1:'已支付',2:'已发货',3:'已收货',4:'已完成',5:'已取消'} },
  onLoad:function(opt){ this.setData({meId: app.globalData.userInfo?app.globalData.userInfo.id:''}); this.id=opt.id; this.load() },
  load:function(){ var t=this; api.getOrder(this.id).then(function(o){ t.setData({o:o}) }) },
  pay:function(){ var t=this; api.payOrder(this.id).then(function(){ wx.showToast({title:'支付成功',icon:'success'}); t.load() }) },
  ship:function(){ var t=this; wx.showModal({title:'发货',editable:true,placeholderText:'物流单号(可空)',success:function(r){ if(r.confirm) api.shipOrder(t.id,{shipCompany:'快递',shipNo:r.content||''}).then(function(){ wx.showToast({title:'已发货',icon:'success'}); t.load() }) }}) },
  receive:function(){ var t=this; wx.showModal({title:'确认收货',content:'确认已收到商品？',success:function(r){ if(r.confirm) api.receiveOrder(t.id).then(function(){ wx.showToast({title:'已收货',icon:'success'}); t.load() }) }}) },
  cancel:function(){ var t=this; wx.showModal({title:'取消订单',content:'确定取消？',success:function(r){ if(r.confirm) api.cancelOrder(t.id,'买家取消').then(function(){ wx.showToast({title:'已取消',icon:'success'}); t.load() }) }}) }
})