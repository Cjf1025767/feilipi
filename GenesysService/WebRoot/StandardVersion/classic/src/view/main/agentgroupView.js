Ext.define('Tab.view.main.agentgroupView', {
    extend: 'Ext.panel.Panel',
    xtype: 'agentgroupView',

    requires: [
        'Tab.view.main.agentgroupViewController',
        'Tab.view.main.agentgroupViewModel',
        'Ext.grid.Panel',
        'Ext.grid.feature.Grouping',
        'Ext.grid.column.Action'
    ],

    controller: 'main-agentgroupview',
    viewModel: {
        type: 'main-agentgroupview'
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
                    iconCls: 'x-fa fa-users',
                    text: ' 坐席分组'
                }, '->',
                {
                    xtype: 'textfield',
                    width: 180,
                    fieldLabel: '坐席',
                    name: 'userName',
                    labelWidth: 50
                },
                {
                    xtype: 'textfield',
                    width: 180,
                    fieldLabel: '工号',
                    name: 'agent',
                    labelWidth: 50
                },
                {
                    xtype: 'combo',
                    width: 180,
                    fieldLabel: '权限组',
                    name: 'role',
                    labelWidth: 50,
                    typeAhead: true,
                    triggerAction: 'all',
                    valueField:'roleid',
                    displayField:'rolename',
                    store: Ext.create('Tab.view.main.agentgroupRolesStore')
                },
                {
                    xtype: 'combo',
                    width: 180,
                    fieldLabel: '队列组',
                    name: 'queues',
                    multiSelect: true,
                    labelWidth: 50,
                    triggerAction: 'all',
                    displayField:'value',
                    store: Ext.create('Tab.view.main.agentgroupQueuesStore')
                },
                {
                    xtype: 'button',
                    text: '添 加',
                    width: 100,
                    handler: function (btn) {
                        var grid = btn.up('gridpanel');
                        var textUserName = btn.up().down('textfield[name="userName"]');
                        var textAgent = btn.up().down('textfield[name="agent"]');
                        var arrayQueue = btn.up().down('combo[name="queues"]');
                        var numberRole = btn.up().down('combo[name="role"]');
                        if (textUserName.value.length == 0) {
                            textUserName.focus();
                            return;
                        }
                        if (textAgent.value.length == 0) {
                            textAgent.focus();
                            return;
                        }
                        if (arrayQueue.getValue()==null || arrayQueue.getValue().length == 0) {
                            arrayQueue.focus();
                            return;
                        }
                        if (numberRole.getValue()==null || numberRole.getValue().length == 0) {
                            numberRole.focus();
                            return;
                        }
                        Ext.Ajax.request({
                            url: '/tab/call/AddOrUpdateAgentQueue',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                value: textUserName.value,
                                field: textAgent.value,
                                values: arrayQueue.getValue(),
                                role: numberRole.value
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    grid.store.reload();
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
            'edit': function (editor, context, eOpts) {
                var ctx = context;
                if(ctx.value===ctx.originalValue){
                    return;
                }
                Ext.Ajax.request({
                    url: '/tab/call/AddOrUpdateAgentQueue',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    params: {
                        id: ctx.record.get('id'),
                        field: ctx.field,
                        value: ctx.value,
                        values: ctx.value
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            if(obj.field==='queues'){
                                ctx.record.set('queues',obj.value);
                            }else if(obj.field==='role'){
                                ctx.record.set('role',obj.value);
                                ctx.record.set('role.rolename',obj.value.rolename);
                            }
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
        store: Ext.create('Tab.view.main.agentgroupStore'),
        columns: [{
                text: '状态',
                dataIndex: 'status',
                align: 'center',
                width: 100,
                renderer: function (value,cell,records,row,col,store) {
                    return (value != 'Active') ? "<span style='color:red'>停用</span>" : "<span style='color:green'>正常</span>";
                }
            },{
                text: '坐席',
                dataIndex: 'userName',
                align: 'center',
                dirtyText: '名称已编辑',
                editor: {
                    allowBlank: false
                }
            },
            {
                text: '分机',
                dataIndex: 'phoneCrmExtension',
                align: 'center',
                dirtyText: '名称已编辑',
                editor: {
                    allowBlank: false
                }
            },
            {
                text: '工号',
                dataIndex: 'phoneCrmExtensionExtra',
                align: 'center',
                dirtyText: '名称已编辑',
                editor: {
                    allowBlank: false
                }
            },
            {
                text: '权限组',
                dataIndex: 'role.rolename',
                align: 'center',
                width: 200,
                editor: {
                    xtype: 'combo',
                    typeAhead: true,
                    triggerAction: 'all',
                    valueField:'roleid',
                    displayField:'rolename',
                    store: Ext.create('Tab.view.main.agentgroupRolesStore')
                }
            }, 
            {
                text: '队列组',
                dataIndex: 'queues',
                align: 'center',
                width: 200,
                editor: {
                    xtype: 'combo',
                    multiSelect: true,
                    triggerAction: 'all',
                    displayField:'value',
                    store: Ext.create('Tab.view.main.agentgroupQueuesStore')
                }
            }, 
            {
                xtype: 'actioncolumn',
                dataIndex: 'status',
                align: 'center',
                text: '操作',
                width: 80,
                menuDisabled: true,
                items: [{
                    tooltip:'启用/禁用',
                    getClass: function (v, meta, rec) {        
                        if (rec.data.status == 'Active') {
                            return 'x-fa fa-toggle-on fix-fa-font';
                        } else {
                            return 'x-fa fa-toggle-off fix-fa-font';
                        }
                    },
                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                        var rs = record;
                        var selModel = view.getSelectionModel();
                        var store = view.getStore();
                        Ext.Ajax.request({
                            url: '/tab/call/RemoveAgentQueue',
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
                                    if(rs.get('status')==='Inactive'){
                                        rs.set('status','Active');
                                        item.iconCls = 'x-fa fa-toggle-on fix-fa-font';
                                    }else{
                                        rs.set('status','Inactive');
                                        item.iconCls = 'x-fa fa-toggle-off fix-fa-font';
                                    }
                                    rs.commit();
                                } else {
                                    Ext.Msg.alert("错误", obj.msg);
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    }
                },
                {
                	iconCls: 'x-fa fa-keyboard-o fix-fa-font',
                    tooltip: '密码重置',
                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                    	Ext.Ajax.request({
                            url: '/tab/call/UIResetPassword',
                            method : 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            async: true,
                            params:{
                            	id: record.get('id')
                            }, 
                            failure: function(response, opts) {                          
                                console.log('server-side failure with status code ' + response.status);
                            },
                            success: function(response, opts) {
                                var obj = Ext.decode(response.responseText);
                                if(obj.success){
                                	Ext.Msg.alert('成功','成功将密码恢复为系统默认密码!');  
                                }
                                else{
                                    Ext.Msg.alert('错误','重置密码失败!');
                                }
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