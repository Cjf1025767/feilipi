Ext.define('Tab.view.record.RecordSummaryView', {
    extend: 'Ext.grid.Panel',
    xtype: 'recordsummaryview',

    requires: [
        'Tab.view.record.RecordSummaryViewController',
        'Tab.view.record.RecordSummaryViewModel',
        'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio',
        'Tab.view.record.RecordSummaryStore'
    ],

    controller: 'record-recordsummaryview',
    viewModel: {
        type: 'record-recordsummaryview'
    },

    store: Ext.create('Tab.view.record.RecordSummaryStore'),
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
                    text: 'Query',
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
                    text: 'Export',
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
                            url: '/tab/rec/UIReportRecSummary',
                            method: 'POST',
                            hidden: true,
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                script: path +'RecordSummaryExport.js',
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
                        fieldLabel: 'StartTime',
                        name: 'starttime',
                        labelWidth: 60,
                        timeCfg: {
                            value: '00:00'
                        }
                    },
                    {
                        xtype: 'datetimetowfield',
                        width: 280,
                        fieldLabel: 'EndTime',
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
                    width: 300,
                    layout: {
                        align: 'middle',
                        type: 'hbox'
                    },
                    items: [{
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '3',
                            boxLabel: 'Month',
                            padding:'0 10 0 0'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '2',
                            boxLabel: 'Week',
                            padding:'0 10 0 0'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '1',
                            checked: true,
                            boxLabel: 'Day',
                            padding:'0 10 0 0'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'agentSummaryType',
                            inputValue: '0',
                            boxLabel: 'Hour'
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
            header: "Host",
            width: 80,
            dataIndex: 'host'
        },
        {
            header: "Rolename",
            width: 115,
            dataIndex: 'rolename'
        },
        {
            header: "Time",
            width: 160,
            dataIndex: 'nYear',
            renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                switch (record.get('nType')) {
                    case 0:
                        return record.get('nYear') + 'Year' + PrefixInteger(record.get('nMonth'),2) + 'Month' + PrefixInteger(record.get('nDay'),2) + 'Day ' + PrefixInteger(record.get('nHour'),2) + 'Hour';
                    case 1:
                        return record.get('nYear') + 'Year' + PrefixInteger(record.get('nMonth'),2) + 'Month' + PrefixInteger(record.get('nDay'),2) + 'Day';
                    case 2:
                        return record.get('nYear') + 'Year' + PrefixInteger(record.get('nWeek'), 2) + 'Week';
                    case 3:
                        return record.get('nYear') + 'Year' + PrefixInteger(record.get('nMonth'),2) + 'Month';
                }
            }
        }, {
            header: "TotalCount",
            width: 100,
            dataIndex: 'nTotalCount'
        }, {
            header: "InBoundCount",
            width: 120,
            dataIndex: 'nInboundCount'
        }, {
            header: "OutBoundCount",
            width: 125,
            dataIndex: 'nOutboundCount'
        }, {
            header: "Seconds",
            width: 100,
            dataIndex: 'nSeconds',
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
            header: "BackupCount",
            width: 120,
            dataIndex: 'nBackupCount'
        }
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});