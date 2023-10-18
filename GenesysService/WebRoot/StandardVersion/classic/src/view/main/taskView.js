
Ext.define('Tab.view.main.taskView', {
    extend: 'Ext.panel.Panel',
    xtype: 'taskView',

    requires: [
        'Tab.view.main.taskViewController',
        'Tab.view.main.taskViewModel'
    ],

    controller: 'main-taskview',
    viewModel: {
        type: 'main-taskview'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [
        {
            xtype: 'gridpanel',
            width: '100%',
            border: 1,
            tbar: {
                autoScroll: true,
                items: [
                    {
                        xtype: 'iconlabel',
                        iconCls: 'x-fa fa-clock-o',
                        text: ' 任务'
                    }, '->',
                    {
                        xtype: 'button',
                        text: '添 加',
                        width: 100,
                        handler: function (btn) {
                            var grid = btn.up('gridpanel');
                            Ext.Ajax.request({
                                url: '/tab/call/AddOrUpdateTask',
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                success: function (response, options) {
                                    var obj = Ext.decode(response.responseText);
                                    if (obj.success) {
                                        grid.store.insert(0, obj.item);
                                        grid.findPlugin('cellediting').startEditByPosition({ row: 0, column: 0 });
                                    }
                                }, failure: function (response, opts) {
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
                            url: '/tab/call/AddOrUpdateTask',
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
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
                            }, failure: function (response, opts) {
                                rs.reject();
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    }
                },
                beforeedit: function (editor, e, options) {
                    if (e.field == 'batchid' || e.field == 'trunkid') {
                        var combo = e.grid.columns[e.colIdx].getEditor(e.record);
                        var id = e.record.get(e.field);
                        combo.getStore().reload(function (records, operation, success) {
                            combo.setValue(id);
                        });
                    }
                },
                'edit': function (editor, context, eOpts) {
                    var ctx = context;
                    Ext.Ajax.request({
                        url: '/tab/call/AddOrUpdateTask',
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        params: {
                            id: ctx.record.get('id'),
                            startdate: ctx.record.get('startdate'),
                            name: ctx.record.get('name'),
                            workdeptid: ctx.record.get('workdeptid'),
                            expiredate: ctx.record.get('expiredate'),
                            period: ctx.record.get('period'),
                            batchid: ctx.record.get('batchid'),
                            trunkid: ctx.record.get('trunkid'),
                            activate: ctx.record.get('activate'),
                            agentratio: ctx.record.get('agentratio')
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                ctx.record.set('batch', obj.item.batch);
                                ctx.record.set('trunk', obj.item.trunk);
                                ctx.record.commit();
                            } else {
                                ctx.record.reject();
                            }
                        }, failure: function (response, opts) {
                            ctx.record.reject();
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            },
            store: Ext.create('Tab.view.main.taskStore'),
            columns: [
                // {
                //     text: 'taskid', dataIndex: 'id',
                //     hidden: true
               // },
                {
                    text: '名称', dataIndex: 'name',
                    dirtyText: '名称已编辑',
                    flex: 1,
                    editor: {
                        allowBlank: true
                    }
                },
                {
                    text: '开始时间', width: 150, dataIndex: 'startdate',
                    dirtyText: '开始时间已编辑',
                    editor: {
                        type: 'textfield',
                        allowBlank: false,
                        listeners: {
                            'focus': function (txt) {
                                var v = parseInt(txt.getValue());
                                txt.setValue(Ext.Date.format(new Date(v), 'Y-m-d H:i:s'));
                            },
                            'blur': function (txt) {
                                var v = txt.getValue();
                                txt.setValue(Number(Ext.Date.parse(v, 'Y-m-d H:i:s', true)));
                            }
                        }
                    }, renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                    }
                }, {
                    text: '结束时间', width: 150, dataIndex: 'expiredate',
                    dirtyText: '结束时间已编辑',
                    editor: {
                        allowBlank: false,
                        listeners: {
                            'focus': function (txt) {
                                var v = parseInt(txt.getValue());
                                txt.setValue(Ext.Date.format(new Date(v), 'Y-m-d H:i:s'));
                            },
                            'blur': function (txt) {
                                var v = txt.getValue();
                                txt.setValue(Number(Ext.Date.parse(v, 'Y-m-d H:i:s', true)));
                            }
                        }
                    }, renderer: function (v) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                    }
                },
                {
                    text: '重复间隔', dataIndex: 'period',
                    dirtyText: '重复间隔参数已编辑',
                    editor: {
                        allowBlank: false,
                        xtype: 'combobox',
                        queryMmode: 'local',
                        displayField: 'name',
                        valueField: 'period',
                        store: Ext.create('Ext.data.Store', {
                            data: [
                                { period: -1, name: '工作日执行' },
                                { period: 0, name: '仅一次' },
                                { period: 3600000, name: '1小时' },
                                { period: 14400000, name: '4小时' },
                                { period: 28800000, name: '8小时' },
                                { period: 43200000, name: '12小时' },
                                { period: 86400000, name: '1天' },
                                { period: 604800000, name: '7天(周)' }
                            ],
                            fields: [
                                { name: 'period' },
                                { name: 'name' }
                            ]
                        }),
                    },
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        switch (value) {
                            case -1: return '工作日执行';
                            case 0: return '仅一次';
                            case 3600000: return '1小时';
                            case 14400000: return '4小时';
                            case 28800000: return '8小时';
                            case 43200000: return '12小时';
                            case 86400000: return '1天';
                            case 604800000: return '7天(周)';
                            default: return value;
                        }
                    }
                },
                {
                    text: '外拨批次', width: 180, dataIndex: 'batchid',
                    dirtyText: '外拨批次已编辑',
                    editor: {
                        xtype: 'combobox',
                        autoLoad: true,
                        displayField: 'name',
                        valueField: 'id',
                        queryMmode: 'remote',
                        allowBlank: false,
                        store: Ext.create('Tab.view.main.batchStore', {
                            filters: [
                                function (item) {
                                    if (typeof (item) == 'undefined') {
                                        return false;
                                    }
                                    if (typeof (item.data) == 'undefined') {
                                        return false;
                                    }
                                    if (typeof (item.data.status) == 'undefined') {
                                        return false;
                                    }
                                    if ((item.data.status & 1) == 1) {
                                        return true;
                                    }
                                    return false;
                                }
                            ]
                        })
                    },
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {

                        return record.get('batch') == null ? "" : record.get('batch').name;
                    }
                }, {
                    text: '中继网关', width: 180, dataIndex: 'trunkid',
                    dirtyText: '中继网关已编辑',
                    editor: {
                        xtype: 'combobox',
                        autoLoad: true,
                        displayField: 'name',
                        valueField: 'id',
                        queryMmode: 'remote',
                        allowBlank: false,
                        store: Ext.create('Tab.view.main.appTrunkStore', {
                            filters: [
                                function (item) {
                                    if (typeof (item) == 'undefined') {
                                        return false;
                                    }
                                    if (typeof (item.data) == 'undefined') {
                                        return false;
                                    }
                                    if (typeof (item.data.status) == 'undefined') {
                                        return false;
                                    }
                                    if ((item.data.status & 1) == 1) {
                                        return true;
                                    }
                                    return false;
                                }
                            ]
                        })
                    },
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return record.get('trunk') == null ? "" : record.get('trunk').name;
                    }
                },
                {
                    text: '工作日部门', dataIndex: 'workdeptid',
                    dirtyText: '部门ID 已编辑',
                    width: 160,
                    editor: {
                        allowBlank: true
                    }
                },
                {
                    text: '坐席倍率', width: 80, dataIndex: 'agentratio',
                    editor: {
                        allowBlank: false
                    },
                },
                {
                    xtype: 'checkcolumn', width: 80, text: '激活', dataIndex: 'activate'
                },
                {
                    text: '完成状态', width: 80, dataIndex: 'complete',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return value ? '处理中' : '';
                    }
                },
                {
                    text: '呼叫状态', width: 80, dataIndex: 'running',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return value ? '呼叫中' : '停止';
                    }
                },
                {
                    text: '调度状态', width: 80, dataIndex: 'queue',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return value ? '队列中' : '';
                    }
                },
                {
                    text: '下次时间', width: 150, dataIndex: 'nextdate',
                    dirtyText: '下次时间已编辑', renderer: function (v) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        if (v > 0) {
                            return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                        }
                        return "";
                    }
                },
                {
                    text: '上传语音',
                    xtype: 'widgetcolumn',
                    widget: {
                        xtype: 'button',
                        text: 'WAV',
                        handler: function (w) {
                            Ext.create("Ext.window.Window", {
                                title: '上传语音',
                                width: 400,
                                height: 150,
                                modal: true,
                                resizable: false,
                                bodyPadding: '5 0 0 5',
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
                                                    { xtype: 'hiddenfield', name: 'fid', value: w.$widgetRecord.data.name },
                                                    {
                                                        xtype: 'filefield',
                                                        fieldLabel: 'WAV',
                                                        labelWidth: 40,
                                                        width: '100%',
                                                        name: 'filename',
                                                        buttonText: '',

                                                        buttonConfig: {
                                                            iconCls: 'fa-file-audio-o'
                                                        },
                                                        validator: function (value) {
                                                            var arrType = value.split('.');
                                                            var docType = arrType[arrType.length - 1].toLowerCase();
                                                            if (docType == 'wav') {
                                                                return true;
                                                            }
                                                            return '文件类型必须为WAV';
                                                        }
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
                                        handler: function (btn) {
                                            var win = btn.up('window');
                                            var form = win.down('form').getForm();
                                            if (form.isValid()) {
                                                form.submit({
                                                    url: '/tab/call/UploadWavFile',
                                                    success: function (fp, value) {
                                                        win.close();
                                                    },
                                                    failure: function (form, action) {
                                                        switch (action.failureType) {
                                                            case Ext.form.action.Action.CLIENT_INVALID:
                                                                Ext.Msg.alert('失败', '您输入的某些内容无效');
                                                                break;
                                                            case Ext.form.action.Action.CONNECT_FAILURE:
                                                                Ext.Msg.alert('失败', '网络通讯错误');
                                                                break;
                                                            case Ext.form.action.Action.SERVER_INVALID:
                                                                Ext.Msg.alert('失败', action.result.msg);
                                                                break;
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }/*,{
                                        xtype: "button",
                                        text: "预览",
                                        handler: 'onPreviewBatch'
                                    }*/
                                ]
                            }).show();
                        }
                    }
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
                            Ext.MessageBox.confirm("删除", "请确认删除",
                                function (btn, value) {
                                    if (btn == 'yes') {
                                        Ext.Ajax.request({
                                            url: '/tab/call/RemoveTask',
                                            method: 'POST',
                                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
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
                                            }, failure: function (response, opts) {
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
            }],
            viewConfig: {
                loadMask: true,
                enableTextSelection: true
            }
        }
    ]
});