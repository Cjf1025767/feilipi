
Ext.define('Tab.view.main.batchView', {
    extend: 'Ext.panel.Panel',
    xtype: 'batchView',

    requires: [
        'Tab.view.main.batchViewController',
        'Tab.view.main.batchViewModel',
        'Ext.form.Panel'
    ],

    controller: 'main-batchview',
    viewModel: {
        type: 'main-batchview'
    },

    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [
        {
            xtype: 'gridpanel',
            name: 'batch',
            flex: 1,
            minWidth: 240,
            border: 1,
            scrollable: true,
            listeners: {
                afterrender: 'onAfterrenderBatchList',
                cellclick: 'onBatchClick',
                edit: 'onBatchEdit'
            },
            store: Ext.create('Tab.view.main.batchStore'),
            tbar: {
                items: [{
                    xtype: 'iconlabel',
                    iconCls: 'x-fa fa-file-excel-o',
                    text: ' 批次'
                }, '->',
                {
                    text: '添加',
                    width: 100,
                    handler: function (btn) {
                        Ext.create("Ext.window.Window", {
                            title: "导入批次",
                            width: 400,
                            height: 430,
                            modal: true,
                            resizable: false,
                            bodyPadding: '5 0 0 5',
                            gridstore: btn.up('gridpanel').store,
                            controller: 'main-batchview',//和父窗口共享controller
                            items: [
                                {
                                    xtype: "form",
                                    defaults: {
                                        anchor: '100%'
                                    },
                                    fieldDefaults: {
                                        labelAlign: "left",
                                        flex: 1,
                                        margin: 5
                                    },
                                    items: [
                                        {
                                            xtype: "fieldcontainer",
                                            layout: "vbox",
                                            items: [
                                                { xtype: 'hiddenfield', name: 'fid' },
                                                { xtype: "textfield", width: '100%', name: "name", fieldLabel: "名称", labelWidth: 40, allowBlank: false },
                                                { xtype: "textfield", width: '100%', name: "description", fieldLabel: "描述", labelWidth: 40 },
                                                {
                                                    xtype: 'filefield',
                                                    fieldLabel: 'Excel',
                                                    labelWidth: 40,
                                                    width: '100%',
                                                    name: 'filename',
                                                    buttonText: '',

                                                    buttonConfig: {
                                                        iconCls: 'fa-file-excel-o'
                                                    },
                                                    validator: function (value) {
                                                        var arrType = value.split('.');
                                                        var docType = arrType[arrType.length - 1].toLowerCase();
                                                        if (docType == 'xlsx') {
                                                            return true;
                                                        } if (docType == 'xls') {
                                                            return true;
                                                        }
                                                        return '文件类型必须为Excel';
                                                    }
                                                },
                                                {
                                                    xtype: "fieldcontainer",
                                                    layout: "hbox",
                                                    items: [
                                                        { xtype: "textfield", width: 190, name: "row", fieldLabel: "预览中选择开始行", labelWidth: 125, allowBlank: false, value: '0' },
                                                        { xtype: "textfield", width: 130, name: "col", fieldLabel: "导入列", labelWidth: 60, allowBlank: false, value: '0' }
                                                    ]
                                                },
                                                {
                                                    xtype: 'gridpanel',
                                                    name: 'phone',
                                                    width: '100%',
                                                    border: 1,
                                                    height: 150,
                                                    scrollable: true,
                                                    store: Ext.create('Ext.data.Store', {
                                                        fields: ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"]
                                                    }),
                                                    selModel: 'cellmodel',
                                                    listeners: {
                                                        cellclick: 'onExcelClick'
                                                    },
                                                    columns: [
                                                        { text: 'A', dataIndex: 'A', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'B', dataIndex: 'B', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'C', dataIndex: 'C', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'D', dataIndex: 'D', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'E', dataIndex: 'E', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'F', dataIndex: 'F', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'G', dataIndex: 'G', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'H', dataIndex: 'H', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'I', dataIndex: 'I', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'J', dataIndex: 'J', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'K', dataIndex: 'K', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'L', dataIndex: 'L', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'M', dataIndex: 'M', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'N', dataIndex: 'N', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'O', dataIndex: 'O', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'P', dataIndex: 'P', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'Q', dataIndex: 'Q', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'R', dataIndex: 'R', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'S', dataIndex: 'S', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'T', dataIndex: 'T', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'U', dataIndex: 'U', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'V', dataIndex: 'V', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'W', dataIndex: 'W', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'X', dataIndex: 'X', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'Y', dataIndex: 'Y', width: 60, menuDisabled: true, sortable: false },
                                                        { text: 'Z', dataIndex: 'Z', width: 60, menuDisabled: true, sortable: false }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }],
                            buttonAlign: 'center',
                            buttons: [
                                {
                                    xtype: "button",
                                    text: "确定",
                                    handler: 'onAddBatchConfirm'
                                }, {
                                    xtype: "button",
                                    text: "预览",
                                    handler: 'onPreviewBatch'
                                }
                            ]
                        }).show();
                    }
                },
                {
                    xtype: 'button',
                    text: '刷 新',
                    width: 100,
                    handler: function () {
                        var view = this.up().up();
                        view.store.load();
                    }
                }]
            },
            plugins: [{
                ptype: 'cellediting',
                clicksToEdit: 2,
            }],
            columns: [
                {
                    text: '名称', dataIndex: 'name',
                    dirtyText: '名称已编辑',
                    width: 200,
                    editor: {
                        allowBlank: true
                    }
                },
                {
                    text: '创建时间', dataIndex: 'createdate', width: 150, renderer: function (v) {
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                    }
                },
                {
                    text: '更新时间', dataIndex: 'updatedate', width: 150, renderer: function (v) {
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                    }
                },
                {
                    text: '描述', dataIndex: 'description',
                    dirtyText: '描述已编辑',
                    flex: 1,
                    editor: {
                        allowBlank: true
                    }
                }, {
                    xtype: 'checkcolumn', text: '激活', dataIndex: 'activate'
                },
                {
                    xtype: 'actioncolumn',
                    align: 'center',
                    text: '删除',
                    tooltip: '删除',
                    width: 50,
                    menuDisabled: true,
                    items: [{
                        iconCls: 'x-fa fa-trash',
                        tooltip: '删除',
                        handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                            var rs = record;
                            var store = view.getStore();
                            var selModel = view.getSelectionModel();
                            var phonegrid = view.up().up().down('gridpanel[name=phone]');
                            Ext.MessageBox.confirm("删除", "请确认删除",
                            function (btn, value) {
                                if (btn == 'yes') {
                                    Ext.Ajax.request({
                                        url : '/tab/call/RemoveBatch',
                                        method : 'POST',
                                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                        params: {
                                            id:rs.get('id')
                                        },
                                        success: function(response, options){
                                            var obj = Ext.decode(response.responseText);
                                            if(obj.success){
                                                store.remove(rs);
                                                if (store.getCount() > 0) {
                                                    selModel.select(0);
                                                }
                                                phonegrid.store.removeAll();
                                            }else{
                                                Ext.Msg.alert("错误",obj.msg);
                                            }
                                        },failure: function(response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        }
                                    });
                                }
                            });
                        }
                    }]
                }
            ],
            dockedItems: [{
                xtype: 'pagingtoolbar',
                dock: 'bottom',
                pageSize: 10,
                displayInfo: true,
                displayMsg: '显示第{0}条到{1}条数据,一共{2}条',
                emptyMsg: '没有记录',
                listeners: {
                    beforerender: function (bgbar) {
                        bgbar.add('-');
                        bgbar.add(
                            new Ext.form.NumberField({
                                minWidth: 90, maxWidth: 90, minValue: 1, maxValue: 1000, step: 10,
                                value: bgbar.up().store.pageSize,
                                listeners: {
                                    'change': function (field, newValue) {
                                        bgbar.pageSize = newValue;
                                        bgbar.up().store.pageSize = newValue;
                                    }
                                }
                            }));
                        bgbar.add('条/页');
                        bgbar.setStore(bgbar.up().store);
                    }
                }
            }]
        },
        {
            xtype: 'gridpanel',
            name: 'phone',
            width: 360,
            border: 1,
            scrollable: true,
            store: Ext.create('Tab.view.main.batchPhoneStore'),
            tbar: {
                items: [{
                    xtype: "textfield", width: '100', name: "phone", fieldLabel: "电话", labelWidth: 40
                }, {
                    xtype: 'button', text: '查询', handler: 'onBatchPhone'
                }
                ]
            },
            columns: [
                {
                    text: '电话', dataIndex: 'phone',
                    flex: 1
                }
            ],
            dockedItems: [{
                xtype: 'pagingtoolbar',
                dock: 'bottom',
                pageSize: 10,
                displayInfo: true,
                displayMsg: '显示第{0}条到{1}条数据,一共{2}条',
                emptyMsg: '没有记录',
                listeners: {
                    beforerender: function (bgbar) {
                        bgbar.add('-');
                        bgbar.add(
                            new Ext.form.NumberField({
                                minWidth: 90, maxWidth: 90, minValue: 1, maxValue: 1000, step: 10,
                                value: bgbar.up().store.pageSize,
                                listeners: {
                                    'change': function (field, newValue) {
                                        bgbar.pageSize = newValue;
                                        bgbar.up().store.pageSize = newValue;
                                    }
                                }
                            }));
                        bgbar.add('条/页');
                        bgbar.setStore(bgbar.up().store);
                    }
                }
            }]
        },
    ]
});
