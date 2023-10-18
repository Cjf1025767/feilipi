Ext.define('Tab.view.main.workspaceView', {
    extend: 'Ext.container.Container',
    xtype: 'workspaceView',

    requires: [
        'Tab.view.main.workspaceViewController',
        'Tab.view.main.workspaceViewModel',
        'Ext.ux.layout.ResponsiveColumn',
        'Ext.chart.*',
        'Ext.grid.Panel'
    ],

    controller: 'main-workspaceview',
    viewModel: {
        type: 'main-workspaceview'
    },
    layout: 'responsivecolumn',

    items: [{
            xtype: 'container',
            userCls: 'big-25 small-50',
            layout: 'hbox',
            items: [{
                xtype: 'button',
                scale: 'large',
                ui: 'soft-cyan',
                iconCls: 'x-fa fa-phone largeIcon',
                height: 80,
                flex: 2,
                listeners: {
                    afterrender: function (btn) {
                        var me = btn;
                        Ext.Ajax.request({
                            url: '/tab/call/DashboardCallInfo',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    me.up().down('container[name="totalCallCount"]').setHtml('<font size="3">' + obj.totalCallCount + '</font>');
                                    me.up('workspaceView').down('container[name="totalCallOutCount"]').setHtml('<font size="3">' + obj.totalCallOutCount + '</font>');
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    },
                    click: function (btn) {
                        btn.fireEvent('afterrender', btn);
                    }
                }
            }, {
                xtype: 'panel',
                layout: 'vbox',
                flex: 3,
                items: [{
                        xtype: 'container',
                        height: 40,
                        name: 'totalCallCount',
                        html: '0',
                        width: '100%',
                        style: 'text-align:center;line-height:60px;'
                    },
                    {
                        xtype: 'container',
                        height: 40,
                        html: '<font size="3">Amount of CallIn&CallOut</font>',
                        width: '100%',
                        style: 'text-align:center;line-height:20px;'
                    }
                ]
            }]
        },
        {
            xtype: 'container',
            userCls: 'big-25 small-50',
            layout: 'hbox',
            items: [{
                xtype: 'button',
                scale: 'large',
                ui: 'soft-red',
                iconCls: 'x-fa fa-share largeIcon',
                height: 80,
                flex: 2
            }, {
                xtype: 'panel',
                layout: 'vbox',
                flex: 3,
                items: [{
                        xtype: 'container',
                        name: 'totalCallOutCount',
                        html: '0',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:60px;'
                    },
                    {
                        xtype: 'container',
                        html: '<font size="3">Amount of CallOut</font>',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:20px;'
                    }
                ]
            }]
        },
        {
            xtype: 'container',
            userCls: 'big-25 small-50',
            layout: 'hbox',
            items: [{
                xtype: 'button',
                scale: 'large',
                ui: 'soft-orange',
                iconCls: 'x-fa fa-tags largeIcon',
                height: 80,
                flex: 2,
                listeners: {
                    afterrender: function (btn) {
                        var me = btn;
                        Ext.Ajax.request({
                            url: '/tab/call/DashboardRecInfo',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    me.up().down('container[name="totalTagsCount"]').setHtml('<font size="3">' + obj.totalTagsCount + '</font>');
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    },
                    click: function (btn) {
                        btn.fireEvent('afterrender', btn);
                    }
                }
            }, {
                xtype: 'panel',
                layout: 'vbox',
                flex: 3,
                items: [{
                        xtype: 'container',
                        name: 'totalTagsCount',
                        html: '0',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:60px;'
                    },
                    {
                        xtype: 'container',
                        html: '<font size="3">Amount of Recording</font>',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:20px;'
                    }
                ]
            }]
        },
        {
            xtype: 'container',
            userCls: 'big-25 small-50',
            layout: 'hbox',
            items: [{
                xtype: 'button',
                scale: 'large',
                ui: 'soft-blue',
                iconCls: 'x-fa fa-clock-o largeIcon',
                height: 80,
                flex: 2,
                listeners: {
                    afterrender: function (btn) {
                        var me = btn;
                        Ext.Ajax.request({
                            url: '/tab/call/DashboardTaskInfo',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    me.up().down('container[name="totalTaskCount"]').setHtml('<font size="3">' + obj.total + '</font>');
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    },
                    click: function (btn) {
                        btn.fireEvent('afterrender', btn);
                    }
                }
            }, {
                xtype: 'panel',
                layout: 'vbox',
                flex: 3,
                items: [{
                        xtype: 'container',
                        name: 'totalTaskCount',
                        html: '0',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:60px;'
                    },
                    {
                        xtype: 'container',
                        html: '<font size="3">Amount of Task</font>',
                        height: 40,
                        width: '100%',
                        style: 'text-align:center;line-height:20px;'
                    }
                ]
            }]
        },
        {
            userCls: 'big-60 small-100',
            height: 300,
            xtype: 'cartesian',
            sprites: {
                type: 'text',
                text: 'Daily statistics',
                fontSize: 22,
                width: 50,
                height: 30,
                x: 5,
                y: 27
            },
            animation: !Ext.isIE9m && Ext.os.is.Desktop,
            background: '#f1f1f1',
            colors: ['#cccccc', '#c1c1c1'],
            axes: [{
                type: 'numeric',
                position: 'left',
                fields: ['callins', 'callouts'],
                hidden: true
            }, {
                type: 'category',
                position: 'bottom',
                fields: 'datetime'
            }],
            series: [{
                type: 'bar',
                xField: 'datetime',
                yField: ['callins', 'callouts'],
                label: {
                    field: 'callins',
                    display: 'insideEnd'
                },
                tooltip: {
                    trackMouse: true,
                    renderer: function (tooltip, record, item) {
                        tooltip.setHtml(record.get('datetime') + ': Incoming calls' + record.get('callins') + ',Outgoing calls' + record.get('callouts'));
                    }
                }
            }],
            listeners: {
                afterrender: function (chart) {
                    chart.store.load();
                }
            },
            store: Ext.create('Ext.data.Store', {
                fields: ['datetime', 'callins', 'callouts'],
                autoload: true,
                proxy: {
                    type: 'rest',
                    url: '/tab/call/DashboardDaySummary',
                    actionMethods: {
                        read: 'POST'
                    },
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    reader: {
                        type: 'json',
                        rootProperty: 'items'
                    }
                }
            })
        },
        {
            userCls: 'big-40 small-100',
            height: 140,
            xtype: 'cartesian',
            sprites: {
                type: 'text',
                text: 'Monthly statistics',
                fontSize: 22,
                width: 50,
                height: 30,
                x: 5,
                y: 27
            },
            animation: !Ext.isIE9m && Ext.os.is.Desktop,
            background: '#70bf73',
            colors: ['#a9d9ab'],
            insetPadding: '20 10 0 10',
            axes: [{
                type: 'numeric',
                position: 'left',
                fields: 'calls',
                hidden: true
            }, {
                type: 'category',
                position: 'bottom',
                fields: 'month'
            }],
            series: [{
                type: 'area',
                xField: 'month',
                yField: 'calls',
                style: {
                    opacity: 0.60
                },
                marker: {
                    opacity: 0,
                    scaling: 0.01,
                    fx: {
                        duration: 200,
                        easing: 'easeOut'
                    }
                },
                highlightCfg: {
                    opacity: 1,
                    scaling: 1.5
                },
                tooltip: {
                    trackMouse: true,
                    renderer: function (tooltip, record, item) {
                        tooltip.setHtml('Frequency(' + record.get('month') + '): ' + record.get('calls'));
                    }
                }
            }],
            listeners: {
                afterrender: function (chart) {
                    chart.store.load();
                }
            },
            store: Ext.create('Ext.data.Store', {
                fields: ['datetime', 'calls'],
                autoload: true,
                proxy: {
                    type: 'rest',
                    url: '/tab/call/DashboardMonthSummary',
                    actionMethods: {
                        read: 'POST'
                    },
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    reader: {
                        type: 'json',
                        rootProperty: 'items'
                    }
                }
            })
        },
        {
            userCls: 'big-40 small-100',
            height: 140,
            ui: 'light',
            title: 'Today`s statistics',
            tools: [{
                type: 'refresh',
                toggleValue: false,
                tooltip: 'Refresh',
                listeners: {
                    click: function (btn) {
                        btn.up().up().store.load();
                    }
                }
            }],
            xtype: 'cartesian',
            animation: !Ext.isIE9m && Ext.os.is.Desktop,
            background: '#a9d9ab',
            colors: ['#ffffff', '#f1f1f1'],
            insetPadding: '10 10 0 10',
            axes: [{
                type: 'numeric',
                position: 'left',
                fields: ['callins', 'callouts'],
                hidden: true
            }, {
                type: 'category',
                position: 'bottom',
                fields: 'hour'
            }],
            series: [{
                type: 'bar',
                xField: 'hour',
                yField: 'callins',
                label: {
                    field: ['callins', 'callouts'],
                    display: 'insideEnd'
                },
                tooltip: {
                    trackMouse: true,
                    renderer: function (tooltip, record, item) {
                        tooltip.setHtml(record.get('hour') + ': Incoming calls' + record.get('callins') + ',Outgoing calls' + record.get('callouts'));
                    }
                }
            }],
            listeners: {
                afterrender: function (chart) {
                    chart.store.load();
                }
            },
            store: Ext.create('Ext.data.Store', {
                fields: ['hour', 'callins', 'callouts'],
                autoload: false,
                proxy: {
                    type: 'rest',
                    url: '/tab/call/DashboardHourSummary',
                    actionMethods: {
                        read: 'POST'
                    },
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    reader: {
                        type: 'json',
                        rootProperty: 'items'
                    }
                }
            })
        },
        {
            xtype: 'grid',
            userCls: 'big-40 small-100',
            height: 300,
            ui: 'light',
            title: 'Ranking of agent',
            sortableColumns:false,
            tools: [{
                type: 'refresh',
                toggleValue: false,
                tooltip: 'Refresh',
                listeners: {
                    click: function (btn) {
                        btn.up().up().store.load();
                    }
                }
            }],
            columns: [{
                    header: "Agent",
                    flex: 2,
                    dataIndex: 'username',
                    menuDisabled: true,
                    renderer: function (value,cell, records) {
        				return (value == null || value == "")? records.get('agent'):value;
        			}
                },
                {
                    header: "Calls",
                    flex: 1,
                    dataIndex: 'calls',
                    menuDisabled: true,
                },
                {
                    header: "CallIns",
                    flex: 1,
                    dataIndex: 'callins',
                    menuDisabled: true,
                },
                {
                    header: "CallOuts",
                    flex: 1,
                    dataIndex: 'callouts',
                    menuDisabled: true,
                }
            ],
            listeners: {
                afterrender: function (grid) {
                    grid.store.load();
                }
            },
            store: Ext.create('Ext.data.Store', {
                fields: ['agent', 'calls', 'callins', 'callouts'],
                autoload: false,
                proxy: {
                    type: 'rest',
                    url: '/tab/call/DashboardAgentSummary',
                    actionMethods: {
                        read: 'POST'
                    },
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    reader: {
                        type: 'json',
                        rootProperty: 'items'
                    }
                }
            })
        },
        {
            xtype: 'grid',
            userCls: 'big-60 small-100',
            height: 300,
            ui: 'light',
            title: 'Refresh',
            tools: [{
                type: 'refresh',
                toggleValue: false,
                tooltip: 'Refresh',
                listeners: {
                    click: function (btn) {
                        btn.up().up().store.load();
                    }
                }
            }],
            dockedItems: [{
                xtype: 'pagingtoolbar',
                dock: 'bottom',
                pageSize: 10,
                displayInfo: true,
                displayMsg: 'Display data from {0} to {1}, a total of {2}',
                emptyMsg: 'No record',
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
            store: Ext.create('Tab.view.main.dashboardTaskStore'),
            listeners: {
                afterrender: function (grid) {
                    grid.store.load();
                }
            },
            columns: [{
                    header: "Task",
                    align: 'left',
                    width: 180,
                    dataIndex: 'name',
                    menuDisabled: true
                },
                {
                    text: 'Start time',
                    width: 150,
                    dataIndex: 'startdate',
                    renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                    }
                },
                {
                    text: 'End time',
                    width: 150,
                    dataIndex: 'expiredate',
                    renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                    }
                },
                {
                    text: 'Repeated interval',
                    dataIndex: 'period',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        switch (value) {
                        	case -1:
                        		return 'Stay online';
                            case 0:
                                return 'Only once';
                            case 3600000:
                                return '1 hour';
                            case 14400000:
                                return '4 hours';
                            case 28800000:
                                return '8 hours';
                            case 43200000:
                                return '12 hours';
                            case 86400000:
                                return '1 day';
                            case 604800000:
                                return '7 days(week)';
                            default:
                                return value;
                        }
                    }
                },
                {
                    text: 'Dial out batch',
                    width: 180,
                    dataIndex: 'batchid',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return record.get('batch').name;
                    }
                },
                {
                    text: 'Trunking gateway',
                    width: 180,
                    dataIndex: 'trunkid',
                    renderer: function (value, metaData, record, rowIndex, colIndex, store, view) {
                        return record.get('trunk').name;
                    }
                },
                {
                    text:'State of task',width:80,dataIndex:'complete',
                    renderer:function(value, metaData, record, rowIndex, colIndex, store, view){
                        return value?'In process':'';
                    }
                },
                {
                    text:'State of call',width:80,dataIndex:'running',
                    renderer:function(value, metaData, record, rowIndex, colIndex, store, view){
                        return value?'Calling':'Stop';
                    }
                },
                {
                    text:'State of dispatch',width:80,dataIndex:'queue',
                    renderer:function(value, metaData, record, rowIndex, colIndex, store, view){
                        return value?'In queue':'';
                    }
                },
                {
                    text: 'Nextdate',
                    width: 150,
                    dataIndex: 'nextdate',
                    renderer: function (v) {
                        if (typeof (v) == 'string') v = parseInt(v);
                        return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
                    }
                },
                {
                    header: "Success",
                    align: 'center',
                    width: 100,
                    dataIndex: 'success',
                    menuDisabled: true
                }, {
                    header: "Failed",
                    align: 'left',
                    width: 100,
                    dataIndex: 'failed',
                    menuDisabled: true,
                }, {
                    header: "Processing",
                    align: 'left',
                    width: 100,
                    dataIndex: 'processing',
                    menuDisabled: true,
                }, {
                    header: "talklength",
                    align: 'left',
                    width: 80,
                    dataIndex: 'talklength',
                    menuDisabled: true,
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
                    header: "Retrycount",
                    align: 'center',
                    width: 100,
                    dataIndex: 'retrycount',
                    menuDisabled: true,
                }
            ]
        }
    ]
});