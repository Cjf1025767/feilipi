Ext.define('Tab.view.report.tongkuai.NoAnsweredCall', {
    extend: 'Ext.grid.Panel',
    xtype: 'NoAnsweredCall',

    requires: [
        'Tab.view.report.tongkuai.NoAnsweredCallController',
        'Tab.view.report.tongkuai.NoAnsweredCallViewModel',
        'Tab.view.report.tongkuai.NoAnsweredCallModel',
        'Tab.view.report.tongkuai.NoAnsweredCallStore',
        'Ext.form.field.Spinner'
    ],

    controller: 'report-NoAnsweredCall',
    viewModel: {
        type: 'report-NoAnsweredCall'
    },

    store: Ext.create('Tab.view.report.tongkuai.NoAnsweredCallStore'),
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
                bgbar.add('Records/Page');
                bgbar.setStore(bgbar.up().store);
            }
        }
    }],
    tbar: {
        autoScroll: true,
        items: [{
                xtype: 'container',
                layout: 'vbox',
                cls:'fix-search-btn',
                items: [{
                        xtype: 'label',
                        style: {
                            'margin':'7px',
                            fontSize:'16px',
                            marginBottom: '10px'
                        },
                        text: 'No Answered Call'
                    },{
                        xtype: 'button',
                        width: 100,
                        margin:'0 0 10 8',
                        text: 'Query',
                        listeners: {
                            'click': function () {
                                var grid = this.up('gridpanel');
                                if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                                    grid.down('datetimetowfield[name="starttime"]').focus();
                                }
                                if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                                    grid.down('datetimetowfield[name="endtime"]').focus();
                                }
                                grid.store.proxy.extraParams = {
                                    starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                    endtime: grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                    department: grid.down('combobox[name="department"]').getValue(),
                                    agent: grid.down('textfield[name="agent"]').getValue(),
                                    caller: grid.down('textfield[name="caller"]').getValue()
                                };
                                grid.store.loadPage(1);
                            }
                        },
//                        menu:[{
//                            text: 'Export',
//                            listeners: {
//                                'click': function (button) {
//                                    var grid = this.up('gridpanel');
//                                    if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
//                                        grid.down('datetimetowfield[name="starttime"]').focus();
//                                    }
//                                    if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
//                                        grid.down('datetimetowfield[name="endtime"]').focus();
//                                    }
//                                    var form = Ext.create('Ext.form.Panel');
//                                    var path = window.location.pathname;
//                                    var pos = window.location.pathname.lastIndexOf('/');
//                                    if(pos>0){
//                                        path = window.location.pathname.substring(0,pos) + '/';
//                                    }
//                                    form.submit({
//                                        target: '_blank',
//                                        standardSubmit: true,
//                                        url: '/tab/call/ReportIvrVdn',
//                                        method: 'POST',
//                                        hidden: true,
//                                        headers: {
//                                            'Content-Type': 'application/x-www-form-urlencoded'
//                                        },
//                                        params: {
//                                            script: path + 'NoAnsweredCallViewExport.js',
//                                            starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
//                                            endtime: grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
//                                            department: grid.down('combobox[name="department"]').getValue(),
//                                            agent: grid.down('textfield[name="agent"]').getValue(),
//                                            caller: grid.down('textfield[name="caller"]').getValue()
//                                        }
//                                    });
//                                    Ext.defer(function () {
//                                        form.close(); //延迟关闭表单(不会影响浏览器下载)
//                                    }, 100);
//                                }
//                            }
//                        }]
                    }
                ]
            },
            {
                xtype: 'container',
                layout:'vbox',
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
            },
            {
                xtype: 'container',
                layout:'vbox',
                items: [
                    {
                        xtype: 'textfield',
                        width: 170,
                        name: 'caller',
                        fieldLabel: 'Caller',
                        labelWidth: 60
                    },
                    {
                        xtype: 'textfield',
                        width: 170,
                        name: 'agent',
                        fieldLabel: 'Agent',
                        labelWidth: 60
                    }
                ]
            },
            {         	
	            xtype: 'container',
	            layout:'vbox',
	            items: [
                    {
                        xtype: 'combobox',
                        width: 195,
                        margin:'0 0 5 9',
                        name:'department',
                        forceSelection: true,
                        fieldLabel: 'department',
                        emptyText: 'All',
                        labelWidth: 80,
                        forceSelection: true,
                        editable: true,
                        queryDelay: 60000,
                        triggerAction: 'all',
                        valueField: 'roleguid',
                        displayField: 'rolename',
                        store: Ext.create('Tab.model.groupsMenuStore'),
                    },
	        ]}
        ]
    },
    columns: [{
            xtype: 'rownumberer',
            width: 80,//租赁
            align: 'center'
        },
        {
            header: "department",width: 150,dataIndex: 'department',align: 'center'
        },
        {
            header: "Time",width: 150,dataIndex: 'begintime',align: 'center'
        },
        {
            header: "UserName",
            width: 150,
            dataIndex: 'username',
            align: 'center',
            renderer: function (value,cell, records) {
				return (value == null || value == "")? records.get('agent'):value;
			}
        },
        {
        	header:'Agent',width: 120,dataIndex:'channel',align:'center'
        },
        {
        	header:'Caller',width: 120,dataIndex:'ani',align:'center'
        },
        {
            header:'Callback',width: 120,dataIndex:'lastcalltime',align:'center',
            renderer: function (value,cell, records) {
                if(value==null||value==""){
                    return "not done"
                } else {
                    var calldate=records.get("ani")
                    if(value>calldate){//最近接通的一通电话的来电时间>这通未接来电的时间
                        return  "already done"
                    }else{
                        return "not done"
                    }
                }
           }
        },
        // {
        //     xtype: 'actioncolumn',
        //     align: 'center',
        //     text: 'toDial',
        //     tooltip: 'toDial',
        //     width: 80,
        //     menuDisabled: true,
        //     items: [{
        //         iconCls: 'x-fa fa-phone',
        //         tooltip: 'toDial',
        //         handler: function (view, rowIndex, cellIndex, item, e, record, row) {
        //             var grid = this.up('gridpanel');
        //                 genesys.wwe.service.agent.get(function(mes){
        //                 },function(mes){
        //                 })
        //                 genesys.wwe.service.voice.dial(record.get("ani"), { myAttachedDataKey1: "myAttachedDataValue1", myAttachedDataKey2: "myAttachedDataValue2" },function(mes){
        //                     var rec =grid.store.getAt(rowIndex);
        //                     var time=new Date().getTime()
        //                     rec.set('lastcalltime',time );
        //                     rec.commit();
        //                    console.log('callbackSuccess:'+JSON.stringify(mes))

        //                 },function(mes){
        //                     console.log('callbackFail:'+JSON.stringify(mes))
        //                 })
        //         }
        //     }]
        // },
        {
        	header:'Called',width: 120,dataIndex:'dnis',align:'center'
        },
        {
            header: "Situation", width: 120,align:'center',dataIndex: 'type',
            renderer: function (value, cell, records) {
                switch(value){
                case 'Abandoned':    
                case 'CustomerAbandoned':
                case 'Redirected':
                    return "NoAnswered";
                case 'IVR':
                    return "AutomaticVoice";
                case 'AbandonedWhileQueued':
                    return 'NoAnswerWait';
                case 'Pulled':
                case 'None':
                case 'Incomplete':
                case 'OutboundStopped':
                case 'Transferred':
                case 'Diverted':
                case 'DestinationBusy':
                case 'Conferenced':
                case 'Cleared':
                case 'Routed':
                case 'AbnormalStop':
                case 'Deferred':
                    return value;
                case 'Completed':
                    return "Answer";
                }
			}
        },
        {
            header: "StartTime",align:'center',width: 150, dataIndex: 'ringtime',
            renderer: function (value, cell, records) {
                return Ext.Date.format(new Date(value),'Y-m-d H:i:s');
            }
        },
        {
            header: "EndTime",align:'center',width: 150, dataIndex: 'endtime',
            renderer: function (value, cell, records) {
                return Ext.Date.format(new Date(value),'Y-m-d H:i:s');
            }
        },
        {
            header: "QueueTime",align:'center',width: 150, dataIndex: 'queuetime',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "RingCount",align:'center',width: 150, dataIndex: 'ringcount'
        },
        {
            header: "Automatic voice duration",align:'center',width: 200, dataIndex: 'wait',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        }
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});