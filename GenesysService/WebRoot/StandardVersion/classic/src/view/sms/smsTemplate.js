Ext.define('Tab.view.sms.smsTemplate', {
    extend: 'Ext.panel.Panel',
    xtype: 'smsTemplate',

    requires: [
        'Tab.view.sms.smsTemplateController',
        'Tab.view.sms.smsTemplateModel',
        'Ext.grid.Panel'
    ],

    controller: 'sms-smstemplate',
    viewModel: {
        type: 'sms-smstemplate'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'gridpanel',
        width: '100%',
        border: 1,
        tbar: {
            autoScroll: true,
            items: [{
                    xtype: 'iconlabel',
                    iconCls: 'x-fa fa-file-powerpoint-o',
                    text: ' 模板'
                }, '->',
                {
                    xtype: 'button',
                    text: '添 加',
                    width: 100,
                    handler: 'onAddOrModifySmsTemplate'
                },
                {
                    xtype: 'button',
                    text: '同 步',
                    width: 100,
                    handler: function () {
                        var view = this.up().up();
                        view.store.load({
                            params: {
                                sync: true
                            },
                            callback: function (records, operation, success) {
                                if (records.length == 0) {
                                    Ext.Msg.show({
                                        title: "提示",
                                        message: "运营商服务器上已经存在的模板，需要使用'获取'后才能同步! <br />是否立即获取模板?",
                                        buttons: Ext.Msg.YESNO,
                                        icon: Ext.Msg.QUESTION,
                                        fn: function (btn) {
                                            if (btn === 'yes') {
                                                view.down('#gettemplate').fireEvent('click', view.down('#gettemplate'));
                                            } else if (btn === 'no') {

                                            } else {
                                                console.log('Cancel pressed');
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                },
                {
                    xtype: 'button',
                    itemId: 'gettemplate',
                    text: '获 取',
                    width: 100,
                    listeners: {
                        'click': function () {
                            var grid = this.up().up();
                            Ext.Msg.prompt('模板标识', '请输入模板标识:', function (btn, text) {
                                if (btn == 'ok') {
                                    Ext.Ajax.request({
                                        url: '/tab/call/GetSmsTemplate',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            templatecode: text
                                        },
                                        success: function (response, options) {
                                            var obj = Ext.decode(response.responseText);
                                            if (obj.success) {
                                                grid.store.insert(0, obj.item);
                                            }
                                        },
                                        failure: function (response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        }
                                    });
                                }
                            });
                        }
                    }
                }
            ]
        },
        listeners: {
            'afterrender': function () {
                var me = this;
                me.store.load()
            }
        },
        store: Ext.create('Tab.view.sms.templateStore'),
        columns: [{
                text: '名称',
                dataIndex: 'templatecode',
                width: 150
            },
            {
                text: '更新时间',
                dataIndex: 'updatedate',
                width: 150,
                renderer: function (v) {
                    return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                }
            },
            {
                text: '内容',
                dataIndex: 'templatecontent',
                flex: 2
            },
            {
                text: '类型',
                dataIndex: 'type',
                width: 80
            },
            {
                text: '状态',
                dataIndex: 'status',
                flex: 1
            },
            {
                text: '更新人',
                dataIndex: 'updatername'
            },
            {
                xtype: 'actioncolumn',
                align: 'center',
                text: '删除',
                tooltip: '删除',
                width: 50,
                menuDisabled: true,
                items: [{
                    tooltip: '修改',
                    iconCls: 'x-fa fa-file-text-o',
                    handler: 'onAddOrModifySmsTemplate'
                }, {
                    iconCls: 'x-fa fa-trash',
                    tooltip: '删除',
                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                        var rs = record;
                        var selModel = view.getSelectionModel();
                        var store = view.getStore();
                        Ext.Msg.show({
                            title: "删除",
                            message: "是否从运营商服务器删除模板?",
                            buttons: Ext.Msg.YESNOCANCEL,
                            icon: Ext.Msg.QUESTION,
                            fn: function (btn) {
                                if (btn === 'yes' || btn === 'no') {
                                    Ext.Ajax.request({
                                        url: '/tab/call/RemoveSmsTemplate',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            templatecode: rs.get('templatecode'),
                                            sync: ((btn === 'yes') ? true : false)
                                        },
                                        success: function (response, options) {
                                            var obj = Ext.decode(response.responseText);
                                            if (obj.success) {
                                                store.remove(rs);
                                                if (store.getCount() > 0) {
                                                    selModel.select(0);
                                                }
                                            } else {
                                                store.reload();
                                                Ext.Msg.alert("错误", obj.msg);
                                            }
                                        },
                                        failure: function (response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        }
                                    });
                                }
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
                            minWidth: 90,
                            maxWidth: 90,
                            minValue: 1,
                            maxValue: 1000,
                            step: 10,
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
        }],
        viewConfig: {
            loadMask: true,
            enableTextSelection: true
        }
    }]
});