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
Ext.define('Rbac.view.record.SearchRecordView', {
	extend: 'Ext.grid.Panel',
	xtype: 'searchrecordview',

	requires: [
		'Ext.window.Toast',
		'Ext.grid.column.Action',
		'Rbac.view.record.SearchRecordStore',
		'Rbac.view.record.SearchRecordModel',
		'Rbac.view.record.MarkWindow'
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
	playMode:0,//0: Tab, 1: Freeswitch, 2: Verint
	player: Ext.create('Rbac.view.record.PlayRecordWindow'),
	selModel: Ext.create('Ext.selection.CheckboxModel', {
		mode: 'SIMPLE',
		checkOnly: true,
		hidden: false
	}),
	store: Ext.create('Rbac.view.record.SearchRecordStore'),

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
	uxResized: false,
	listeners: {
		afterlayout: function() {
            var me = this,
                store = me.getStore();
            
            if (!me.rendered || me.uxResized || store.isLoading()) { return false; }
			var url = window.location.search; //获取url中"?"符后的字串
			var queryParams = new Object();
			if (url.indexOf("?") != -1) {
				var str = url.substring(1);
				strs = str.split("&");
				for(var i = 0; i < strs.length; i ++) {
					queryParams[strs[i].split("=")[0]] = decodeURI(strs[i].split("=")[1]);
				}
			}
			if(queryParams['nolist']==="1"){
				me.uxResized = true;
				var height = me.up().getHeight() + 49;
				me.up().setHeight(height);
				me.getColumns().forEach(function(col){
					if(col.dataIndex=='playcount'){
						col.setVisible(false);
					}else if(col.dataIndex=='sMark'){
						col.setVisible(false);
					}else if(col.dataIndex=='mobile'){
						col.setVisible(false);
					}else if(col.dataIndex=='sAgent'){
						col.setVisible(false);
					}else if(col.dataIndex=='bLock'){
						col.setVisible(false);
					}else if(col.dataIndex=='email'){
						col.setVisible(false);
					}else if(col.dataIndex=='sGroupName'){
						col.setVisible(false);
					}
				});
			}
        },
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
			}else if (columnHeader.dataIndex == 'sMark') {
				var win = Ext.create('Rbac.view.record.MarkWindow');
				try{
				win.params = JSON.parse(record.get('sMark') || {});
				}catch(e){win.params = {}}
				win.recordId = record.get('sGuid');
				win.record = record;
				win.show();
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
			if (!(columnHeader.dataIndex == 'sUcid' || columnHeader.dataIndex == 'sMark')) {
				if (this.isPlayer && record.get('sFilename').length > 0) {
					if (this.playMode == 0) {
						this.player.makePlay(location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.mp3", record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
					}else if (this.playMode == 1) {
						var bFirstParams = true;
						if(record.get('sFilename').indexOf("?")>location.origin.length){
							bFirstParams = false;
						}
						if(record.get('sFilename').lastIndexOf("exten=")>location.origin.length){
							this.player.makePlay(location.origin + globalVars.recordPath + record.get('sFilename'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						}else{
							if(bFirstParams){
								this.player.makePlay(location.origin + globalVars.recordPath + record.get('sFilename') + "?exten=" + record.get('sExtension'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
							}else{
								this.player.makePlay(location.origin + globalVars.recordPath + record.get('sFilename') + "&exten=" + record.get('sExtension'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
							}
						}
					}else if (this.playMode == 2) {
						var bFirstParams = true;
						if(record.get('sFilename').indexOf("?")>location.origin.length){
							bFirstParams = false;
						}
						if(record.get('sFilename').lastIndexOf("exten=")>location.origin.length){
							this.player.makePlay(location.origin + record.get('sFilename'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
						}else{
							if(bFirstParams){
								this.player.makePlay(location.origin + record.get('sFilename') + "?exten=" + record.get('sExtension'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
							}else{
								this.player.makePlay(location.origin + record.get('sFilename') + "&exten=" + record.get('sExtension'), record.get('sSystemTim') + ' - ' + record.get('sExtension'), record.get('sGuid'));
							}
						}
					}
					Ext.Ajax.request({
						url: '/tab/rbac/UIAuditLog',
						method: 'POST',
						headers: {
							'Content-Type': 'application/x-www-form-urlencoded'
						},
						params: {
							resourcename:"Recfiles",
							resourceid: record.get('sGuid'),
							operationname: "play",
							operationstatus: "1",
							operationvalue: ""
						},
						async: true,
						success: function (response, options) {
							var obj = Ext.decode(response.responseText);
							if (obj.success) {
								console.log('server-side audit log success.');
							}
						},
						failure: function (response, opts) {
							console.log('server-side failure with status code ' + response.status);
						}
					});
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
			Ext.Ajax.request({
				url: '/tab/rec/UIGetPlaybackUrl',
				method: 'POST',
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded'
				},
				async: true,
				success: function (response, options) {
					var obj = Ext.decode(response.responseText);
					if (obj.success) {
						me.playMode = obj.playMode;
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
				items: [{
						xtype: 'button',
						cls:'fix-search-btn',
						text: '查 询',
						margin: '0 0 10 0',
						width: 100,
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
									caller: me.down('textfield[name="caller"]').getValue(),
									called: me.down('textfield[name="called"]').getValue(),
									agent: me.down('combobox[name="agent"]').getValue(),
									backup: 0,
									lock: me.down('combobox[name="lock"]').getValue(),
									delete: 0,
									answer: 0,
									direction: me.down('combobox[name="direction"]').getValue(),
									inside: 0,
									length: me.down('spinnerfield[name="minseconds"]').getValue(),
									extension: me.down('textfield[name="extension"]').getValue(),
									maxlength: 0,
									guidid: '',
									ucid: me.down('textfield[name="ucid"]').getValue(),
									groupguidid: me.down('combobox[name="group"]').getValue(),
									mark: me.down('textfield[name="mark"]').getValue()
								};
								grid.store.loadPage(1);
							}
						}
					},
					{
						xtype: 'container',
						items: [{
							xtype: 'splitbutton',
							cls:'fix-search-btn',
							text: '下载',
							width: 100,
							listeners: {
								click: function (button) {
									var grid = this.up().up().up().up(),
										selectRecords = grid.getSelection(),
										me = this.up().up().up();
									if (me.up().isDownload == false) {
										Ext.toast({
											html: '没有下载录音权限',
											title: '提示',
											width: 200,
											align: 'br'
										});
										return;
									}
									if (Ext.isEmpty(selectRecords)) {
										Ext.toast({
											html: '没有选择任何录音记录',
											title: '提示',
											width: 200,
											align: 'br'
										});
										return;
									} else if (selectRecords.length > 50) {
										Ext.toast({
											html: '最多只能下载50个录音文件',
											title: '提示',
											width: 200,
											align: 'br'
										});
										return;
									}

									var recordList = {
										hostname: location.hostname,
										port: location.port,
										origin: location.origin,
										fileCount: 0,
										recordFiles: []
									};
									Ext.Array.each(selectRecords, function (record) {
										var fileName = record.get('sSystemTim') + '-' + record.get('sExtension');
										fileName += '-' + record.get('sCaller') + '-' + record.get('sCalled') + '-' + record.get('sAgent');
										fileName += '-' + recordList.fileCount;
										var RecordFile = {
											fileName: fileName,
											guid: record.get('sGuid'),
											recordName: record.get('filename')
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
								text: '下载全部',
								listeners: {
									click: function (button) {
										var me = this.up().up().up().up().up();
										if (me.up().isDownload == false) {
											Ext.toast({
												html: '没有下载录音权限',
												title: '提示',
												width: 200,
												align: 'br'
											});
											return;
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
											url: '/tab/rec/UISearchRecordsDownload',
											method: 'POST',
											hidden: true,
											headers: {
												'Content-Type': 'application/x-www-form-urlencoded'
											},
											params: {
												script: path + 'SearchRecordViewExport.js',
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
												length: me.down('spinnerfield[name="minseconds"]').getValue(),
												extension: me.down('textfield[name="extension"]').getValue(),
												maxlength: 0,
												guidid: '',
												ucid: me.down('textfield[name="ucid"]').getValue(),
												groupguidid: me.down('combobox[name="group"]').getValue(),
												mark: me.down('textfield[name="mark"]').getValue()
											}
										});
										Ext.defer(function () {
											form.close(); //延迟关闭表单(不会影响浏览器下载)
										}, 100);
									}
								}
							}, {
								text: '导出全部',
								listeners: {
									click: function (button) {
										var me = this.up().up().up().up().up();
										var form = Ext.create('Ext.form.Panel');
										var path = window.location.pathname;
										var pos = window.location.pathname.lastIndexOf('/');
										if(pos>0){
											path = window.location.pathname.substring(0,pos) + '/';
										}
										form.submit({
											target: '_blank',
											standardSubmit: true,
											url: '/tab/rec/UISearchRecordsExport',
											method: 'POST',
											hidden: true,
											headers: {
												'Content-Type': 'application/x-www-form-urlencoded'
											},
											params: {
												script: path + 'SearchRecordViewExport.js',
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
												length: me.down('spinnerfield[name="minseconds"]').getValue(),
												extension: me.down('textfield[name="extension"]').getValue(),
												maxlength: 0,
												guidid: '',
												ucid: me.down('textfield[name="ucid"]').getValue(),
												groupguidid: me.down('combobox[name="group"]').getValue(),
												mark: me.down('textfield[name="mark"]').getValue()
											}
										});
										Ext.defer(function () {
											form.close(); //延迟关闭表单(不会影响浏览器下载)
										}, 100);
									}
								}
							}]
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
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
					xtype: 'combobox',
					width: 180,
					fieldLabel: '组名',
					name: 'group',
					emptyText: '全部',
					labelWidth: 40,
					forceSelection: true,
					editable: true,
					queryDelay: 60000,
					triggerAction: 'all',
					valueField: 'roleguid',
					displayField: 'rolename',
					store: Ext.create('Rbac.view.record.groupsMenuStore'),
					listeners: {
						'select': function (me, record) {
							var roleguid = record.get('roleguid');
							var combo = me.up().down('combobox[name="agent"]');
							combo.setValue('');
							combo.store.proxy.extraParams = {
								roleguid: roleguid
							};
							combo.store.reload();
						}
					}
				}, {
					xtype: 'combobox',
					width: 180,
					fieldLabel: '座席',
					name: 'agent',
					emptyText: '全部',
					labelWidth: 40,
					editable: true,
					queryDelay: 60000,
					triggerAction: 'all',
					displayField: 'nickname',
					valueField: 'userguid',
					store: Ext.create('Rbac.view.record.usersMenuStore')
				}]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'textfield',
						width: 180,
						fieldLabel: '主叫号码',
						name: 'caller',
						labelWidth: 65
					},
					{
						xtype: 'textfield',
						width: 180,
						fieldLabel: '被叫号码',
						name: 'called',
						labelWidth: 65
					}
				]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'combobox',
						width: 170,
						fieldLabel: '标记',
						name: 'lock',
						labelWidth: 40,
						value: 0,
						queryMode: 'local',
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Rbac.view.record.typeModel',
							data: [{
									"name": "包含标记",
									"id": 0
								},
								{
									"name": "不含标记",
									"id": 2
								},
								{
									"name": "仅含标记",
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
						width: 170,
						fieldLabel: '方向',
						name: 'direction',
						labelWidth: 40,
						queryMode: 'local',
						value: 0,
						store: Ext.create('Ext.data.JsonStore', {
							model: 'Rbac.view.record.typeModel',
							data: [{
									"name": "呼入呼出",
									"id": 0
								},
								{
									"name": "仅含呼入",
									"id": 2
								},
								{
									"name": "仅含呼出",
									"id": 1
								}
							]
						}),
						editable: false,
						triggerAction: 'all',
						displayField: 'name',
						valueField: 'id'
					}
				]
			},
			{
				xtype: 'container',
				layout: 'vbox',
				items: [{
						xtype: 'spinnerfield',
						width: 150,
						fieldLabel: '最小时长(秒)',
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
						xtype: 'textfield',
						width: 150,
						fieldLabel: '分机号码',
						name: 'extension',
						labelWidth: 60
					}
				]
			}, {
				xtype: 'container',
				layout: 'vbox',
				items: [{
					xtype: 'textfield',
					width: 170,
					fieldLabel: '标识',
					name: 'ucid',
					labelWidth: 40
				},
				{
					xtype: 'textfield',
					width: 170,
					fieldLabel: '备注',
					name: 'mark',
					labelWidth: 40
				}]
			}
		]
	},

	columns: [{
			xtype: 'rownumberer',
			width: 45,
			align: 'center',
			draggable: false
		},
		{
			itemId: 'playColumnIndex',
			menuDisabled: true,
			draggable: false,
			text: '操作',
			menuText: '操作',
			width: 80,
			align: 'center',
			xtype: 'actioncolumn',
			items: [{
				id: 'play',
				tooltip: '播放',
				iconCls: 'x-fa fa-play-circle-o fix-fa-font',
				handler: function (view, rowIndex, cellIndex, item, e, record, row) {
					this.up().up().fireEvent('beforecelldblclick', view, null, cellIndex, record, null, rowIndex, e, 'play');
					this.up().up().fireEvent('celldblclick', view, null, cellIndex, record, null, rowIndex, e, 'play');
				}
			},{
				itemId: 'download',
				iconCls: 'x-fa fa-download fix-fa-font',
				tooltip: '下载',
				handler: function (view, rowIndex, cellIndex, item, e, record, row) {
					if (this.up().up().isDownload == false) {
						Ext.toast({
							html: '没有下载录音权限',
							title: '提示',
							width: 200,
							align: 'br'
						});
						return;
					}
					var dowloadurl;
					if (this.up().up().playMode == 0) {
						dowloadurl = location.origin.split(":")[0] + ":" + location.origin.split(":")[1] + ":55503/{" + record.get('sGuid') + "}.MP3";
					} else if (this.up().up().playMode == 1) {
						var bFirstParams = true;
						if(record.get('sFilename').indexOf("?")>location.origin.length){
							bFirstParams = false;
						}
						if(record.get('sFilename').lastIndexOf("exten=")>location.origin.length){
							//路径/recordings反向代理给TabRbacServer模块，可支持FreeSWITCH下载
							dowloadurl = location.origin + globalVars.recordPath + record.get('sFilename');
						}else{
							if(bFirstParams){
								dowloadurl = location.origin + globalVars.recordPath + record.get('sFilename') + "?exten=" + record.get('sExtension');
							}else{
								dowloadurl = location.origin + globalVars.recordPath + record.get('sFilename') + "&exten=" + record.get('sExtension');
							}
						}
					} else if (this.up().up().playMode == 2) {
						var bFirstParams = true;
						if(record.get('sFilename').indexOf("?")>location.origin.length){
							bFirstParams = false;
						}
						if(record.get('sFilename').lastIndexOf("exten=")>location.origin.length){
							//字段内容通过/playwindow或/playback实现反向代理给不同的录音下载服务，对接不同的录音系统
							dowloadurl = location.origin + record.get('sFilename');
						}else{
							if(bFirstParams){
								dowloadurl = location.origin + record.get('sFilename') + "?exten=" + record.get('sExtension');
							}else{
								dowloadurl = location.origin + record.get('sFilename') + "&exten=" + record.get('sExtension');
							}
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
			header: "标记",
			menuDisabled: true,
			draggable: false,
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
			header: "分机",
			align: 'center',
			draggable: false,
			width: 100,
			dataIndex: 'sExtension',
			filter: {
				type: 'string',
				emptyText: '请输入分机号'
			}
		},
		{
			header: "姓名",
			align: 'center',
			width: 100,
			dataIndex: 'sUserName'
		},
		{
			header: "员工号",
			width: 100,
			align: 'center',
			dataIndex: 'email',
			filter: {
				type: 'string',
				emptyText: '请输入账号'
			}
		},
		{
			header: "主叫",
			width: 120,
			align: 'center',
			dataIndex: 'sCaller',
			filter: {
				type: 'string',
				emptyText: '请输入主叫'
			}
		},
		{
			header: "被叫",
			width: 120,
			align: 'center',
			dataIndex: 'sCalled',
			filter: {
				type: 'string',
				emptyText: '请输入被叫'
			}
		},
		{
			header: "时间",
			align: 'center',
			width: 160,
			dataIndex: 'sSystemTim',
			filter: {
				emptyText: '请输入日期'
			}
		},
		{
			header: "时长",
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
				emptyText: '请输入时长'
			}
		},
		{
			header: "方向",
			menuDisabled: true,
			width: 60,
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
			header: "分机组",
			width: 100,
			align: 'center',
			dataIndex: 'sGroupName',
			filter: {
				type: 'string',
				emptyText: '请输入分机组'
			}
		},
		{
			header: "播放次数",
			width: 100,
			align: 'center',
			dataIndex: 'playcount',
			filter: {
				type: 'integer',
				emptyText: '请输入播放次数'
			}
		},{
			header: "备注",
			menuDisabled: true,
			draggable: false,
			flex: 1,
			dataIndex: 'sMark',
			align: 'left',renderer: function (v) {
				if (v) {
					text = '';
					try{
					var json = JSON.parse(v);
					text = '客户名称:' + (json.username ? json.username : '');
					text += ' 备注:' + (json.memo ? json.memo : '');
					}catch(e){}
					return text;
				}
			}
		},
		{
			header: "工号",
			align: 'center',
			hidden: true,
			width: 100,
			dataIndex: 'sAgent',
			filter: {
				type: 'string',
				emptyText: '请输入工号'
			}
		},
		{
			header: "分机",
			width: 100,
			hidden: true,
			align: 'center',
			dataIndex: 'mobile',
			filter: {
				type: 'string',
				emptyText: '请输入电话'
			}
		},
		{
			header: "主机",
			align: 'center',
			hidden: true,
			width: 100,
			dataIndex: 'nHost',
			filter: {
				type: 'string',
				emptyText: '请输入主机编号'
			}
		},{
			header: "通道",
			align: 'center',
			hidden: true,
			width: 100,
			dataIndex: 'nChannel',
			filter: {
				type: 'string',
				emptyText: '请输入分机通道号'
			}
		},
		{
			header: "标识",
			width: 280,
			hidden: true,
			align: 'center',
			dataIndex: 'sUcid',
			filter: {
				type: 'string',
				emptyText: '请输入标识'
			}
		}
	],
	viewConfig: {
		loadMask: true,
		enableTextSelection: true
	}
});