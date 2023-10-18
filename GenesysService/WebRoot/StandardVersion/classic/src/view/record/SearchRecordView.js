/*  Note: app.json
    "classic": {
        "js": [
            {
                "path": "classic/src/view/record/audio.js"
            },
            {
                "path": "classic/src/view/record/audio.elan.js"
            },
            {
                "path": "classic/src/view/record/audio.regions.js"
            }
        ]
    },
*/
Ext.define('Tab.view.record.SearchRecordView', {
	extend: 'Ext.grid.Panel',
	xtype: 'searchrecordview',

	requires: [
		'Ext.window.Toast',
		'Ext.grid.column.Action',
		'Tab.view.record.SearchRecordStore',
		'Tab.view.record.SearchRecordModel'
	],

	controller: 'searchrecord',
	viewModel: {
		type: 'searchrecord'
	},
	alias: 'widget.searchrecordview',
	autoScroll: true,
	maskOnDisable: false,
	stateId: "recordSearchRecordViewStateId",
	stateful: true,
	stateEvents: ['columnresize', 'columnmove', 'show', 'hide'],
	isDownload: false,
	isPlayer: false,
	isQuality: false,
	player: Ext.create('Tab.view.record.PlayRecordWindow'),
	selModel: Ext.create('Ext.selection.CheckboxModel', {
		mode: 'SIMPLE',
		checkOnly: true,
		hidden: true
	}),
	store: Ext.create('Tab.view.record.SearchRecordStore'),

	dockedItems: [{
		xtype: 'pagingtoolbar',
		dock: 'bottom',
		pageSize: 10,
		displayInfo: true,
		displayMsg: 'Display data from {0} to {1}, a total of {2}',
		emptyMsg: 'No records',
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
				bgbar.add('records/page');
				bgbar.setStore(bgbar.up().store);
			}
		}
	}],

	listeners: {
		afterrender: function (me) {
			me.setMaxHeight(me.up().getHeight());
		},
		cellclick: function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
			var columnHeader = view.getHeaderCt().getHeaderAtIndex(cellIndex);
			if (columnHeader.dataIndex == 'bLock') {
				if (this.isQuality) {
					Ext.Ajax.request({
						url: '/tab/rec/UILockRecord',
						method: 'POST',
						headers: {
							'Content-Type': 'application/x-www-form-urlencoded'
						},
						params: {
							guidid: record.get('sGuid'),
							lock: record.get('bLock') ? false : true
						},
						async: false,
						success: function (response, options) {
							var obj = Ext.decode(response.responseText);
							if (obj.success) {
								if (record.get('bLock')) {
									record.set('bLock', false);
								} else {
									record.set('bLock', true);
								}
							}
						},
						failure: function (response, opts) {
							console.log('server-side failure with status code ' + response.status);
						}
					});
				}
			}
		},
		beforecelldblclick: function (view, td, cellIndex, record, tr, rowindex, e) {
			var columnHeader = view.getHeaderCt().getHeaderAtIndex(cellIndex);
			if (columnHeader.dataIndex != 'sUcid') {
				if (this.isPlayer) {
					this.player.close();
				}
			}
		},
		celldblclick: function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
			var columnHeader = view.getHeaderCt().getHeaderAtIndex(cellIndex);
			if (columnHeader.dataIndex != 'sUcid') {
				if (this.isPlayer && record.get('sFilename').length > 0) {
					if (typeof (globalVars) == 'undefined' || typeof (globalVars.playMode) == 'undefined') {
						this.player.makePlay(location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.mp3", record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
					} else if (globalVars.playMode == 0) {
						if (globalVars.playUrl.length == 0) {
							this.player.makePlay(location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.mp3", record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						} else {
							this.player.makePlay(globalVars.playUrl + "{" + record.get('sGuid') + "}.mp3", record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						}
					} else {
						if (globalVars.playUrl.length == 0) {
							this.player.makePlay(location.origin +'/tab/rec/records/'+ record.get('sFilename'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						} else {
							this.player.makePlay(location.origin+globalVars.playUrl + record.get('sFilename')+'?agent='+record.get('sAgent'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						}
					}
					this.player.show();
				}
			}
		},
		viewready: function () {
			var me = this;
			Ext.Ajax.request({
				url: '/tab/rbac/UIGetOperations',
				method: 'POST',
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded'
				},
				async: true,
				success: function (response, options) {
					var obj = Ext.decode(response.responseText);
					if (obj.success) {
						obj.Operations.forEach(function (s) {
							switch (s) {
								case "DEAE3629-2649-4EAA-A1E3-32C758FF2FF8":
									me.isDownload = true;
									break;
								case "4C0ECEB3-312C-4C42-B919-AFA33AFF8CA6":
									me.isPlayer = true;
									break;
								case "3CA268C0-E098-4A8B-ABF7-86E215AE7F42":
									me.isQuality = true;
									break;
							}
						});
					}
				},
				failure: function (response, opts) {
					console.log('server-side failure with status code ' + response.status);
				}
			});
		}
	},
	tbar: {
		autoScroll: true,
		items: [{
				xtype: 'container',
				layout: 'vbox',
				cls:'fix-search-btn',
				items: [{
						xtype: 'button',
						text: 'Search',
						margin: '0 0 10 0',
						width: 110,
						listeners: {
							'click': function () {
								var grid = this.up().up().up(),
									me = this.up().up();
									var morequeues=me.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString()
								if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
									grid.down('datetimetowfield[name="starttime"]').focus();
								}
								if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
									grid.down('datetimetowfield[name="endtime"]').focus();
								}
								grid.store.proxy.extraParams = {
									starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
									endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
									caller: me.down('textfield[name="caller"]').getValue(),
									called: me.down('textfield[name="called"]').getValue(),
									agent: me.down('combobox[name="agent"]').getValue(),
									backup: 0,
									lock: me.down('combobox[name="lock"]').getValue(),
									delete: 0,
									answer: 0,
									direction: me.down('combobox[name="direction"]').getValue(),
									inside: 0,
									minlength: me.down('spinnerfield[name="minseconds"]').getValue(),
									maxlength: me.down('spinnerfield[name="maxlength"]').getValue(),
									guidid:  me.down('textfield[name="calluuid"]').getValue(),
									groupguidid: me.down('combobox[name="group"]').getValue(),
									morequeues:morequeues
								};
								grid.store.loadPage(1);
							}
						}
					},
					{
						xtype: 'container',
						items: [{
							xtype: 'splitbutton',
							text: 'Download',
							width: 110,
							listeners: {
								click: function (button) {
									var grid = this.up().up().up().up(),
										selectRecords = grid.getSelection(),
										me = this.up().up().up();
									if (me.up().isDownload == false) {
										Ext.toast({
											html: 'No permission to download recording',
											title: 'Tips',
											width: 200,
											align: 'br'
										});
										return;
									}
									if (Ext.isEmpty(selectRecords)) {
										Ext.toast({
											html: 'No recording selected',
											title: 'Tips',
											width: 200,
											align: 'br'
										});
										return;
									} else if (selectRecords.length > 50) {
										Ext.toast({
											html: 'Up to 50 recording files can be downloaded',
											title: 'Tips',
											width: 200,
											align: 'br'
										});
										return;
									}

									var recordList = {
										mode: (typeof (globalVars) == 'undefined' || typeof (globalVars.playMode) == 'undefined') ? 0 : globalVars.playMode,
										hostname: location.hostname,
										port: location.port,
										origin: location.origin,
										fileCount: 0,
										recordFiles: []
									};
									Ext.Array.each(selectRecords, function (record) {
										var fileName = record.get('sSystemTim') + '-' + record.get('sExtension');
										fileName += '-' + record.get('sCaller') + '-' + record.get('sCalled') + '-' + record.get('sAgent');
										// fileName += '-' + recordList.fileCount + ((typeof (globalVars) == 'undefined'  || typeof (globalVars.playMode) == 'undefined' || globalVars.playMode == 0 || globalVars.playMode == 2) ? '.wav' : '.mp3');
										var RecordFile = {
											fileName: fileName,
											guid: record.get('sGuid'),
											recordName: record.get('filename'),
											extension: record.get('sExtension'),
											agent: record.get('sAgent')
										};
										recordList.fileCount++;
										recordList.recordFiles.push(RecordFile);
									});
									var form = Ext.create('Ext.form.Panel');
									form.submit({
										target: '_blank',
										standardSubmit: true,
										url: '/tab/rec/UIRecordsDownload',
										method: 'POST',
										hidden: true,
										headers: {
											'Content-Type': 'application/x-www-form-urlencoded'
										},
										params: {
											files: Ext.encode(recordList)
										}
									});
									Ext.defer(function () {
										form.close(); //延迟关闭表单(不会影响浏览器下载)
									}, 100);
								}
							},
							menu: [{
								text: 'Download All',
								listeners: {
									click: function (button) {
										var me = this.up().up().up().up().up();
										if (me.up().isDownload == false) {
											Ext.toast({
												html: 'No permission to download recording',
												title: 'Tips',
												width: 200,
												align: 'br'
											});
											return;
										}
										var form = Ext.create('Ext.form.Panel');
										form.submit({
											target: '_blank',
											standardSubmit: true,
											url: '/tab/FLPrec/UISearchRecordsDownload',
											method: 'POST',
											hidden: true,
											headers: {
												'Content-Type': 'application/x-www-form-urlencoded'
											},
											params: {
												script: '/js/record/SearchRecordViewExport.js',
												starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
												endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
												caller: me.down('textfield[name="caller"]').getValue(),
												called: me.down('textfield[name="called"]').getValue(),
												agent: me.down('combobox[name="agent"]').getValue(),
												backup: 0,
												lock: me.down('combobox[name="lock"]').getValue(),
												delete: 0,
												answer: 0,
												direction: me.down('combobox[name="direction"]').getValue(),
												inside: 0,
												minlength: me.down('spinnerfield[name="minseconds"]').getValue(),
												maxlength: me.down('spinnerfield[name="maxlength"]').getValue(),
												guidid:  me.down('textfield[name="calluuid"]').getValue(),
												groupguidid: me.down('combobox[name="group"]').getValue(),
												mode: (typeof (globalVars) == 'undefined' || typeof (globalVars.playMode) == 'undefined') ? 0 : globalVars.playMode,
												hostname: location.hostname,
												port: location.port,
												origin: location.origin,
												queues: me.down('combobox[fieldLabel="queues"]').getValue()
											}
										});
										Ext.defer(function () {
											form.close(); //延迟关闭表单(不会影响浏览器下载)
										}, 100);
									}
								}
							}
						]
						}]
					}
				]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'datetimetowfield',
						width: 280,
						fieldLabel: 'StarTtime',
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
				layout: 'vbox',
				items: [{
					xtype: 'combobox',
					width: 180,
					fieldLabel: 'Group',
					name: 'group',
					emptyText: 'All',
					labelWidth: 40,
					forceSelection: true,
					queryDelay: 60000,
					editable: true,
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
				}, {
					xtype: 'combobox',
					width: 180,
					fieldLabel: 'Agent',
					name: 'agent',
					emptyText: 'All',
					labelWidth: 40,
					editable: true,
					queryDelay: 60000,
					triggerAction: 'all',
					displayField: 'nickname',
					valueField: 'userguid',
					store: Ext.create('Tab.model.usersMenuStore')
				}]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'textfield',
						width: 160,
						fieldLabel: 'Caller',
						name: 'caller',
						labelWidth: 45
					},
					{
						xtype: 'textfield',
						width: 160,
						fieldLabel: 'Called',
						name: 'called',
						labelWidth: 45
					}
				]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [
					{
						xtype: 'combobox',
						width: 200,
						fieldLabel: 'Lock',
						name: 'lock',
						hidden:true,
						labelWidth: 68,
						value: 0,
						queryMode: 'local',
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Tab.view.record.typeModel',
							data: [{
									"name": "Include tag",
									"id": 0
								},
								{
									"name": "Exclude tag",
									"id": 2
								},
								{
									"name": "Tag only",
									"id": 1
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
						width: 160,
						fieldLabel: 'calluuid',
						name: 'calluuid',
						labelWidth: 45
					},
					{
						xtype: 'combobox',
						width: 215,
						fieldLabel: 'CallIn&Out',
						name: 'direction',
						
						labelWidth: 68,
						queryMode: 'local',
						value: 0,
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Tab.view.record.typeModel',
							data: [{
									"name": "CallIn&CallOut",
									"id": 0
								},
								{
									"name": "OnlyCallIn",
									"id": 2
								},
								{
									"name": "OnlyCallOut",
									"id": 1
								}
							]
						}),
						editable: false,
						triggerAction: 'all',
						displayField: 'name',
						valueField: 'id'
					},

					{
						xtype: 'combobox',
						width: 180,
						fieldLabel: 'queues',
						name: 'queues',
						hidden:true,
						emptyText: 'All',
						labelWidth: 60,
						editable: true,
						queryDelay: 60000,
						triggerAction: 'all',
						valueField: 'rolename',
						displayField: 'rolename',
						store: Ext.create('Tab.model.queuesStore')
					},
				
				]
			},
			{
				
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'spinnerfield',
						width: 150,
						fieldLabel: 'minlength(s)',
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
						xtype: 'spinnerfield',
						width: 150,
						fieldLabel: 'maxlength(s)',
						name: 'maxlength',
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
					}
				]
			
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [
					{
						fieldLabel:'MoreQueues',
						triggerAction:'all',
						xtype: 'tagfield',
						// reference: 'locations',
						// publishes: 'value',
						width:700,
						emptyText:'please choose',
						store: Ext.create('Tab.model.queuesStore'),
						valueField:'rolename',
						displayField:'rolename',
					
					}
				]
			},
		]
	},

	columns: [{
			xtype: 'rownumberer',
			width: 45,
			align: 'center'
		},
		{
			itemId: 'playColumnIndex',
			menuDisabled: true,
			text: 'Operate',
			menuText: 'Operate',
			width: 80,
			align: 'center',
			xtype: 'actioncolumn',
			items: [{
				id: 'play',
				tooltip: 'Play',
				iconCls: 'x-fa fa-play-circle-o fix-fa-font',
				handler: function (view, rowIndex, cellIndex, item, e, record, row) {
					this.up().up().fireEvent('beforecelldblclick', view, null, cellIndex, record, null, rowIndex, e, 'play');
					this.up().up().fireEvent('celldblclick', view, null, cellIndex, record, null, rowIndex, e, 'play');
				}
			},{
				itemId: 'download',
				iconCls: 'x-fa fa-download fix-fa-font',
				tooltip: 'Download',
				handler: function (view, rowIndex, cellIndex, item, e, record, row) {
					if (this.up().up().isDownload == false) {
						Ext.toast({
							html: 'No permission to download recording',
							title: 'Tips',
							width: 200,
							align: 'br'
						});
						return;
					}
					var dowloadurl;
					if (typeof (globalVars) == 'undefined' || typeof (globalVars.playMode) == 'undefined') {
						dowloadurl = location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.WAV";
					} else if (globalVars.playMode == 0) {
						if (globalVars.playUrl.length == 0) {
							dowloadurl = location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.WAV";
						} else {
							dowloadurl = globalVars.playUrl + "{" + record.get('sGuid') + "}.WAV";
						}
					} else {
						if (globalVars.playUrl.length == 0) {
							dowloadurl = location.origin + record.get('sFilename');
						} else {
							dowloadurl = location.origin+globalVars.playUrl + record.get('sFilename')+'?agent='+record.get('sAgent');
						}
					}
					Ext.core.DomHelper.append(document.body, {
						tag: 'iframe',
						frameBorder: 0,
						width: 0,
						height: 0,
						src: dowloadurl
					});
				}
			}]
		},
		{
			header: "Sign",
			menuDisabled: true,
			width: 60,
			align: 'center',
			dataIndex: 'bLock',
			renderer: function (v, m) {
				if (v) {
					m.tdCls = 'lock-cell';
				} else {
					m.tdCls = 'unlock-cell';
				}
			}
		},
		{
			header: "Agent",
			align: 'center',
			width: 100,
			dataIndex: 'sAgent',
			filter: {
				type: 'string',
				emptyText: 'Please enter the agent number'
			}
		},
		{
			header: "queues",
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
			header: "UserName",
			align: 'center',
			width: 105,
			dataIndex: 'sUserName'
		},
		{
			header: "SystemTime",
			align: 'center',
			width: 160,
			dataIndex: 'sSystemTim',
			filter: {
				emptyText: 'Please enter the date'
			}
		},
		{
			header: "Seconds",
			width: 100,
			align: 'center',
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
			},
			filter: {
				type: 'number',
				emptyText: 'Please enter the duration'
			}
		},
		{
			header: "Direction",
			menuDisabled: true,
			width: 105,
			align: 'center',
			dataIndex: 'nDirection',
			renderer: function (v, m) {
				if (v == 0 || v == 2) {
					m.tdCls = 'inbound-cell';
					m.tdAttr = 'data-qtip="呼入"';
				} else if (v == 1 || v == 3) {
					m.tdCls = 'outbound-cell';
					m.tdAttr = 'data-qtip="呼出"';
				}
			}
		},
		{
			header: "Caller",
			width: 100,
			align: 'center',
			dataIndex: 'sCaller',
			filter: {
				type: 'string',
				emptyText: 'Please enter the calling'
			}
			/*renderer: function (value, m) {
				//客户号码显示字符重复去重取号
			    if ((value == '6000033') || (value == '6000066')) {
				     var s1 = value;
				     var c;
				     var cc = s1.match(/(\d{2})/g);
				     for(var i = 0;i<cc.length;i++){
				           c = cc[i].substring(0,1);
				           s1 = s1.replace(cc[i],c);
				     }
				     return s1;
				}
			    return value;
			}*/
		},
		{
			header: "Called",
			width: 100,
			align: 'center',
			dataIndex: 'sCalled',
			filter: {
				type: 'string',
				emptyText: 'Please enter the called'
			}
			//客户号码显示字符重复去重取号
			/*renderer: function (value, m) {
			    if ((value == '6000033') || (value == '6000066')) {
				     var s1 = value;
				     var c;
				     var cc = s1.match(/(\d{2})/g);
				     for(var i = 0;i<cc.length;i++){
				           c = cc[i].substring(0,1);
				           s1 = s1.replace(cc[i],c);
				     }
				     return s1;
				}
			    return value;
			}*/
		},
		// {
		// 	header: "GroupName",
		// 	width: 100,
		// 	align: 'center',
		// 	dataIndex: 'sGroupName',
		// 	filter: {
		// 		type: 'string',
		// 		emptyText: 'Please enter the group name'
		// 	}
		// },
		{
			header: "UCID",
			width: 280,
			align: 'center',
			dataIndex: 'sUcid',
			filter: {
				type: 'string',
				emptyText: 'Please enter the UCID'
			}
		}
	],
	viewConfig: {
		loadMask: true,
		enableTextSelection: true
	}
});