Ext.define('Tab.view.voicemail.voicemailView', {
	extend: 'Ext.grid.Panel',
	xtype: 'voicemailview',

	requires: [
		'Ext.window.Toast',
		'Ext.grid.column.Action',
		'Tab.view.voicemail.voicemailStore',
		'Tab.view.voicemail.voicemailModel'
	],
	controller: 'searchrecord',
	viewModel: {
		type: 'searchrecord'
	},
	 alias: 'widget.voicemailview',
	autoScroll: true,
	maskOnDisable: false,
	stateId: "recordvoicemailviewStateId",
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
	store: Ext.create('Tab.view.voicemail.voicemailStore'),
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
			this.player.makePlay(location.origin+globalVars.voicemailplayUrl + record.get('filepath'), "","");
			this.player.show();
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
								if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
									grid.down('datetimetowfield[name="starttime"]').focus();
								}
								if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
									grid.down('datetimetowfield[name="endtime"]').focus();
								}
								grid.store.proxy.extraParams = {
									starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
									endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
									phone: me.down('textfield[name="phone"]').getValue(),
									hotline_num: me.down('textfield[name="hotline_num"]').getValue(),
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
									var fileName = record.get('starttime') + '-' + record.get('phone');
									var RecordFile = {
										fileName: fileName,
										filepath: record.get('filepath'),
									};
									recordList.fileCount++;
									recordList.recordFiles.push(RecordFile);});
									var form = Ext.create('Ext.form.Panel');
									form.submit({
										target: '_blank',
										standardSubmit: true,
										url: '/tab/FLPrec/UIRecordsDownload',
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
											url: '/tab/FLPrec/UISearchVoiceMailsDownload',
											method: 'POST',
											hidden: true,
											headers: {
												'Content-Type': 'application/x-www-form-urlencoded'
											},
											params: {
												starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
												endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
												phone: me.down('textfield[name="phone"]').getValue(),
												hotline_num: me.down('textfield[name="hotline_num"]').getValue(),
												ifmailVoice:true
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
				items: [
					{
					xtype: 'textfield',
					width: 140,
					fieldLabel: 'phone',
					name: 'phone',
					labelWidth: 40
				},
				{
					xtype: 'textfield',
					width: 140,
					fieldLabel: 'hotline',
					name: 'hotline_num',
					labelWidth: 40
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
					dowloadurl = location.origin+globalVars.voicemailplayUrl + record.get('filepath');
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
			header: "phone",
			align: 'center',
			width: 105,
			dataIndex: 'phone'
		},
		{
			header: "hotline",
			align: 'center',
			width: 105,
			dataIndex: 'hotline_num'
		},
		
		{
			header: "starttime",
			align: 'center',
			width: 160,
			dataIndex: 'starttime',
			filter: {
				emptyText: 'Please enter the date'
			}
		},
		{
			header: "endtime",
			align: 'center',
			width: 160,
			dataIndex: 'endtime',
			filter: {
				emptyText: 'Please enter the date'
			}
		},
		{
			header: "length",
			width: 100,
			align: 'center',
			dataIndex: 'length',
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
	],
	viewConfig: {
		loadMask: true,
		enableTextSelection: true
	}
});