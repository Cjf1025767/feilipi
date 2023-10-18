Ext.define('Tab.view.main.HolidayWindow', {
	extend: 'Ext.window.Window',
	xtype: 'basic-window',
    requires: [
        'Tab.view.main.worktimeStore'
    ],
    title: "worktime(everyday)",
    width: 400,
    height: 300,
    modal: true,
    resizable: false,
    bodyPadding: '5 0 0 5',
    //controller: 'main-batchview',//和父窗口共享controller
    week: null,
    lastDept: null,
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
                    xtype: "gridpanel",
                    store: Ext.create('Tab.view.main.worktimeStore'),
                    plugins: [{
                        ptype: 'cellediting',
                        clicksToEdit: 2,
                    }],
                    listeners: {
                        'beforerender': function (me) {
                            me.store.load({
                                params: {
                                    week: me.up('window').week,
                                    dept: me.up('window').lastDept,
                                    onlytime: true
                                }
                            });
                        },
                        'cellclick': function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
                            var me = view;
                            var col = me.getHeaderCt().getHeaderAtIndex(cellIndex).dataIndex;
                            var rs = record;
                            if (col == 'activate') {
                                Ext.Ajax.request({
                                    url: '/tab/call/AddOrUpdateHolidays',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        callholidayid: record.get('callholidayid'),
                                        onlyactivate: true,
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
                                url: '/tab/call/AddOrUpdateHolidays',
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                params: {
                                    callholidayid: ctx.record.get('callholidayid'),
                                    onlytime: true,
                                    starttime: ctx.record.get('starttime'),
                                    endtime: ctx.record.get('endtime')
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
                    columns: [
                        {
                            xtype: 'checkcolumn',
                            sortable: false,
                            text: 'work',
                            tooltip: '取消选择，表示取消值班电话',
                            dataIndex: 'activate'
                        },
                        {
                            text: 'starttime',
                            dirtyText: 'starttime edited',
                            dataIndex: 'starttime',
                            sortable: false,
                            width: 120,
                            editor: {
                                completeOnEnter: false,
                                field: {
                                    xtype: 'timefield',
                                    editable: true,
                                    submitFormat: 'H:i:s',
                                    format: 'H:i:s',
                                    allowBlank: true,
                                    listeners: {
                                        'focus': function (txt) {
                                            var v = txt.getRawValue();
                                            if (v.indexOf(":") < 0) {
                                                v = parseInt(v);
                                                if (isNaN(v)) {
                                                    v = 0;
                                                }
                                                txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                            }
                                        }
                                    }
                                }
                            },
                            renderer: function (v) {
                                if (v == null) return "";
                                return Ext.Date.format(new Date(v), 'H:i:s')
                            }
                        },
                        {
                            text: 'endtime',
                            dirtyText: 'endtime edited',
                            dataIndex: 'endtime',
                            sortable: false,
                            width: 120,
                            editor: {
                                completeOnEnter: false,
                                field: {
                                    xtype: 'timefield',
                                    editable: true,
                                    submitFormat: 'H:i:s',
                                    format: 'H:i:s',
                                    allowBlank: true,
                                    listeners: {
                                        'focus': function (txt) {
                                            var v = txt.getRawValue();
                                            if (v.indexOf(":") < 0) {
                                                v = parseInt(v);
                                                if (isNaN(v)) {
                                                    v = 0;
                                                }
                                                txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                            }
                                        }
                                    }
                                }
                            },
                            renderer: function (v) {
                                if (v == null) return "";
                                return Ext.Date.format(new Date(v), 'H:i:s')
                            }
                        },
                        {
                            xtype: 'actioncolumn',
                            align: 'center',
                            text: 'delete',
                            tooltip: 'delete',
                            width: 50,
                            menuDisabled: true,
                            items: [{
                                iconCls: 'x-fa fa-trash',
                                tooltip: 'delete',
                                handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                                    var rs = record;
                                    var selModel = view.getSelectionModel();
                                    var store = view.getStore();
                                    Ext.MessageBox.confirm("delete", "please comfirm to delete",
                                    function (btn, value) {
                                        if (btn == 'yes') {
                                            Ext.Ajax.request({
                                                url : '/tab/call/RemoveHolidays',
                                                method : 'POST',
                                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                                params: {
                                                    callholidayid: rs.get('callholidayid')
                                                },
                                                success: function(response, options){
                                                    var obj = Ext.decode(response.responseText);
                                                    if(obj.success){
                                                        store.remove(rs);
                                                        if (store.getCount() > 0) {
                                                            selModel.select(0);
                                                        }
                                                    }else{
                                                        Ext.Msg.alert("error",obj.msg);
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
                    ]
                }
            ]
        }],
    buttonAlign: 'center',
    buttons: [
        {
            xtype: 'button',
            text: 'add',
            handler: function (me) {
                var btn = me;
                var grid = btn.up('window').down('gridpanel');
                var selModel = grid.getSelectionModel();
                Ext.Ajax.request({
                    url: '/tab/call/AddOrUpdateHolidays',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    params: {
                        onlytime: true,
                        week: btn.up('window').week,
                        dept: btn.up('window').lastDept
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            grid.store.insert(0, obj.item);
                            //selModel.select(0);
                        }
                    },
                    failure: function (response, opts) {
                        console.log('server-side failure with status code ' + response.status);
                    }
                });
            }
        },{
            xtype: "button",
            text: "refresh",
            handler: function (me) {
                var btn = me;
                var grid = btn.up('window').down('gridpanel');
                grid.store.load({
                    params: {
                        week: me.up('window').week,
                        dept: me.up('window').lastDept,
                        onlytime: true
                    }
                });;
            }
        }, {
            xtype: "button",
            text: "close",
            handler: function (me) {
                var btn = me;
                btn.up('window').close();
            }
        }
    ]
});