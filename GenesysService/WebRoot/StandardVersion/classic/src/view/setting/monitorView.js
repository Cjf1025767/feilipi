Ext.define('Tab.view.setting.monitorCIew', {
  extend: 'Ext.panel.Panel',
  xtype: 'monitorViewOld',
  layout: {
    type: 'hbox',
    align: 'stretch',
    pack: 'start'
  },
  requires: [
    'Ext.toolbar.TextItem',
    'Ext.view.View',
    'Ext.ux.BoxReorderer',
    'Ext.ux.DataView.Animated'
  ],
  data: [],
  ifsort: false,
  filterby: 'all',
  refreshTime: function () {
    // var fdata=this.data
    // for (var i = 0; i < fdata.length; i++) {
    //   // var obj = $.extend(true, {}, newCallArray[i])
    //   fdata[i].startTime=fdata[i].startTime+1
    // }
    // this.down('dataview').store.setData(fdata)
  },
  softstate: function (a, b) {
    var anum = 0
    var bnum = 0
    if (a.status == '登出') {
      anum = 0
    } else if (a.state != '待机') {
      anum = 2
    } else if (a.status == '就绪' || a.status == '工作就绪') {
      anum = 3
    } else if (a.status == '未就绪' || a.status == '后处理') {
      anum = 4
    }
    if (b.status == '登出') {
      bnum = 0
    } else if (b.state != '待机') {
      bnum = 2
    } else if (b.status == '就绪' || b.status == '工作就绪') {
      bnum = 3
    } else if (b.status == '未就绪' || b.status == '后处理') {
      bnum = 4
    }
    return anum - bnum
  },
  filter: function () {
    var filterby = this.filterby
    var fifdata = this.data.slice(0)
    if (filterby == '登出') {
      fifdata = fifdata.filter(function (item) {
        if (item.status == '登出') return item
      })
    } else if (filterby == '忙碌') {
      fifdata = fifdata.filter(function (item) {
        if (item.state != '待机') return item
      })
    } else if (filterby == '空闲') {
      fifdata = fifdata.filter(function (item) {
        if (
          item.state == '待机' &&
          (item.status == '就绪' || item.status == '工作就绪')
        )
          return item
      })
    } else if (filterby == '事务处理') {
      fifdata = fifdata.filter(function (item) {
        if (
          item.state == '待机' &&
          (item.status == '后处理' || item.status == '未就绪')
        )
          return item
      })
    }
    var grid = this.down('dataview')
    grid.store.setData(fifdata)
  },
  monitor: function () {
    var hostname = window.location.hostname;
    //var sMonitor = 'ws://127.0.0.1:55588'
    var sMonitor = 'ws://'+hostname+':55588'
    var MyMonitor = new $.Monitor()
    var grid = this.down('dataview')
    var me = this
    var MyMonitorMessage = function (message) {
      var objmess = JSON.parse(message)
      var noewtime=new Date()
      if (objmess.event == 'Initialize') {
        me.data = objmess.data
        //初始化加载所有分机
        //grid.store.loadData(objmess.data)
      } else {
        //后续状态变化分机
        var rec = objmess.data
        var data = me.data
        for (var i = 0; i < data.length; i++) {
          if (rec[0].name == data[i].name) {
            data[i] = rec[0]
          }
        }
        // grid.store.setData(data)
        // me.data = data
      }
      var fifdata = me.data.slice(0)
      if (me.ifsort) {
        //是否排序
        fifdata.sort(me.softstate)
      }
      //是否有过滤条件
      var filterby = me.filterby
      if (filterby == '登出') {
        fifdata = fifdata.filter(function (item) {
          if (item.status == '登出') return item
        })
      } else if (filterby == '忙碌') {
        fifdata = fifdata.filter(function (item) {
          if (item.state != '待机') return item
        })
      } else if (filterby == '空闲') {
        fifdata = fifdata.filter(function (item) {
          if (
            item.state == '待机' &&
            (item.status == '就绪' || item.status == '工作就绪')
          )
            return item
        })
      } else if (filterby == '事务处理') {
        fifdata = fifdata.filter(function (item) {
          if (
            item.state == '待机' &&
            (item.status == '后处理' || item.status == '未就绪')
          )
            return item
        })
      }

      grid.store.setData(fifdata)
      var logoutdata = me.data.filter(function (item) {
        if (item.status == '登出') return item
      })
      var logoutlen = logoutdata.length
      var logoutbut = me.down('button[name="logout"]')
      logoutbut.setText('登出:' + logoutlen)
      var alllength = me.data.length
      var allbut = me.down('button[name="all"]')
      allbut.setText('全部:' + alllength)
      var busydata = me.data.filter(function (item) {
        if (item.state != '待机') return item
      })
      var busylen = busydata.length
      var busybut = me.down('button[name="busy"]')
      busybut.setText('忙碌:' + busylen)
      var freedata = me.data.filter(function (item) {
        if (
          item.state == '待机' &&
          (item.status == '就绪' || item.status == '工作就绪')
        )
          return item
      })
      var freelen = freedata.length
      var freebut = me.down('button[name="free"]')
      freebut.setText('空闲:' + freelen)

      var transactiondata = me.data.filter(function (item) {
        if (
          item.state == '待机' &&
          (item.status == '未就绪' || item.status == '后处理')
        )
          return item
      })
      var transactionlen = transactiondata.length
      var transactionbut = me.down('button[name="transaction"]')
      transactionbut.setText('事务处理:' + transactionlen)
    }
    //var monitor = document.getElementById("var_monitor").value;
    MyMonitor.monitor(sMonitor, MyMonitorMessage)
  },
  // controller: 'setting-groupsettingview',
  // viewModel: {
  //   type: 'setting-groupsettingview'
  // },
  listeners: {
    beforerender: function () {
      this.monitor()
      
    },
    afterrender:function(){
    }
  },
  tbar: {
    
    items: [
      {
        xtype: 'button',
        text: 'ALL',
        name: 'all',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').filterby = 'all'
          this.up('panel').filter()
        }
      },
      {
        xtype: 'button',
        text: 'Busy',
        name: 'busy',
        cls:'busybutton',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').filterby = '忙碌'
          this.up('panel').filter()
        }
      },
      {
        xtype: 'button',
        text: 'Free',
        cls:'freebutton',
        name: 'free',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').filterby = '空闲'
          this.up('panel').filter()
        }
      },
      {
        xtype: 'button',
        text: 'Transaction',
        name: 'transaction',
        cls:'transactionbutton',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').filterby = '事务处理'
          this.up('panel').filter()
        }
      },
      {
        xtype: 'button',
        text: 'Logout',
        name: 'logout',
        margin: '0 31 0 0',
        cls:'logoutbutton',
        handler: function () {
          this.up('panel').filterby = '登出'
          this.up('panel').filter()
        }
      },
      {
        xtype: 'button',
        text: 'Sort',
        cls:'sortbutton',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').ifsort = true
          var data = this.up('panel').data
          var sortdata = data.slice(0)
          sortdata.sort(this.up('panel').softstate)
          this.up('panel')
            .down('dataview')
            .store.setData(sortdata)
        }
      },
      {
        xtype: 'button',
        text: 'Disorder',
        margin: '0 31 0 0',
        handler: function () {
          this.up('panel').ifsort = false
          var data = this.up('panel').data
          this.up('panel')
            .down('dataview')
            .store.setData(data)
        }
      },
     
      
    ]
  },
  items: [
    {
      xtype: 'dataview',
      listeners: {},
      reference: 'dataview',
      layout: 'fit',
      plugins: {
        'ux-animated-dataview': true
      },
      store: Ext.create('Ext.data.Store', {
        data: null
      }),
      itemSelector: 'div.dataview-multisort-item',
      tpl: [
        '<tpl for=".">',
        '<div class="dataview-multisort-item">',
        '{[this.dataSlice(values)]}',
        '</div>',
        '</tpl>',
        {
          // compiled:true,
          dataSlice: function (values) {
            var state = values.state
            var status = values.status
            var startTime = values.startTime
            var name = values.name
            if (status == '登出') {
              return (
                '<div "><img  src="../images/logout.png"></img></div>' +
                '<div"><div><span style="color:#B8B8B8">分机:' +
                name +
                '</span></div>' +
                '<div><span style="color:#B8B8B8">状态:登出</span></div>' +
                '<span style="color:#B8B8B8;width:100%">状态开始:' +
                startTime +
                '</span></div>'
              )
            } else if (state != '待机') {
              return (
                '<div "><img  src="../images/tlak.png"></img></div>' +
                '<div"><div><span style="color:#FF8A65">分机:' +
                name +
                '</span></div>' +
                '<span style="color:#FF8A65">状态:忙碌</span>' +
                '<span style="color:#FF8A65">状态开始:' +
                startTime +
                '</span></div>'
              )
            } else if (status == '就绪' || status == '工作就绪') {
              return (
                '<div "><img  src="../images/login.png"></img></div>' +
                '<div"><div><span style="color:#4DB6AC">分机:' +
                name +
                '</span></div>' +
                '<div><span style="color:#4DB6AC">状态:空闲</span></div>' +
                '<span style="color:#4DB6AC">状态开始:' +
                startTime +
                '</span></div>'
              )
            } else if (status == '未就绪' || status == '后处理') {
              return (
                '<div "><img  src="../images/afterwork.png"></img></div>' +
                '<div"><div><span style="color:#FFB74D">分机:' +
                name +
                '</span></div>' +
                '<div><span style="color:#FFB74D">状态:事务处理</span></div>' +
                '<span style="color:#FFB74D">状态开始:' +
                startTime +
                '</span></div>'
              )
            }
          },
        }
      ]
    }
  ]
})

