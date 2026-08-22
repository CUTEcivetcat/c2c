const app = getApp()
Page({
  data:{ userInfo:null, loginSource:'' },
  onShow:function(){ this.setData({userInfo:app.globalData.userInfo,loginSource:app.globalData.userInfo?app.globalData.userInfo.loginSource:''}) },
  wechatLogin:function(){ wx.showLoading({title:'登录中'}); var t=this; app.wechatLogin(function(e){ wx.hideLoading(); if(!e&&app.globalData.userInfo){ t.setData({userInfo:app.globalData.userInfo,loginSource:app.globalData.userInfo.loginSource}); wx.showToast({title:'登录成功',icon:'success'}) } }) },
  nav:function(e){ wx.navigateTo({url:e.currentTarget.dataset.url}) },
  goAuthLogin: function(){ wx.navigateTo({ url: '/pages/auth/login' }) },
  logout:function(){ app.globalData.token='';app.globalData.userInfo=null;wx.removeStorageSync('token');wx.removeStorageSync('userInfo');this.setData({userInfo:null,loginSource:''}) }
})