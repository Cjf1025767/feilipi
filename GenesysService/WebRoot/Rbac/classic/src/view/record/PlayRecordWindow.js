Ext.define('Rbac.view.record.PlayRecordWindow', {
	extend: 'Ext.window.Window',
	xtype: 'basic-window',
	requires: [
		'Ext.button.Segmented'
	],
	origintitle: 'Play recording',
	height: 175,
	width: 560,
	autoScroll: false,
	bodyPadding: 0,
	closeAction: 'hide',
	constrain: true,
	closable: true,
	modal: false,
	resizable: false, //调整窗体大小
	alwaysOnTop: true,
	canClosable: false,

	recordid: null,
	ws: null,
	prevAnnotation: null,
	prevRow: null,
	region: null,
	audioUrl: '',
	audioStep: 0,
	html: '<div id=tabWaveform></div><div id=tabText></div>',
	listeners: {
		'afterrender': function () {
			var me = this;
			if (!Ext.isIE) {
				me.ws = WaveSurfer.create({
					container: document.querySelector('#tabWaveform'),
					waveColor: '#A8DBA8',
					progressColor: '#3B8686',
					backend: 'MediaElement',
					audioRate: 1,
					height: 85,
					waveColor: '#03b4d5',
					progressColor: '#000080'
				});
				me.ws.on('error', function (e) {
					console.warn(e);
					me.canClosable = true;
					me.close();
					var msg = e;
					if (e=='Error loading media element'){
						msg = "Recording download failed";
					}else if(e=="Error decoding audiobuffer"){
						msg = "Recording download failed";
					}
					Ext.Msg.alert('Error', msg);
				});
				me.ws.on('ready', function () {
					me.canClosable = true;
					if (me.ws.getDuration()) {
						me.down('#audioTimer').setText("0/" + Math.ceil(me.ws.getDuration()) + "(s)");
					}
					var tool = me.down('#toggleButton');
					if (tool.toggleValue) {
						Ext.Ajax.request({
							url: '/tab/rec/UIGetRecText',
							method: 'POST',
							headers: {
								'Content-Type': 'application/x-www-form-urlencoded'
							},
							async: true,
							params: {
								recordid: me.recordid
							},
							failure: function (response, opts) {
								console.log('server-side failure with status code ' + response.status);
							},
							success: function (response, opts) {
								var obj = Ext.decode(response.responseText);
								if (obj.success) {
									me.ws.elan.load(obj.items);
									me.ws.play();
									var playbtn = me.down('#playPause');
									playbtn.setIconCls('x-fa fa-pause');
									playbtn.toggleValue = true;
								}
							}
						});	
					}else{
						me.ws.play();
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-pause');
						playbtn.toggleValue = true;
					}
				});
				me.ws.on('audioprocess', function (time) {
					var step = Math.ceil(time);
					if (me.audioStep != step) {
						me.audioStep = step;
						me.down('#audioTimer').setText(step + "/" + Math.ceil(me.ws.getDuration()) + "(s)");
					}
				});
			} else {
				me.down("#toggleButton").hide();
				me.down("#playPause").hide();
				me.down("#speedButtons").hide();
				me.down("#audioTimer").hide();
				me.setHeight(103);
				me.canClosable = true;
			}
		},
		'show': function () {
			if (!Ext.isIE) {
				var me = this;
				me.ws.load(me.audioUrl);
				if(typeof me.lastY == 'undefined') me.lastY = me.getY() - 100;
				me.setPosition(me.getX(), me.lastY);
			}
		},
		'beforeclose': function () {
			return this.canClosable;
		},
		'close': function () {
			if (!Ext.isIE) {
				var me = this;
				if (me.ws.isPlaying()) {
					me.ws.stop();
				}
			} else {
				var win = window.document.getElementById('MediaPlayer1');
				if (win != undefined) win.controls.stop();
			}
		}
	},
	makePlay: function (url, title, recordid) {
		var me = this;
		if (!Ext.isIE) {
			me.recordid = recordid;
			me.setTitle(me.origintitle + '-' + title);
			me.audioUrl = url;
		} else {
			me.update('<div id="mplayer" align="center"><object id="MediaPlayer1" name="mediaplayer1" width="556" height="45" classid="CLSID:6BF52A52-394A-11d3-B153-00C04F79FAA6" codebase="http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=6,4,7,1112" align="baseline" border="0" standby="Loading Microsoft Windows Media Player components..." type="application/x-oleobject" ><param name="volume" value="100"> <param name="autoStart" value="true"> <param name="invokeURLs" value="false"> <param name="defaultFrame" value="datawindow"> <param name="AllowChangeDisplaySize" value="1"><param name="URL" value="' +
				url + '"></object></div>');
		}
	},
	tools: [{
		itemId: 'toggleButton',
		type: 'down',
		toggleValue: false,
		tooltip: 'Speech Recognition',
		listeners: {
			click: function (tool, e, o) {
				var me = o;
				if (me.ws.isPlaying()) {
					me.down('#playPause').fireEvent('click', me.down('#playPause'));
				}
				tool.toggleValue = !tool.toggleValue;
				if (tool.toggleValue) {
					me.ws.addPlugin(WaveSurfer.elan.create({
						container: '#tabText'
					})).initPlugin('elan');
					me.ws.addPlugin(WaveSurfer.regions.create()).initPlugin('regions');
					tool.setType('up');
					me.setHeight(395);
					me.ws.elan.on('select', function (start, end) {
						me.ws.backend.play(start, end);
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-pause');
						playbtn.toggleValue = true;
					});
					me.ws.elan.on('ready', function (data) {
						me.ws.un('audioprocess');
						me.ws.on('audioprocess', function (time) {
							var step = Math.ceil(time);
							if (me.audioStep != step) {
								me.audioStep = step;
								me.down('#audioTimer').setText(step + "/" + Math.ceil(me.ws.getDuration()) + "(s)");
							}
							var annotation = me.ws.elan.getRenderedAnnotation(time);

							if (me.prevAnnotation != annotation) {
								me.prevAnnotation = annotation;

								me.region && me.region.remove();
								me.region = null;

								if (annotation) {
									var row = me.ws.elan.getAnnotationNode(annotation);
									me.prevRow && me.prevRow.classList.remove('playing');
									me.prevRow = row;
									row.classList.add('playing');
									var before = row.previousSibling;
									if (before) {
										me.ws.elan.container.scrollTop = before.offsetTop;
									}
									me.region = me.ws.addRegion({
										start: annotation.start,
										end: annotation.end,
										resize: false,
										color: 'rgba(223, 240, 216, 0.7)'
									});
								}
							}
						});
					});
					me.ws.on('finish', function (data) {
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-play');
						playbtn.toggleValue = false;
					});
					me.ws.on('pause', function (data) {
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-play');
						playbtn.toggleValue = false;
					});
					Ext.Ajax.request({
						url: '/tab/rec/UIGetRecText',
						method: 'POST',
						headers: {
							'Content-Type': 'application/x-www-form-urlencoded'
						},
						async: true,
						params: {
							recordid: me.recordid
						},
						failure: function (response, opts) {
							console.log('server-side failure with status code ' + response.status);
						},
						success: function (response, opts) {
							var obj = Ext.decode(response.responseText);
							if (obj.success) {
								me.ws.elan.load(obj.items);
							}
						}
					});
				} else {
					me.ws.un('audioprocess');
					me.ws.on('audioprocess', function (time) {
						var step = Math.ceil(time);
						if (me.audioStep != step) {
							me.audioStep = step;
							me.down('#audioTimer').setText(step + "/" + Math.ceil(me.ws.getDuration()) + "(s)");
						}
					});
					tool.setType('down');
					me.setHeight(175);
					me.region && me.region.remove();
					me.region = null;
					me.prevAnnotation = null;
					me.prevRow && me.prevRow.classList.remove('success');
					me.prevRow = null;
					me.ws.destroyPlugin('regions');
					me.ws.destroyPlugin('elan');
				}

			}
		}
	}],
	buttons: [{
			itemId: 'playPause',
			tooltip: 'Play/Pause',
			xtype: 'button',
			iconCls: 'x-fa fa-play',
			toggleValue: false,
			listeners: {
				click: function (btn) {
					var me = btn.up('window');
					me.ws.playPause();
					btn.toggleValue = !btn.toggleValue;
					if (btn.toggleValue) {
						btn.setIconCls('x-fa fa-pause');
					} else {
						btn.setIconCls('x-fa fa-play');
					}
				}
			}
		},
		{
			xtype: 'segmentedbutton',
			itemId: 'speedButtons',
			items: [{
				text: 'x1',
				tooltip: '1x speed',
				ui: 'gray',
				pressed: true,
				handler: function (btn) {
					var me = btn.up('window');
					if (!me.ws.isPlaying()) {
						me.ws.play();
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-pause');
						playbtn.toggleValue = true;
					}
					me.ws.setPlaybackRate(1);
				}
			}, {
				text: 'x2',
				tooltip: '2x speed',
				ui: 'gray',
				handler: function (btn) {
					var me = btn.up('window');
					if (!me.ws.isPlaying()) {
						me.ws.play();
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-pause');
						playbtn.toggleValue = true;
					}
					me.ws.setPlaybackRate(2);
				}
			}, {
				text: 'x3',
				tooltip: '3x speed',
				ui: 'gray',
				handler: function (btn) {
					var me = btn.up('window');
					if (!me.ws.isPlaying()) {
						me.ws.play();
						var playbtn = me.down('#playPause');
						playbtn.setIconCls('x-fa fa-pause');
						playbtn.toggleValue = true;
					}
					me.ws.setPlaybackRate(3);
				}
			}]
		},
		{
			xtype: 'component',
			flex: 1
		},
		{
			xtype: 'label',
			itemId: 'audioTimer',
			text: '0/0'
		}
	]
});