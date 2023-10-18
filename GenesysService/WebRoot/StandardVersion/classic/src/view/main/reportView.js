Ext.define('Tab.view.main.reportView', {
    extend: 'Ext.grid.Panel',
    xtype: 'reportView',

    requires: [
        'Tab.view.main.reportViewController',
        'Tab.view.main.reportViewModel'
    ],

    controller: 'main-reportview',
    viewModel: {
        type: 'main-reportview'
    },
    maskOnDisable: false,
    stateId: "reportViewStateId",
    stateful: true,
    stateEvents: ['columnresize', 'columnmove', 'show', 'hide'],
    store: Ext.create('Tab.view.main.reportStore'),
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
    tbar: {
        defaults: {
            margin: '5',
        },
        items: [{
            xtype: 'container',
            items: [{
                xtype: 'button',
                text: '查 询',
                width: 100,
                handler: function (btn) {
                    var me = btn,
                    grid = btn.up('gridpanel');

                    if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                        grid.down('datetimetowfield[name="starttime"]').focus();
                    }
                    if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                        grid.down('datetimetowfield[name="endtime"]').focus();
                    }
                    Ext.apply(grid.store.proxy.extraParams,{
                        starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                        endtime: grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                        caller: grid.down('textfield[name="caller"]').getValue(),
                        task: grid.down('textfield[name="task"]').getValue(),
                        minlength: grid.down('spinnerfield[name="minseconds"]').getValue()
                    });
                    grid.store.loadPage(1);
                }
            }]
        }, {
            xtype: 'container',
            padding: '0 0 0 0',
            items: [{
                    xtype: 'datetimetowfield',
                    width: 280,
                    fieldLabel: '开始时间',
                    name: 'starttime',
                    labelWidth: 60,
                    timeCfg: {
                        value: '00:00'
                    }
                },
                {
                    xtype: 'datetimetowfield',
                    width: 280,
                    fieldLabel: '结束时间',
                    name: 'endtime',
                    labelWidth: 60,
                    timeCfg: {
                        value: '23:59'
                    }
                }
            ]
        }, {
            xtype: 'container',
            items: [{
                    xtype: 'textfield',
                    width: 180,
                    fieldLabel: '电话号码',
                    name: 'caller',
                    labelWidth: 70
                },
                {
                    xtype: 'textfield',
                    width: 180,
                    fieldLabel: '应用/网关',
                    name: 'task',
                    labelWidth: 70
                }
            ]
        }, {
            xtype: 'container',
            items: [{
                xtype: 'spinnerfield',
                width: 150,
                fieldLabel: '最小时长(秒)',
                name: 'minseconds',
                labelWidth: 80,
                value: 0,
                editable: true,
                allowBlank: false,
                onSpinUp: function () {
                    var me = this;
                    if (!me.readOnly) {
                        var val = parseInt(me.getValue(), 10) || 0;
                        me.setValue(val + 1);
                    }
                },
                onSpinDown: function () {
                    var me = this;
                    if (!me.readOnly) {
                        var val = parseInt(me.getValue(), 10) || 0;
                        if (val > 0) {
                            me.setValue(val - 1);
                        }
                    }
                }
            }]
        }]
    },
    columns: [{
        xtype: 'rownumberer',
        width: 45,
        align: 'center'
    }, {
        header: "任务",
        align: 'center',
        width: 100,
        dataIndex: 'taskid',renderer:function(v,data,record){
            return record.get('task')==null ? "(已删除任务)" : record.get('task').name;
        }
    }, {
        header: "电话",
        align: 'center',
        width: 180,
        dataIndex: 'phone',
        filter: {
            type: 'string',
            emptyText: '请输入电话号码'
        }
    }, {
        header: "完成状态",
        align: 'left',
        width: 80,
        dataIndex: 'completestatus',renderer:function(v,data,record){
            if(v<0)return '失败';
            if(v==0)return '未完成';
            if(v==1)return '成功';
            return '';
        }
    }, {
        header: "状态说明",
        align: 'left',
        width: 160,
        dataIndex: 'results'
    }, {
        header: "时间",
        align: 'left',
        width: 160,
        dataIndex: 'completedate',
        renderer: function (v) {
            return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
        }
    }, {
        header: "时长",
        align: 'left',
        width: 80,
        dataIndex: 'talklength',
        renderer: function (d) {
            var hour = Math.floor(d / (3600)); //小时
            var h = d % (3600);
            var minute = Math.floor(h / (60)); //分钟
            var m = h % (60);
            var second = Math.floor(m);
            var hh = (hour < 10 ? "0" + hour : hour)
            var mm = (minute < 10 ? "0" + minute : minute)
            var ss = (second < 10 ? "0" + second : second)
            return hh + ":" + mm + ":" + ss;
        }
    }, {
        header: "重拨次数",
        align: 'center',
        width: 80,
        dataIndex: 'retrycount'
    }, {
        header: "呼叫标识",
        flex:1,
        align: 'center',
        dataIndex: 'calluuid'
    }]
});