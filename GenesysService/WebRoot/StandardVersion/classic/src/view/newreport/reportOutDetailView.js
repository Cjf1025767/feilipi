Ext.define('Tab.view.newreport.reportOutDetailView', {
	extend: 'Ext.grid.Panel',
	xtype: 'reportOutDetailView',

	requires: [
		'Tab.view.newreport.reportOutDetailViewController',
		'Tab.view.newreport.reportOutDetailViewModel',
		'Tab.view.newreport.ReportOutDetailStore',
		'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio'
	],

	controller: 'newreport-reportoutdetailview',
	viewModel: {
		type: 'newreport-reportoutdetailview'
	},
	store: Ext.create('Tab.view.newreport.ReportOutDetailStore'),
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
									caller: me.down('textfield[fieldLabel="主叫号码"]').getValue(),
									inbound: 2,
									nType: me.down('combobox[fieldLabel="呼出类型"]').getValue(),
									weekend: 0,
									length: 0,
									wait: 0,
									groupguid: me.down('combobox[fieldLabel="组名"]').getValue()
								};
								grid.store.loadPage(1);
							}
						}
					},
					{
						xtype: 'button',
						width: 100,
						text: '导 出',
						listeners: {
							'click': function (button) {
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
									url: '/tab/call/ReportAgentDetail',
									method: 'POST',
									hidden: true,
									headers: {
										'Content-Type': 'application/x-www-form-urlencoded'
									},
									params: {
										script: path + 'ReportAgentDetailExport.js',
										starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
										endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
										agent: me.down('combobox[name="agent"]').getValue(),
										extension: '',
										caller: me.down('textfield[fieldLabel="主叫号码"]').getValue(),
										inbound: 2,
										nType: me.down('combobox[fieldLabel="呼出类型"]').getValue(),
										weekend: 0,
										length: 0,
										wait: 0,
										groupguid: me.down('combobox[fieldLabel="组名"]').getValue()
									}
								});
								Ext.defer(function () {
									form.close(); //延迟关闭表单(不会影响浏览器下载)
								}, 100);
							}
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
				layout:'vbox',
				items: [{
						xtype: 'combobox',
						width: 180,
						fieldLabel: '组名',
						name: 'group',
						emptyText: '全部',
						labelWidth: 30,
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
						fieldLabel: '座席',
						name: 'agent',
						emptyText: '全部',
						labelWidth: 30,
						editable: true,
						queryDelay: 60000,
						triggerAction: 'all',
						displayField: 'nickname',
						valueField: 'userguid',
						store: Ext.create('Tab.model.usersMenuStore')
					}
				]
			}, {
				xtype: 'container',
				layout:'vbox',
				items: [{
						xtype: 'combobox',
						width: 180,
						fieldLabel: '呼出类型',
						labelWidth: 60,
						queryMode: 'local',
						value: 0,
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Tab.view.record.typeModel',
							data: [{
									"name": "全部",
									"id": 0
								},
								{
									"name": "呼出接听",
									"id": 2
								},
								{
									"name": "呼出未接听",
									"id": 4
								}
							]
						}),
						editable: false,
						triggerAction: 'all',
						displayField: 'name',
						valueField: 'id'
					},
					{
						xtype: 'textfield',
						width: 180,
						fieldLabel: '主叫号码',
						labelWidth: 60
					}
				]
			}
		]
	},
	columns: [{
			xtype: 'rownumberer',
			width: 30,
			align: 'center'
		},
		{
			header: "日期",
			width: 100,
			align: 'center',
			dataIndex: 'ringtime',
			renderer: function (value) {
				return value.substr(0, 10);
			}
		},
		{
			header: "来电时间",
			width: 150,
			align: 'center',
			dataIndex: 'ringtime',
			renderer: function (value) {
				return value.substr(10);
			}
		},
		{
			header: "主叫号码",
			width: 150,
			align: 'center',
			dataIndex: 'ani',
			renderer: function (value, cell, records) {
				if (records.get('type') == 9 || records.get('type') == 7 || records.get('type') == 10) {
					var caller = records.get('dnis');
					return caller;
				} else {
					return value;
				}
			}
		},
		{
			header: "呼叫类型",
			width: 150,
			align: 'center',
			dataIndex: 'type',
			renderer: function (value, cell, records) {
				if (value == 4) {
					return "呼出无人应答";
				} else if(value == 2 || value == 5){
					return "呼出接听";
				}else {
					return "";
				}
			}
		},
		{
			header: "分机",
			width: 100,
			align: 'center',
			dataIndex: 'channel',
			renderer: function (value) {
				return value == 0 ? " " : value;
			}
		},
		{
			header: "姓名",
			width: 150,
			align: 'center',
			dataIndex: 'username',
			renderer: function (value,cell, records) {
				return (value == null || value == "")? records.get('channel'):value;
			}
		},
		{
			header: "开始时间",
			width: 120,
			align: 'center',
			dataIndex: 'begintime',
			renderer: function (value) {
				return value.substr(10);
			}
		},
		{
			header: "结束时间",
			width: 120,
			align: 'center',
			dataIndex: 'endtime',
			renderer: function (value) {
				return value.substr(10);
			}
		},
		{
			header: "振铃时长",
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
			header: "处理时长",
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