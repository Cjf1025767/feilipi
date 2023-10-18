
Ext.define('Tab.view.sms.SmsSummaryView',{
    extend: 'Ext.grid.Panel',
    xtype: 'smssummaryview',

    requires: [
        'Tab.view.sms.SmsSummaryViewController',
        'Tab.view.sms.SmsSummaryViewModel',
        'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio',
        'Tab.view.sms.SmsSummaryStore'
    ],

    controller: 'sms-smssummaryview',
    viewModel: {
        type: 'sms-smssummaryview'
    },

    store: Ext.create('Tab.view.sms.SmsSummaryStore'),
    tbar: {
        autoScroll: true,
        items: [{
                xtype: 'container',
                layout: 'vbox',
                cls: 'fix-search-btn',
                items: [{
                    xtype: 'button',
                    width: 100,
                    margin: '0 0 10 0',
                    text: '查 询',
                    handler: function () {
                        var grid = this.up().up().up(),
                            me = this.up().up();
                        if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                            grid.down('datetimetowfield[name="starttime"]').focus();
                        }
                        if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                            grid.down('datetimetowfield[name="endtime"]').focus();
                        }
                        grid.store.proxy.extraParams = {
                            starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                            endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                            type: me.down('radiogroup').getChecked()[0].inputValue
                        }
                        grid.store.load();
                    }
                }, {
                    xtype: 'button',
                    width: 100,
                    text: '导 出',
                    handler: function () {
                        var grid = this.up().up().up(),
                            me = this.up().up();
                        if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                            grid.down('datetimetowfield[name="starttime"]').focus();
                        }
                        if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                            grid.down('datetimetowfield[name="endtime"]').focus();
                        }
                        grid.store.proxy.extraParams = {
                            starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                            endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                            type: me.down('radiogroup').getChecked()[0].inputValue
                        }
                        var form = Ext.create('Ext.form.Panel');
                        var path = window.location.pathname;
                        var pos = window.location.pathname.lastIndexOf('/');
                        if(pos>0){
                            path = window.location.pathname.substring(0,pos) + '/';
                        }
                        form.submit({
                            target: '_blank',
                            standardSubmit: true,
                            url: '/tab/call/ReportSmsSummary',
                            method: 'POST',
                            hidden: true,
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                script: path + 'SmsSummaryExport.js',
                                starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                type: me.down('radiogroup').getChecked()[0].inputValue
                            }
                        });
                        Ext.defer(function () {
                            form.close(); //延迟关闭表单(不会影响浏览器下载)
                        }, 100);
                    }
                }]
            },
            {
                xtype: 'container',
                layout: 'vbox',
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
                layout: 'vbox',
                items: [{
                    xtype: 'radiogroup',
                    width: 200,
                    layout: {
                        align: 'middle',
                        type: 'hbox'
                    },
                    items: [{
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '3',
                            boxLabel: '每月'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '2',
                            boxLabel: '每周'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '1',
                            checked: true,
                            boxLabel: '每日'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '0',
                            boxLabel: '每时'
                        }
                    ]
                }]
            }
        ]
    },
    columns: [{
            xtype: 'rownumberer',
            width: 30,
            align: 'center'
        },
        {
            header: "时间",
            width: 160,
            dataIndex: 'nYear',
            renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                switch (record.get('nType')) {
                    case 0:
                        return record.get('nYear') + '年' + PrefixInteger(record.get('nMonth'),2) + '月' + PrefixInteger(record.get('nDay'),2) + '日 ' + PrefixInteger(record.get('nHour'),2) + '时';
                    case 1:
                        return record.get('nYear') + '年' + PrefixInteger(record.get('nMonth'),2) + '月' + PrefixInteger(record.get('nDay'),2) + '日';
                    case 2:
                        return record.get('nYear') + '年' + PrefixInteger(record.get('nMonth'),2) + '月 星期' + (parseInt(record.get('nDayOfWeek')) + 1);
                    case 3:
                        return record.get('nYear') + '年' + PrefixInteger(record.get('nMonth'),2) + '月';
                }
            }
        }, {
            header: "短信总数",
            width: 100,
            dataIndex: 'nTotalCount'
        }, {
            header: "发送总数",
            width: 100,
            dataIndex: 'nSuccessCount'
        }, {
            header: "失败总数",
            width: 100,
            dataIndex: 'nErrorCount'
        }, {
            header: "发送人员",
            width: 100,
            dataIndex: 'username'
        }
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});
