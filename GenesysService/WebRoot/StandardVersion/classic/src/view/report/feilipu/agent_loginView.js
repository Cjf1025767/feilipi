Ext.define('Tab.view.report.tongkuai.agent_loginView', {
    extend: 'Ext.grid.Panel',
    xtype: 'agent_login',
    requires: [
        'Tab.view.report.feilipu.agent_loginModel',
        'Tab.view.report.feilipu.agent_loginStore',
        'Ext.form.field.Spinner'
    ],
    store: Ext.create('Tab.view.report.feilipu.agent_loginStore'),
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
                        text: 'Agent Login'
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
                                    agent: grid.down('textfield[name="agent"]').getValue(),
                                    tenant_name: grid.down('textfield[fieldLabel="TenantName"]').getValue(),
                                    agentname:grid.down('textfield[name="agentname"]').getValue(),
                                };
                                grid.store.loadPage(1);
                            }
                        },
                        menu:[{
                            text: 'Export',
                            listeners: {
                                'click': function (button) {
                                    var grid = this.up('gridpanel');
                                    if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                                        grid.down('datetimetowfield[name="starttime"]').focus();
                                    }
                                    if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                                        grid.down('datetimetowfield[name="endtime"]').focus();
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
                                        url: '/tab/FLPrec/UISearchAgentLogin',
                                        method: 'POST',
                                        hidden: true,
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            script: path + 'agent_loginEeport.js',
                                            starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                            endtime: grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                            agent: grid.down('textfield[name="agent"]').getValue(),
                                            tenant_name: grid.down('textfield[fieldLabel="TenantName"]').getValue(),
                                            agentname:grid.down('textfield[name="agentname"]').getValue(),
                                        }
                                    });
                                    Ext.defer(function () {
                                        form.close(); //延迟关闭表单(不会影响浏览器下载)
                                    }, 100);
                                }
                            }
                        }]

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
                        width: 200,
                        name: 'agent',
                        fieldLabel: 'Agent',
                        labelWidth: 60
                    },  {
                        xtype: 'textfield',
                        name: 'agentname',
                        margin:'0 30 0 0',
                        width: 200,
                        fieldLabel: 'AgentName',
                        labelWidth: 80
                    }
                ]
            },
            {
                xtype: 'container',
                layout:'vbox',
                items: [
                    {
                        xtype: 'textfield',
                        width: 190,
                        name: 'tenant_name',
                        fieldLabel: 'TenantName',
                        labelWidth: 80
                    }
                ]
            },
           
        ]
    },
    columns: [{
            xtype: 'rownumberer',
            width: 80,//租赁
            align: 'center'
        },
        {
            header: "start time",align:'center',width: 150, dataIndex: 'start_ts_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "end time",align:'center',width: 150, dataIndex: 'end_ts_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
        	header:'agent ',width: 120,dataIndex:'employee_id',align:'center'
        },
        {
        	header:'agent name',width: 120,dataIndex:'resource_name',align:'center'
        },
       
        {
        	header:'media name',width: 120,dataIndex:'media_name',align:'center'
        },
        {
            header: "tenant name",align:'center',width: 150, dataIndex: 'tenant_name',
        },
      
        {
            header: "total_duration",align:'center',width: 150, dataIndex: 'total_duration',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});