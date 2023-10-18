Ext.define('Tab.view.report.reportDeptDetailView', {
	extend: 'Ext.grid.Panel',
	xtype: 'reportDeptDetailView',
   //通快 明细报表 按业务分组
	requires: [
		'Tab.view.report.reportDetailViewController',
		'Tab.view.report.reportDeptDetailWebModel',
		'Tab.view.report.ReportDeptDetailModel',
		'Tab.view.report.ReportDeptDetailStore',
		'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio'
	],

	controller: 'report-reportDetailViewController',
	viewModel: {
		type: 'report-reportDeptDetailWebModel'
	},
	store: Ext.create('Tab.view.report.ReportDeptDetailStore'),
	dockedItems: [{
		xtype: 'pagingtoolbar',
		dock: 'bottom',
		pageSize: 10,
		displayInfo: true,
		displayMsg: 'Display data from {0} to {1},  a total of {2}',
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
				cls: 'fix-search-btn',
				items: [{
						xtype: 'label',
						style: {
							'margin':'8px',
							fontSize:'16px',
							marginBottom: '10px'							
						},
						text: 'Call Detial Report'
					},{
						xtype: 'button',
						width: 100,
						margin: '0 0 10 8',
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
									starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
									endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
									agent: me.down('combobox[name="agent"]').getValue(),
									extension: '',
									caller: me.down('textfield[name="caller"]').getValue(),
									inbound: me.down('combobox[fieldLabel="State"]').getValue(),
									nType: me.down('radiogroup').getChecked()[0].inputValue,
									weekend: 0,
									length: 0,
									wait: 0,
									department: me.down('combobox[fieldLabel="Department"]').getValue(),
									queues: me.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString(),
									firstdigits:me.down('tagfield[fieldLabel="FirstDigits"]').getValue().toString(),
									seconddigits:me.down('tagfield[fieldLabel="SecondDigits"]').getValue().toString(),
								};
								grid.store.loadPage(1);
							}
						},
					},{
						xtype: 'button',
						width: 100,
						margin: '0 0 10 8',
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
							
	                        var form = Ext.create('Ext.form.Panel');
	                        var path = window.location.pathname;
	                        var pos = window.location.pathname.lastIndexOf('/');
	                        if(pos>0){
	                            path = window.location.pathname.substring(0,pos) + '/';
	                        }
	                        form.submit({
	                            target: '_blank',
	                            standardSubmit: true,
	                            url: '/tab/call/ReportDeptDetailByqueues',
	                            method: 'POST',
	                            hidden: true,
	                            headers: {
	                                'Content-Type': 'application/x-www-form-urlencoded'
	                            },
	                            params: {
	                                script: path +'reportDeptDetailExport.js',
	                                starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
									endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
									agent: me.down('combobox[name="agent"]').getValue(),
									extension: '',
									caller: me.down('textfield[name="caller"]').getValue(),
									inbound: me.down('combobox[fieldLabel="State"]').getValue(),
									nType: me.down('radiogroup').getChecked()[0].inputValue,
									weekend: 0,
									length: 0,
									wait: 0,
									department: me.down('combobox[fieldLabel="Department"]').getValue(),
									queues: me.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString(),
									firstdigits:me.down('tagfield[fieldLabel="FirstDigits"]').getValue().toString(),
									seconddigits:me.down('tagfield[fieldLabel="SecondDigits"]').getValue().toString(),
	                            }
	                        });
	                        Ext.defer(function () {
	                            form.close(); //延迟关闭表单(不会影响浏览器下载)
	                        }, 100);
							}
					}
				]
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
									"name": "In&Out",
									"id": 0
								},
								{
									"name": "OnlyIn",
									"id": 1
								},
								{
									"name": "OnlyOut",
									"id": 2
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
						fieldLabel: 'Department',
						name: 'department',
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
						items: [{
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
								boxLabel: 'InAnswered',
								margin:'0 0 0 10'
							},
							{
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '2',
								boxLabel: 'OutAnswered',
								margin:'0 0 0 10'
							},
							{
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '3',
								boxLabel: 'NoInAnswered',
								margin:'0 0 0 10'
							},
							{
								xtype: 'radiofield',
								name: 'detailType',
								inputValue: '4',
								boxLabel: 'NoOutAnswered',
								margin:'0 0 0 10'
							}
						]
					}
				]
			},
			{
				xtype: 'container',
				layout:'vbox',
				items: [
					{
						fieldLabel:'FirstDigits',
						triggerAction:'all',
						xtype: 'tagfield',
						width:600,
						emptyText:'please choose',
						store: Ext.create('Tab.model.firstdigitStore'),
						valueField:'value',
						displayField:'name',
					},
					{
						fieldLabel:'SecondDigits',
						triggerAction:'all',
						xtype: 'tagfield',
						width:600,
						emptyText:'please choose',
						store: Ext.create('Tab.model.seconddigitStore'),
						valueField:'value',
						displayField:'name',
					}
				]
			},
		]
	},
	columns: [{
			xtype: 'rownumberer',
			width: 80,
			align: 'center'
		},
		{
			header: "RingDate",
			width: 120,
			align: 'center',
			dataIndex: 'ringdate',
			renderer: function (value) {
				return value;
			}
		},
		{
			header: "RingTime",
			width: 120,
			align: 'center',
			dataIndex: 'ringtime',
			renderer: function (value) {
				return value;
			}
		},
		
		// {
		// 	header: "Caller",
		// 	width: 150,
		// 	align: 'center',
		// 	dataIndex: 'ani',
		// 	renderer: function (value, cell, records) {
		// 		if (records.get('type') == 2 || records.get('type') == 4 || records.get('type') == 5) {
		// 			var caller = records.get('dnis');
		// 			return caller;
		// 		} else {
		// 			return value;
		// 		}
		// 	}
		// },
		{
			header: "Caller",
			width: 150,
			align: 'center',
			dataIndex: 'ani',
		},
		{
			header: "Called",
			width: 150,
			align: 'center',
			dataIndex: 'dnis',
		},
		{
			header: "Hotline",
			width: 150,
			align: 'center',
			dataIndex: 'targetinfo',
		},
		{
			header: "Type",
			width: 150,
			align: 'center',
			dataIndex: 'type',
			renderer: function (value, cell, records) {
				if (value == 4) {
					return "NoAnswered";
				} else if(value == 2 || value == 5){
					return "OutBoundAnswered";
				}else if (value == 0 || value == 1) {
					return "InBoundAnswered";
				} else if (value == 3) {
					return "NoInBoundAnswered";
				}else {
					return "";
				}
			}
		},
		{
			header: "Department",
			width: 100,
			align: 'center',
			dataIndex: 'department',
		},
		{
			header: "Queues",
			width: 100,
			align: 'center',
			dataIndex: 'queues',
			renderer: function (value) {
				if (typeof globalVars.monitorMaping[value] == 'undefined') {
					return value;
				}
				return globalVars.monitorMaping[value];
			}
		},
		{
			header: "Agent",
			width: 100,
			align: 'center',
			dataIndex: 'channel',
			renderer: function (value) {
				return value == 0 ? " " : value;
			}
		},
		{
			header: "Name",
			width: 150,
			align: 'center',
			dataIndex: 'username',
			renderer: function (value,cell, records) {
				return (value == null || value == "")? records.get('channel'):value;
			}
		},
		{
			header: "StartTime",
			width: 120,
			align: 'center',
			dataIndex: 'begintime',
			renderer: function (value) {
				return value;
			}
		},
		{
			header: "EndTime",
			width: 120,
			align: 'center',
			dataIndex: 'endtime',
			renderer: function (value) {
				return value;
			}
		},
		{
			header: "RingWaitTime",
			width: 120,
			align: 'center',
			dataIndex: 'wait',
			renderer: function (value, cell, records) {
				var hour = Math.floor(value / (3600)); //小时
				var h = value % (3600);
				var minute = Math.floor(h / (60)); //分钟
				var m = h % (60);
				var second = Math.floor(m);
				var hh = (hour < 10 ? "0" + hour : hour)
				var mm = (minute < 10 ? "0" + minute : minute)
				var ss = (second < 10 ? "0" + second : second)
				return hh + ":" + mm + ":" + ss;
			}
		},
		{

            header: "Satisfaction",
            width: 150,
            dataIndex: 'satisfaction', 
            renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                return v;
            }
        },
		{
			header: "Length",
			width: 120,
			align: 'center',
			dataIndex: 'length',
			renderer: function (value) {
				var hour = Math.floor(value / (3600)); //小时
				var h = value % (3600);
				var minute = Math.floor(h / (60)); //分钟
				var m = h % (60);
				var second = Math.floor(m);
				var hh = (hour < 10 ? "0" + hour : hour)
				var mm = (minute < 10 ? "0" + minute : minute)
				var ss = (second < 10 ? "0" + second : second)
				return hh + ":" + mm + ":" + ss;
			}
		}
	],
	viewConfig: {
		loadMask: true,
		enableTextSelection: true
	}
});