Ext.define('Tab.view.report.tongkuai.cdr_voiceView', {
    extend: 'Ext.grid.Panel',
    xtype: 'cdr_voice',
    requires: [
        'Tab.view.report.feilipu.cdr_voiceModel',
        'Tab.view.report.feilipu.cdr_voiceStore',
        'Ext.form.field.Spinner'
    ],
    store: Ext.create('Tab.view.report.feilipu.cdr_voiceStore'),
    listeners: {
        beforerender: function (grid) {
            if( globalVars.workspaceAgent){
                grid.down('tagfield[fieldLabel="MoreQueues"]').hide();
                grid.down('combobox[fieldLabel="Group"]').hide();
                grid.down('textfield[name="agentname"]').hide();
                grid.down('combobox[name="agent"]').hide();
            }
           
        },
    },
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
            },
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
                        text: 'Cdr Voice'
                    },{
                        xtype: 'button',
                        width: 100,
                        margin:'0 0 10 8',
                        text: 'Query',
                        listeners: {
                            'click': function () {
								var grid = this.up().up().up(),
									me = this.up().up();
								if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
									grid.down('datetimetowfield[name="starttime"]').focus();
								}
								if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
									grid.down('datetimetowfield[name="endtime"]').focus();
								}
								grid.store.proxy.extraParams = {
                                    workspaceAgent:globalVars.workspaceAgent,
									starttime:grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
									endtime:grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
									agent:grid.down('combobox[name="agent"]').getValue(),
									caller:grid.down('textfield[name="caller"]').getValue(),
									inbound:grid.down('combobox[fieldLabel="State"]').getValue(),
									nType:grid.down('radiogroup').getChecked()[0].inputValue,
									groupguid:grid.down('combobox[fieldLabel="Group"]').getValue(),
									queues:grid.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString(),
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
                                       url: '/tab/FLPrec/UISearchCdrVoice',
                                       method: 'POST',
                                       hidden: true,
                                       headers: {
                                           'Content-Type': 'application/x-www-form-urlencoded'
                                       },
                                       params: {
                                           script: path + 'cdr_voiceExport.js',
                                            workspaceAgent:globalVars.workspaceAgent,
                                           starttime:grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                           endtime:grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                           agent:grid.down('combobox[name="agent"]').getValue(),
                                           caller:grid.down('textfield[name="caller"]').getValue(),
                                           agentname:grid.down('textfield[name="agentname"]').getValue(),
                                           
                                           inbound:grid.down('combobox[fieldLabel="State"]').getValue(),
                                           nType:grid.down('radiogroup').getChecked()[0].inputValue,
                                           groupguid:grid.down('combobox[fieldLabel="Group"]').getValue(),
                                           queues:grid.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString()
                                       }
                                   });
                                   Ext.defer(function () {
                                       form.close(); //延迟关闭表单(不会影响浏览器下载)
                                   }, 100);
                               }
                           }
                       }]

                    },
                    
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
						xtype: 'combobox',
						width: 170,
						fieldLabel: 'State',
						labelWidth: 60,
						queryMode: 'local',
						value: 0,
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Tab.view.record.typeModel',
							data: [{
									"name": "ALL",
									"id": 0
								},
								{
									"name": "INBOUND",
									"id": 1
								},
								{
									"name": "OUTBOUND",
									"id": 2
								},
                                {
									"name": "INTERNAL",
									"id": 3
								}
							]
						}),
						editable: false,
						triggerAction: 'all',
						displayField: 'name',
						valueField: 'id'
					},
				]
			},
            {
				xtype: 'container',
				layout:'vbox',
				items: [{
						xtype: 'combobox',
						width: 180,
						fieldLabel: 'Group',
						name: 'Group',
						emptyText: 'All',
						labelWidth: 80,
						forceSelection: true,
						editable: true,
						queryDelay: 60000,
						triggerAction: 'all',
						valueField: 'roleguid',
						displayField: 'rolename',
						store: Ext.create('Tab.model.groupsMenuStore'),
						listeners: {
							'select': function (me, record) {
								var roleguid = record.get('roleguid');
								var combo = me.up().down('combobox[name="agent"]');
								combo.setValue('');
								combo.store.proxy.extraParams = {
									roleguid: roleguid
								};
								combo.store.reload();
								var comboqueues = me.up().up().down('tagfield[name="morequeues"]');
								comboqueues.setValue('');
								comboqueues.store.proxy.extraParams = {
									roleguid: roleguid
								};
								comboqueues.store.reload();
							},
							afterrender:function(me,record){
								var roleguid = '00000000-0000-0000-0000-000000000000';
								var combo = me.up().down('combobox[name="agent"]');
								combo.setValue('');
								combo.store.proxy.extraParams = {
									roleguid: roleguid
								};
							
							}
						}
					},
					{
						xtype: 'combobox',
						width: 180,
						fieldLabel: 'Agent',
						name: 'agent',
						emptyText: 'All',
						labelWidth: 45,
						editable: true,
						queryDelay: 60000,
						triggerAction: 'all',
						displayField: 'nickname',
						valueField: 'userguid',
						store: Ext.create('Tab.model.usersMenuStore')
					}
				]
			}, 
            	
			{
				xtype: 'container',
				layout:'vbox',
				items: [
					{
						xtype: 'container',
						layout: 'vbox',
						items: [
							{
								fieldLabel:'MoreQueues',
								triggerAction:'all',
								xtype: 'tagfield',
								name:'morequeues',
								width:700,
								emptyText:'please choose',
								store: Ext.create('Tab.model.queuesStore'),
								valueField:'rolename',
								displayField:'rolename',
							
							}
						]
					},
					{
						xtype: 'radiogroup',
						width: 530,
						layout: {
							align: 'middle',
							type: 'hbox'
						},
						items: [
                            {
								xtype: 'textfield',
								name: 'agentname',
								margin:'0 30 0 0',
                                width: 170,
                                fieldLabel: 'AgentName',
                                labelWidth: 80
							},
                            {
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '0',
								checked: true,
								boxLabel: 'All',
								margin:'0 0 0 10'
							},
							{
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '1',
								boxLabel: 'Answered',
								margin:'0 0 0 10'
							},
							{
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '2',
								boxLabel: 'NoAnswered',
								margin:'0 0 0 10'
							}
						]
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

        // {
        //     header: "StartTime",align:'center',width: 150, dataIndex: 'starttime',
        //     renderer: function (value, cell, records) {
        //        return  globalVars.reverttime(value);
        //     }
        // },
        // {
        //     header: "EndTime",align:'center',width: 150, dataIndex: 'endtime',
        //     renderer: function (value, cell, records) {
        //        return  globalVars.reverttime(value);
        //     }
        // },
        // {
        //     header: "agentName",
        //     width: 150,
        //     dataIndex: 'agent_last_name',
        //     align: 'center'
        // },
        // {
        // 	header:'Agent',width: 120,dataIndex:'agent',align:'center'
        // },
        // {
        // 	header:'Caller',width: 120,dataIndex:'ani',align:'center'
        // },
        // {
        // 	header:'Called',width: 120,dataIndex:'dnis',align:'center'
        // },
        // {
        //     header: "Situation", width: 120,align:'center',dataIndex: 'type',
        //     renderer: function (value, cell, records) {
        //         switch(value){
        //         case 'Abandoned':    
        //         case 'CustomerAbandoned':
        //         case 'Redirected':
        //             return "NoAnswered";
        //         case 'IVR':
        //             return "AutomaticVoice";
        //         case 'AbandonedWhileQueued':
        //             return 'NoAnswerWait';
        //         case 'Pulled':
        //         case 'None':
        //         case 'Incomplete':
        //         case 'OutboundStopped':
        //         case 'Transferred':
        //         case 'Diverted':
        //         case 'DestinationBusy':
        //         case 'Conferenced':
        //         case 'Cleared':
        //         case 'Routed':
        //         case 'AbnormalStop':
        //         case 'Deferred':
        //             return value;
        //         case 'Completed':
        //             return "Answer";
        //         }
		// 	}
        // },
        // {
        //     header: "QueueTime",align:'center',width: 150, dataIndex: 'queuetime',
        //     renderer: function (value, cell, records) {
        //         return standartLength(value);
        //     }
        // },
        
         {
            header: "start time",align:'center',width: 150, dataIndex: 'start_date_time_string',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "end time",align:'center',width: 150, dataIndex: 'end_date_time_string',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value)
            }
        },
        {
            header: "ring_time",align:'center',width: 150, dataIndex: 'ring_time',
            renderer: function (value, cell, records) {
                if(value&&value>0){
                   return  globalVars.reverttime(value);
                }else{
                    return ''
                }
            }
        },
        {
            header: "dialing_time",align:'center',width: 150, dataIndex: 'dialing_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "answer_time",align:'center',width: 150, dataIndex: 'answer_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
         {
            header: "agentName",
            width: 150,
            dataIndex: 'agent_last_name',
            align: 'center'
        },
        {
            header: "employee_id",
            width: 150,
            dataIndex: 'employee_id',
            align: 'center'
        },
        
        {
            header: "interaction_type", width: 120,align:'center',dataIndex: 'interaction_type',
        },
        {
            header: "interaction_id",
            width: 150,
            dataIndex: 'interaction_id',
            align: 'center'
        },
        {
            header: "media_server_ixn_guid",
            width: 150,
            dataIndex: 'media_server_ixn_guid',
            align: 'center'
        },
        {
            header: "party_name",
            width: 150,
            dataIndex: 'party_name',
            align: 'center'
        },
        {
            header: "caller",
            width: 150,
            dataIndex: 'source_address',
            align: 'center'
        },

          {
            header: "called",
            width: 150,
            dataIndex: 'target_address',
            align: 'center'
        },
        {
            header: "mediation_duration",
            width: 150,
            dataIndex: 'mediation_duration',
            align: 'center',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "routing_point_duration",
            width: 150,
            dataIndex: 'routing_point_duration',
            align: 'center',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "talk_duration",
            width: 150,
            dataIndex: 'talk_duration',
            align: 'center',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "ring_duration",
            width: 150,
            dataIndex: 'ring_duration',
            align: 'center',   renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "dial_duration",
            width: 150,
            dataIndex: 'dial_duration',
            align: 'center',   renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "hold_duration",
            width: 150,
            dataIndex: 'hold_duration',
            align: 'center',
            renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "after_call_work_duration",
            width: 150,
            dataIndex: 'after_call_work_duration',
            align: 'center',   renderer: function (value, cell, records) {
                return standartLength(value);
            }
        },
        {
            header: "stop_action",
            width: 150,
            dataIndex: 'stop_action',
            align: 'center'
        },
        {
            header: "resource_role",
            width: 150,
            dataIndex: 'resource_role',
            align: 'center'
        },
        {
            header: "role_reason",
            width: 150,
            dataIndex: 'role_reason',
            align: 'center'
        },
        {
            header: "technical_result",
            width: 150,
            dataIndex: 'technical_result',
            align: 'center'
        },
        {
            header: "result_reason",
            width: 150,
            dataIndex: 'result_reason',
            align: 'center'
        },
        {
            header: "queues",
            width: 150,
            dataIndex: 'queues',
            align: 'center',
        },
        {
            header: "media_name",
            width: 150,
            dataIndex: 'media_name',
            align: 'center'
        },
        {
            header: "transfer_agent_name",
            width: 150,
            dataIndex: 'transfer_agent_name',
            align: 'center'
        },
        {
            header: "transfer_employee_id",
            width: 150,
            dataIndex: 'transfer_employee_id',
            align: 'center'
        }
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});