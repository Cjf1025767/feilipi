Ext.define('Tab.view.main.appTrunkView', {
    extend: 'Ext.panel.Panel',
    xtype: 'appTrunkView',

    requires: [
        'Tab.view.main.appTrunkViewController',
        'Tab.view.main.appTrunkViewModel',
        'Ext.grid.Panel'
    ],

    controller: 'main-apptrunkview',
    viewModel: {
        type: 'main-apptrunkview'
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
                    iconCls: 'x-fa fa-connectdevelop',
                    text: ' 应用和网关'
                }, '->',
                {
                    xtype: 'button',
                    text: '添 加',
                    width: 100,
                    handler: function (btn) {
                        var grid = btn.up('gridpanel');
                        Ext.Ajax.request({
                            url: '/tab/call/AddOrUpdateTrunk',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
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
                },
                {
                    xtype: 'button',
                    text: '刷 新',
                    width: 100,
                    handler: function () {
                        var view = this.up().up();
                        view.store.load();
                    }
                }
            ]
        },
        plugins: [{
            ptype: 'cellediting',
            clicksToEdit: 2,
        }],
        listeners: {
            'afterrender': function () {
                var me = this;
                me.store.load()
            },
            'cellclick': function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
                var me = view;
                var col = me.getHeaderCt().getHeaderAtIndex(cellIndex).dataIndex;
                var rs = record;
                if (col == 'activate') {
                    Ext.Ajax.request({
                        url: '/tab/call/AddOrUpdateTrunk',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            id: record.get('id'),
                            activate: record.get('activate')
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                rs.commit();
                            } else {
                                rs.reject();
                            }
                        },
                        failure: function (response, opts) {
                            rs.reject();
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            },
            'edit': function (editor, context, eOpts) {
                var ctx = context;
                Ext.Ajax.request({
                    url: '/tab/call/AddOrUpdateTrunk',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    params: {
                        id: ctx.record.get('id'),
                        trunk: ctx.record.get('trunk'),
                        name: ctx.record.get('name'),
                        delay: ctx.record.get('delay'),
                        intercurrent: ctx.record.get('intercurrent'),
                        maxretrycount: ctx.record.get('maxretrycount'),
                        activate: ctx.record.get('activate'),
                        timetolive: ctx.record.get('timetolive')
                        
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            ctx.record.commit();
                        } else {
                            ctx.record.reject();
                        }
                    },
                    failure: function (response, opts) {
                        ctx.record.reject();
                        console.log('server-side failure with status code ' + response.status);
                    }
                });
            }
        },
        store: Ext.create('Tab.view.main.appTrunkStore'),
        columns: [{
                text: '名称',
                dataIndex: 'name',
                dirtyText: '名称已编辑',
                flex: 1,
                editor: {
                    allowBlank: true
                }
            },
            {
                text: '中继网关',
                dataIndex: 'trunk',
                dirtyText: '中继网关已编辑',
                editor: {
                    allowBlank: false
                }
            }, {
                text: '延迟(毫秒)',
                dataIndex: 'delay',
                dirtyText: '延迟已编辑',
                editor: {
                    allowBlank: false,
                    vtype: 'alphanum'
                }
            }, {
                text: '并发参数',
                dataIndex: 'intercurrent',
                dirtyText: '并发参数已编辑',
                editor: {
                    allowBlank: false,
                    vtype: 'alphanum'
                }
            }, {
                text: '重试次数',
                dataIndex: 'maxretrycount',
                dirtyText: '重试次数已编辑',
                editor: {
                    allowBlank: false,
                    vtype: 'alphanum'
                }
            },
            {
                text: '请求存活时间(秒)',
                dataIndex: 'timetolive',
                dirtyText: '请求存活时间(秒)已编辑',
                editor: {
                    allowBlank: false,
                    vtype: 'alphanum'
                }
            },
            
            {
                text: '创建时间',
                dataIndex: 'createdate',
                width: 150,
                renderer: function (v) {
                    return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                }
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
                xtype: 'checkcolumn',
                text: '激活',
                dataIndex: 'activate'
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
                        Ext.MessageBox.confirm("删除", "请确认删除",
                            function (btn, value) {
                                if (btn == 'yes') {
                                    Ext.Ajax.request({
                                        url: '/tab/call/RemoveTrunk',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            id: rs.get('id')
                                        },
                                        success: function (response, options) {
                                            var obj = Ext.decode(response.responseText);
                                            if (obj.success) {
                                                store.remove(rs);
                                                if (store.getCount() > 0) {
                                                    selModel.select(0);
                                                }
                                            } else {
                                                Ext.Msg.alert("错误", obj.msg);
                                            }
                                        },
                                        failure: function (response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        }
                                    });
                                }
                            }
                        );
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