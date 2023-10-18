Ext.define('Tab.view.setting.groupSettingView', {
  extend: 'Ext.grid.Panel',
  xtype: 'groupSettingView',

  requires: [
    'Tab.view.setting.groupSettingViewController',
    'Tab.view.setting.groupSettingViewModel',
    'Tab.view.setting.GroupSettingStore'
  ],
  controller: 'setting-groupsettingview',
  viewModel: {
    type: 'setting-groupsettingview'
  },
  selModel: Ext.create('Ext.selection.CheckboxModel', {
    mode: 'SIMPLE',
    checkOnly: true,
    hidden: true,
    //mode: 'single' //默认多选  single是单选
  }),
  store: Ext.create('Tab.view.setting.GroupSettingStore'),
  listeners: {},
  // enableDragDrop: true,
  tbar: {
    items: [
      {
        text: 'Scheduled restart',
        handler: function () {
          var grid = this.up('gridpanel')
          Ext.create('Ext.window.Window', {
            title: 'Scheduled Restart',
            width: 320,
            height: 150,
            resizable: false,
            bodyPadding: '5 0 0 5',
            items: [
              {
                xtype: 'form',
                defaults: {},
                anchor: '100%',
                fieldDefaults: {
                  labelAlign: 'left',
                  flex: 1,
                  margin: 5
                },
                items: [
                  {
                    xtype: 'container',
                    layout: 'vbox',
                    items: [
                      {
                        xtype: 'datetimetowfield',
                        width: 310,
                        fieldLabel: 'StartTime',
                        name: 'starttime',
                        labelWidth: 60,
                        timeCfg: {
                          value: '23:59'
                        }
                      }
                    ]
                  }
                ]
              }
            ],
            buttonAlign: 'right',
            buttons: [
              {
                xtype: 'button',
                text: 'Setting',
                margin: '0 31 0 0',
                handler: function () {
                  var time = this.up()
                    .up()
                    .down('datetimetowfield[name="starttime"]')
                    .getValue()
                  Ext.Ajax.request({
                    url: '/tab/call/RestartThread',
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    params: {
                      starttime: time
                    },
                    failure: function (response, opts) {
                      console.log(
                        'server-side failure with status code ' +
                          response.status
                      )
                    },
                    success: function (response, opts) {}
                  })
                  this.up('window').close()
                }
              }
            ]
          }).show()
        }
      },
      {
        text: 'Query Group Name',
        handler: function () {
          var grid = this.up('gridpanel')
          Ext.create('Ext.window.Window', {
            title: 'Query Group Name',
            width: 300,
            height: 200,
            resizable: false,
            bodyPadding: '5 0 0 5',
            items: [
              {
                xtype: 'form',
                defaults: {},
                anchor: '100%',
                fieldDefaults: {
                  labelAlign: 'left',
                  flex: 1,
                  margin: 5
                },
                items: [
                  {
                    xtype: 'container',
                    layout: 'vbox',
                    items: [
                      {
                        xtype: 'textfield',
                        width: 200,
                        emptyText: 'Enter query criteria',
                        name: 'groupname',
                        fieldLabel: 'Group Name Keyword',
                        labelWidth: 80,
                        allowBlank: true
                      },     {
                        xtype: 'combobox',
                        width: 200,
                        name:'departmentname',
                        forceSelection: true,
                        fieldLabel: 'Department',
                        value: '大众',
                        emptyText: 'Volkswagen',
                        labelWidth: 80,
                        editable: true,
                        triggerAction: 'all',
                        valueField: 'value',
                        displayField: 'value',
                        store: Ext.create('Ext.data.Store', {
                          fields: ['value'],
                          data: [['Volkswagen'], ['Audi'],['All']]
                        }),
                        queryMode: 'local'
                      }
                    ]
                  }
                ]
              }
            ],
            buttonAlign: 'right',
            buttons: [
              {
                xtype: 'button',
                text: 'Query',
                margin: '0 31 0 0',
                handler: function () {
                  var groupname = this.up()
                    .up()
                    .down('textfield[name="groupname"]')
                    .getValue()
                    var department = this.up()
                    .up()
                    .down('combobox[name="departmentname"]')
                    .getValue()
                  if (groupname == null || groupname.length == '') {
                    grid.store.proxy.extraParams = {
                      condition: '',
                      department:department
                    }
                    grid.store.loadPage(1)
                  } else {
                    grid.store.proxy.extraParams = {
                      condition: groupname,
                      department:department
                    }
                    grid.store.loadPage(1)
                  }
                  this.up('window').close()
                }
              }
            ]
          }).show()
        }
      },
      {
        text: 'Clean&Fresh',
        handler: function () {
          var grid = this.up('gridpanel')
          //   alert( grid.getStore().getAt(0).get("number"))
          //   alert( grid.getStore().getAt(1).get("number"))
          grid.store.proxy.extraParams = {
            condition: ''
          }
          grid.store.loadPage(1)
        }
      },
      {
        text: 'Delete Group',
        handler: function () {
          var grid = this.up('gridpanel')
          var store = grid.store;
          selectRecords = grid.getSelection();
          var VWarray=new Array();
          var ADarray=new Array();
          var GroupNum=new Array();
          var SplitList=new Array();
          if(selectRecords.length>0){
            for(var i=0;i<selectRecords.length;i++){
              var record=selectRecords[i]
              var department=record.get('department')
              var extension=record.get('skillGroup')
              var num=record.get("number")
              GroupNum.push(num)
              SplitList.push(extension)
              if(department=='大众'){
                VWarray.push(extension)
              }else if(department=='奥迪'){
                ADarray.push(extension)
              }
            }
          }
          Ext.Ajax.request({
            url: '/tab/call/DeleteGroup',
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            params: {
              VWarray: VWarray,
              ADarray: ADarray,
              SplitList:SplitList,
              GroupNum:GroupNum
            },
            failure: function (response, opts) {
              var obj = Ext.decode(response.responseText)
              console.log(
                'server-side failure with status code ' + response.status
              )
            },
            success: function (response, opts) {
              var obj = Ext.decode(response.responseText)
              if(obj.success){
                Ext.Array.each(selectRecords, function(record) {
                  store.remove(record);
               })
              }
            }
          })
        }
      },
      {
        text: 'Add Group',
        handler: function () {
          var grid = this.up().up()
          Ext.create('Ext.window.Window', {
            title: 'Add Group',
            width: 300,
            height: 220,
            resizable: false,
            bodyPadding: '5 0 0 5',
            items: [
              {
                xtype: 'form',
                defaults: {
                  anchor: '100%'
                },
                fieldDefaults: {
                  labelAlign: 'left',
                  flex: 1,
                  margin: 5
                },
                items: [
                  {
                    xtype: 'container',
                    layout: 'vbox',
                    items: [
                      {
                        xtype: 'numberfield',
                        width: 250,
                        emptyText: 'Please enter the group number',
                        name: 'vdn',
                        fieldLabel: 'Skill Group',
                        labelWidth: 60,
                        allowBlank: false
                      },
                      {
                        xtype: 'textfield',
                        width: 250,
                        emptyText: 'Please enter the group name',
                        name: 'groupname',
                        fieldLabel: 'Group Nmae',
                        labelWidth: 60,
                        allowBlank: false
                      },
                      {
                        xtype: 'combobox',
                        width: 250,
                        name: 'departmentname',
                        forceSelection: true,
                        fieldLabel: 'Department',
                        value: 'Volkswagen',
                        emptyText: 'Volkswagen',
                        labelWidth: 60,
                        editable: true,
                        triggerAction: 'all',
                        valueField: 'value',
                        displayField: 'value',
                        store: Ext.create('Ext.data.Store', {
                          fields: ['value'],
                          data: [['Volkswagen'], ['Audi']]
                        }),
                        queryMode: 'local'
                      }
                    ]
                  }
                ]
              }
            ],
            buttonAlign: 'right',
            buttons: [
              {
                xtype: 'button',
                text: 'Confirm',
                margin: '0 31 0 0',
                handler: function () {
                  var mee = this
                  var vdn = this.up()
                    .up()
                    .down('numberfield[name="vdn"]')
                    .getValue()
                  var groupname = this.up()
                    .up()
                    .down('textfield[name="groupname"]')
                    .getValue()
                  var department = this.up()
                    .up()
                    .down('combobox[name="departmentname"]')
                    .getValue()
                    if(vdn!=null&&vdn>0&&groupname!=null&&groupname.length>0){
                      mee.up('window').close()
                      Ext.Ajax.request({
                        url: '/tab/call/AddGroup',
                        method: 'POST',
                        headers: {
                          'Content-Type':
                            'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params: {
                          vdn: vdn,
                          groupname:groupname,
                          department:department
                        },
                        failure: function (response, opts) {
                          console.log(
                            'server-side failure with status code ' +
                              response.status
                          )
                        },
                        success: function (response, opts) {
                          var obj = Ext.decode(response.responseText)
                          if (obj.success) {
                            grid.store.proxy.extraParams = {
                              condition: ''
                            }
                            grid.store.loadPage(1)
                          } else {
                            Ext.Msg.alert('Add failed', 'Please add again')
                          }
                        }
                      })
                    }else{
                      Ext.Msg.alert('Tips', 'Please enter the group name and queue number')
                    }
                }
              }
            ]
          }).show()
        }
      }
      // {
      //   text: '上移',
      //   handler: function () {
      //     var grid = this.up('gridpanel')
      //     var store = grid.store;
      //     selectRecords = grid.getSelection();
      //     if(selectRecords.length>0){
      //       var record=selectRecords[0]
      //       var rowid = store.indexOf(record);
      //       if(rowid==0){
      //         Ext.Msg.alert('提示', "已经是首行")
      //       }else{
      //         var fskillgroup=record.get('skillGroup');
      //       var fnumber=record.get('number');
      //       var fvalue=record.get('value');
      //       var lastrecord=store.getAt(rowid-1);
      //       var tskillgroup=lastrecord.get('skillGroup');
      //       var tvalue=lastrecord.get('value');
      //       var tnumber=lastrecord.get('number')
      //       Ext.Ajax.request({
      //         url: '/tab/call/DragGroupRow',
      //         method: 'POST',
      //         headers: {
      //           'Content-Type': 'application/x-www-form-urlencoded'
      //         },
      //         params: {
      //           fskillgroup:fskillgroup,
      //           fnumber:fnumber,
      //           fvalue:fvalue,
      //           tskillgroup:tskillgroup,
      //           tvalue:tvalue,
      //           tnumber:tnumber
      //         },
      //         failure: function (response, opts) {
      //           console.log(
      //             'server-side failure with status code ' +
      //               response.status
      //           )
      //         },
      //         success: function (response, opts) {
      //           grid.store.clearFilter(true)
      //           grid.getStore().load()
      //         }
      //       })
      //       }
      //     }else{
      //       Ext.Msg.alert('提示', "请选择一行")
      //     }
      //   }
      // },
      //   {
      //     text: '下移',
      //  handler: function () {
      //       var grid = this.up('gridpanel')
      //       var store = grid.store;
      //       selectRecords = grid.getSelection();
      //       if(selectRecords.length>0){
      //         var record=selectRecords[0]
      //         var rowid = store.indexOf(record);
      //       var cunnt=store.getCount();
      //       if(rowid==(cunnt-1)){
      //           Ext.Msg.alert('提示', "已经是末行")
      //         }else{
      //           var fskillgroup=record.get('skillGroup');
      //         var fnumber=record.get('number');
      //         var fvalue=record.get('value');
      //         var lastrecord=store.getAt(rowid+1);
      //         var tskillgroup=lastrecord.get('skillGroup');
      //         var tvalue=lastrecord.get('value');
      //         var tnumber=lastrecord.get('number')
      //         Ext.Ajax.request({
      //           url: '/tab/call/DragGroupRow',
      //           method: 'POST',
      //           headers: {
      //             'Content-Type': 'application/x-www-form-urlencoded'
      //           },
      //           params: {
      //             fskillgroup:fskillgroup,
      //             fnumber:fnumber,
      //             fvalue:fvalue,
      //             tskillgroup:tskillgroup,
      //             tvalue:tvalue,
      //             tnumber:tnumber
      //           },
      //           failure: function (response, opts) {
      //             console.log(
      //               'server-side failure with status code ' +
      //                 response.status
      //             )
      //           },
      //           success: function (response, opts) {
      //             grid.store.clearFilter(true)
      //             grid.getStore().load()
      //           }
      //         })
      //         }
      //       }else{
      //         Ext.Msg.alert('提示', "请选择一行")
      //       }

      //     }
      //   },
    ]
  },
  viewConfig: {
    loadMask: true,
    enableTextSelection: true
    // plugins: {
    //     ptype: "gridviewdragdrop",
    //     dragText: "可用鼠标拖拽进行上下排序"
    // }
  },
  dockedItems: [
    {
      xtype: 'pagingtoolbar',
      dock: 'bottom',
      pageSize: 10,
      displayInfo: true,
      displayMsg: '{0} to {1} of {2}',
      emptyMsg: 'No record',
      listeners: {
        beforerender: function (bgbar) {
          bgbar.add('-')
          bgbar.add(
            new Ext.form.NumberField({
              minWidth: 90,
              maxWidth: 90,
              minValue: 1,
              maxValue: 1000,
              step: 10,
              value: bgbar.up().store.pageSize,
              listeners: {
                change: function (field, newValue) {
                  bgbar.pageSize = newValue
                  bgbar.up().store.pageSize = newValue
                }
              }
            })
          )
          bgbar.add('Records/Page')
          bgbar.setStore(bgbar.up().store)
        }
      }
    }
  ],
  plugins: [
    Ext.create('Ext.grid.plugin.CellEditing', {
      clicksToEdit: 1,
      listeners: {
        edit: function (me, e) {
          //  alert(e.record.get(e.field))
          // alert(e.originalValue)
          if (!(e.record.get(e.field) == e.originalValue)) {
            var rec = e.record.get(e.field)
            var skillgroup = e.record.get('skillGroup')
            // if (rec == '') {
            //   rec = null
            // }
            Ext.Ajax.request({
              url: '/tab/call/EditGroupName',
              method: 'POST',
              headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
              },
              params: {
                skillgroup: skillgroup,
                groupname: rec
              },
              failure: function (response, opts) {
                var obj = Ext.decode(response.responseText)
                console.log(
                  'server-side failure with status code ' + response.status
                )
              },
              success: function (response, opts) {
                // var obj = Ext.decode(response.responseText)
                // me.grid.store.load()
              }
            })
          }
        }
      }
    })
  ],
  columns: [
    {
      xtype: 'rownumberer',
      width: 35,
      align: 'center'
    },
    {
      header: 'Group Number',
      flex: 1,
      hidden: true,
      dataIndex: 'number'
    },
    {
      header: 'Skill Group',
      flex: 1,
      dataIndex: 'skillGroup',
      menuDisabled: true,
      align: 'center'
    },
    {
      header: 'Group Name',
      flex: 1,
      dataIndex: 'value',
      menuDisabled: true,
      align: 'center',
      editor: {
        xtype: 'textfield'
      }
    },
    {
      header: 'Department',
      flex: 1,
      dataIndex: 'department',
      menuDisabled: true,
      align: 'center'
    }
  ]
})
