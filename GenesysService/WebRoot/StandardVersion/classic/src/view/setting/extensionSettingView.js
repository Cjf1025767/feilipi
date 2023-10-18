// Ext.define('Tab.view.setting.extensionSettingView', {
//     extend: 'Ext.grid.Panel',
//     xtype: 'extensionSettingView',

//     requires: [
//         'Tab.view.setting.extensionSettingViewController',
//         'Tab.view.setting.extensionSettingViewModel',
//         'Tab.view.setting.ExtensionSettingStore'
//     ],

//     controller: 'setting-extensionsettingview',
//     viewModel: {
//         type: 'setting-extensionsettingview'
//     },
//     store: Ext.create('Tab.view.setting.ExtensionSettingStore'),
//     listeners: {},
//     tbar: {},
//     viewConfig: {
//         loadMask: true,
//         enableTextSelection: true
//     }
// });
Ext.define('Tab.view.setting.extensionSettingView', {
  extend: 'Ext.container.Container',
  xtype: 'extensionSettingView',
  requires: [
    'Ext.tab.Panel',
    'Tab.view.setting.extensionSettingViewController',
    'Tab.view.setting.extensionSettingViewModel',
    'Tab.view.setting.ExtensionSettingStore',
    'Ext.grid.CellEditor',
    'Tab.view.setting.groupSettingView',
    'Ext.Component'
  ],
  layout: {
    type: 'hbox',
    align: 'stretch',
    pack: 'start'
  },
  listeners: {
    beforerender: function (view) {
      var exstore = view.down('gridpanel[name="修改分机"]').getStore()
      exstore.load()
      var grstore = view.down('gridpanel[name="分组设置"]').getStore()
      grstore.load()
    }
  },
  items: [
    {
      xtype: 'gridpanel',
      margin: '0 10 0 0',
      name: '修改分机',
      title: 'Extension Setting',
      flex: 1,
      controller: 'setting-extensionsettingview',
      viewModel: {
        type: 'setting-extensionsettingview'
      },
      selModel: Ext.create('Ext.selection.CheckboxModel', {
        mode: 'SIMPLE',
        checkOnly: true,
        hidden: true,
      }),
      viewConfig: {
        // columnsText: '显示的列',//显示的列所定义名字
        // scrollOffset: 30,//留出的滚动条宽度
        // sortAscText: '升序',
        loadMask: true,
        enableTextSelection: true
      },
      store: Ext.create('Tab.view.setting.ExtensionSettingStore'),
      tbar: {
        items: [
          {
            text: 'Add extension',
            handler: function () {
              var grid = this.up().up()
              Ext.create('Ext.window.Window', {
                title: 'Add Extension',
                width: 480,
                height: 180,
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
                            xtype: 'textfield',
                            width: 450,
                            emptyText:
                              'Please enter a format such as 123456 or 123546, 123457 or 100000-110000',
                            name: 'extensionname',
                            fieldLabel: 'Extension Name',
                            labelWidth: 60,
                            allowBlank: false
                          },
                          {
                            xtype: 'combobox',
                            width: 180,
                            name:'departmentname',
                            forceSelection: true,
                            fieldLabel: 'Department',
                            value: '大众',
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
                      var extension = this.up()
                        .up()
                        .down('textfield[name="extensionname"]')
                        .getValue()
                        var department = this.up()
                        .up()
                        .down('combobox[name="departmentname"]')
                        .getValue()
                      var extensions = extension
                      var extensiongroup = new Array()
                      //判断输入的分机号格式
                      if (
                        typeof extension != 'undefined' &&
                        extension.length > 0
                      ) {
                        var rng = /^[0-9,-]*[0-9]{1,6}$/
                        var rrr = /^[0-9]+$/
                        if (rng.test(extensions) || rrr.test(extensions)) {
                          var rrg = /,/
                          var rgg = /-/
                          //多个分机号逗号隔开格式
                          if (rrg.test(extensions)) {
                            extensiongroup = extension.toString().split(',')
                            var newgroup = new Array()
                            for (var i = 0; i < extensiongroup.length; i++) {
                              var newstr = extensiongroup[i]
                              if (rgg.test(newstr)) {
                                //处理1000-2000,2005-2060这种情况
                                var newarr = newstr.split('-')
                                if (newarr.length == 2) {
                                  for (var j = newarr[0]; j <= newarr[1]; j++) {
                                    newgroup.push(j)
                                  }
                                }
                              } else {
                                newgroup.push(newstr)
                              }
                            }
                            extensiongroup = newgroup.slice(0)
                          }
                          //连续分机号格式
                          else if (rgg.test(extensions)) {
                            var firstextension = extension
                              .toString()
                              .split('-')[0]
                            var secondextension = extension
                              .toString()
                              .split('-')[1]
                            if (firstextension > 0) {
                              for (
                                var i = firstextension;
                                i <= secondextension;
                                i++
                              ) {
                                extensiongroup.push(i)
                              }
                            } else {
                              Ext.Msg.alert('Error', 'Please enter the correct extension format')
                            }
                          }
                          //单个分机号格式
                          else {
                            extensiongroup.push(extension)
                          }
                          Ext.Ajax.request({
                            url: '/tab/call/AddExtension',
                            method: 'POST',
                            headers: {
                              'Content-Type':
                                'application/x-www-form-urlencoded'
                            },
                            async: true,
                            params: {
                              extensionList: extensiongroup,
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
                                mee.up('window').close()
                                grid.store.proxy.extraParams = {
                                  condition: ''
                                }
                                grid.store.loadPage(1)
                                //grid.store.load()
                              } else {
                                Ext.Msg.alert('Error', 'Please enter again')
                                mee
                                  .up('window')
                                  .down('textfield[name="extensionname"]')
                                  .setValue('')
                              }
                            }
                          })
                        } else {
                          Ext.Msg.alert('Error', 'Please enter the correct extension format')
                          this.up()
                            .up()
                            .down('textfield[name="extensionname"]')
                            .setValue('')
                        }
                      } else {
                        Ext.Msg.alert('Error', 'Extension number and department cannot be blank')
                        this.up()
                          .up()
                          .down('textfield[name="extensionname"]')
                          .setValue('')
                      }
                    }
                  }
                ]
              }).show()
            }
          },
          {
            text: 'Query Extension',
            handler: function () {
              var grid = this.up('gridpanel')
              Ext.create('Ext.window.Window', {
                title: 'Query Extension',
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
                            width: 325,
                            emptyText: 'Enter query criteria',
                            name: 'extensionname',
                            fieldLabel: 'Extension keyword',
                            labelWidth: 80,
                            allowBlank: true
                          },
                          {
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
                      var extension = this.up()
                        .up()
                        .down('textfield[name="extensionname"]')
                        .getValue()
                        var department=  this.up()
                        .up()
                        .down('combobox[name="departmentname"]')
                        .getValue()
                        grid.store.proxy.extraParams = {
                          condition: extension,
                          department:department
                        }
                        grid.store.loadPage(1)
                      // if (extension == null || extension.length == '') {
                      //   grid.store.proxy.extraParams = {
                      //     condition: ''
                      //   }
                      //   grid.store.loadPage(1)
                      // } else {
                       
                      // }
                      this.up('window').close()
                    }
                  }
                ]
              }).show()
            }
          },
          {
            text: 'Delete Extension',
            handler: function () {
              var grid = this.up('gridpanel')
              var store = grid.store;
              selectRecords = grid.getSelection();
              var VWarray=new Array();
              var ADarray=new Array();
              var dearray=new Array();
              if(selectRecords.length>0){
                for(var i=0;i<selectRecords.length;i++){
                  var record=selectRecords[i]
                  var department=record.get('department')
                  var extension=record.get('extension')
                  dearray.push(extension)
                  if(department=='大众'){
                    VWarray.push(extension)
                  }else if(department=='奥迪'){
                    ADarray.push(extension)
                  }
                }
              }
              Ext.Ajax.request({
                url: '/tab/call/DeleteExtension',
                method: 'POST',
                headers: {
                  'Content-Type': 'application/x-www-form-urlencoded'
                },
                params: {
                  VWarray: VWarray,
                  ADarray: ADarray,
                  DeviceList:dearray
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
            text: 'Clean&Fresh',
            handler: function () {
              var grid = this.up('gridpanel')
              grid.store.proxy.extraParams = {
                condition: ''
              }
              grid.store.loadPage(1)
            }
          }
        ]
      },
     // plugins: [
        // Ext.create('Ext.grid.plugin.CellEditing', {
        //   clicksToEdit: 1,
        //   listeners: {
        //     edit: function (me, e) {
        //       //  alert(e.record.get(e.field))
        //       // alert(e.originalValue)
        //       if (!(e.record.get(e.field) == e.originalValue)) {
        //         var rec = e.record.get(e.field)
        //         // if (rec == '') {
        //         //   rec = null
        //         // }
        //         Ext.Ajax.request({
        //           url: '/tab/call/EditExtension',
        //           method: 'POST',
        //           headers: {
        //             'Content-Type': 'application/x-www-form-urlencoded'
        //           },
        //           params: {
        //             extension: rec,
        //             oldextension: e.originalValue
        //           },
        //           failure: function (response, opts) {
        //             var obj = Ext.decode(response.responseText)

        //             console.log(
        //               'server-side failure with status code ' + response.status
        //             )
        //           },
        //           success: function (response, opts) {
        //             var obj = Ext.decode(response.responseText)
        //             if (!obj.success) {
        //               me.grid.store.load()
        //             }
        //           }
        //         })
        //       }
        //     }
        //   }
        // })
     // ],
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
      columns: [
        {
          xtype: 'rownumberer',
          width: 35,
          align: 'center'
        },
        {
          header: 'Extension',
          flex: 1,
          dataIndex: 'extension',
          editable: true,
          menuDisabled: true,
          align: 'center'
          // editor: {
          //   xtype: 'numberfield'
          // }
        },
        {
          header: 'Department',
          flex: 1,
          dataIndex: 'department',
          align: 'center'
          // editor: new Ext.form.ComboBox({
          //   editable: false,
          //   forceSelection: true,
          //   valueField :"value",displayField: "value",
          //   store: Ext.create('Ext.data.Store', {
          //     fields: [ 'value'],
          //     data: [
          //       ['奥迪'],
          //       ['大众']
          //     ]
          //   })
          // })
        }
        // {
        //   text: '操作',
        //   width: 120,
        //   menuText: '操作',
        //   align: 'center',
        //   xtype: 'actioncolumn',
        //   items: [
        //     {
        //       iconCls: 'x-fa fa-trash-o',
        //       tooltip: '删除',
        //       margin: '10px',
        //       handler: function (
        //         view,
        //         rowIndex,
        //         cellIndex,
        //         item,
        //         e,
        //         record,
        //         row
        //       ) {
        //         var grid = this.up('gridpanel')
        //         Ext.Msg.confirm('删除', '确定要删除吗?', function (btn) {
        //           if (btn == 'yes') {
        //             var extension = record.get('extension')
        //             var me = this
        //             Ext.Ajax.request({
        //               url: '/tab/call/DeleteExtension',
        //               method: 'POST',
        //               headers: {
        //                 'Content-Type': 'application/x-www-form-urlencoded'
        //               },
        //               params: {
        //                 extension: extension
        //               },
        //               failure: function (response, opts) {
        //                 console.log(
        //                   'server-side failure with status code ' +
        //                     response.status
        //                 )
        //               },
        //               success: function (response, opts) {
        //                 var obj = Ext.decode(response.responseText)
        //                 if (obj.success) {
        //                   // var rowid = store.indexOf(record);
        //                   grid.store.remove(record)
        //                   //  grid.store.load();
        //                 } else {
        //                   Ext.Msg.alert('错误', obj.msg)
        //                 }
        //               }
        //             })
        //           }
        //         })
        //       }
        //     }
        //   ]
        // }
      ]
    },
    {
    	xtype: 'groupSettingView',
    	flex: 1,
    	title: 'Extension Setting',
    	name: '分组设置'
    }
  ]
})
