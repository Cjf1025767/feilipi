Ext.define('Tab.view.sms.smsSign', {
    extend: 'Ext.panel.Panel',
    xtype: 'smsSign',

    requires: [
        'Tab.view.sms.smsSignController',
        'Tab.view.sms.smsSignModel',
        'Ext.grid.Panel'
    ],

    controller: 'sms-smssign',
    viewModel: {
        type: 'sms-smssign'
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
                    iconCls: 'x-fa fa-file-code-o',
                    text: ' 签名'
                }, '->',
                {
                    xtype: 'button',
                    text: '添 加',
                    width: 100,
                    handler: function (btn) {
                        var grid = btn.up('gridpanel');
                        Ext.Msg.prompt('名字', '请输入签名的名字:', function (btn, text) {
                            if (btn == 'ok') {
                                Ext.Ajax.request({
                                    url: '/tab/call/AddOrUpdateSmsSign',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        signname: text
                                    },
                                    success: function (response, options) {
                                        var obj = Ext.decode(response.responseText);
                                        if (obj.success) {
                                            grid.store.insert(0, obj.item);
                                            grid.findPlugin('cellediting').startEditByPosition({
                                                row: 0,
                                                column: 0
                                            });
                                        }
                                    },
                                    failure: function (response, opts) {
                                        console.log('server-side failure with status code ' + response.status);
                                    }
                                });
                            }
                        });
                    }
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
                                    Ext.Msg.alert("提示", "需要添加签名后，才能同步运营商服务器上的签名状态! <br />不存在的签名需要准备相关证明资料，由后台服务人员协助注册。");
                                }
                            }
                        });
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
        store: Ext.create('Tab.view.sms.signStore'),
        columns: [{
                text: '名称',
                dataIndex: 'signname',
                flex: 1
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
                    iconCls: 'x-fa fa-trash',
                    tooltip: '删除',
                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                        var rs = record;
                        var selModel = view.getSelectionModel();
                        var store = view.getStore();
                        Ext.Msg.show({
                            title: "删除",
                            message: "是否从运营商服务器删除签名?",
                            buttons: Ext.Msg.YESNOCANCEL,
                            icon: Ext.Msg.QUESTION,
                            fn: function (btn) {
                                if (btn === 'yes' || btn === 'no') {
                                    Ext.Ajax.request({
                                        url: '/tab/call/RemoveSmsSign',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            signname: rs.get('signname'),
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