Ext.define('Tab.view.main.reportSummaryView', {
    extend: 'Ext.grid.Panel',
    xtype: 'reportSummaryView',

    requires: [
        'Tab.view.main.reportSummaryViewController',
        'Tab.view.main.reportSummaryViewModel',
        'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio'
    ],

    controller: 'main-reportsummaryview',
    viewModel: {
        type: 'main-reportsummaryview'
    },
    maskOnDisable: false,
    stateId: "reportSummaryViewStateId",
    stateful: true,
    stateEvents: ['columnresize', 'columnmove', 'show', 'hide'],
    store: Ext.create('Tab.view.main.reportSummaryStore'),
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
                    Ext.apply(grid.store.proxy.extraParams, {
                        starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                        endtime: grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                        type: grid.down('radiogroup').getChecked()[0].inputValue,
                        task: grid.down('textfield[name="task"]').getValue(),
                        minlength: grid.down('spinnerfield[name="minseconds"]').getValue()
                    });
                    grid.store.reload();
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
                xtype: 'radiogroup',
                width: 200,
                layout: {
                    align: 'middle',
                    type: 'hbox'
                },
                items: [{
                        xtype: 'radiofield',
                        name: 'recordtype',
                        inputValue: '3',
                        boxLabel: '每月'
                    }, {
                        xtype: 'radiofield',
                        name: 'recordtype',
                        inputValue: '2',
                        boxLabel: '每周'
                    },
                    {
                        xtype: 'radiofield',
                        name: 'recordtype',
                        inputValue: '1',
                        boxLabel: '每日'
                    },
                    {
                        xtype: 'radiofield',
                        name: 'recordtype',
                        inputValue: '0',
                        checked: true,
                        boxLabel: '每时'
                    }
                ]
            }]
        }]
    },
    columns: [{
        header: "时间",
        align: 'left',
        width: 160,
        dataIndex: 'nYear',
        renderer: function (v, data, record) {
            var s = v;
            if (typeof (record.get('nMonth')) != 'undefined') {
                s += '-' + record.get('nMonth');
                if (typeof (record.get('nDay')) != 'undefined') {
                    s += '-' + record.get('nDay');
                    if (typeof (record.get('nHour')) != 'undefined') {
                        s += ' ' + record.get('nHour');
                    }
                }
            }else{
            	s += '-' + record.get('nWeek');
            }
            return s;
        }
    }, {
        header: "完成",
        align: 'center',
        width: 100,
        dataIndex: 'complete',
        filter: {
            type: 'string',
            emptyText: '请输入电话号码'
        }
    }, {
        header: "失败",
        align: 'left',
        width: 160,
        dataIndex: 'failed'
    }, {
        header: "处理中",
        align: 'left',
        width: 160,
        dataIndex: 'processing'
    }, {
        header: "时长",
        align: 'left',
        width: 160,
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
        width: 160,
        dataIndex: 'retrycount'
    }]
});