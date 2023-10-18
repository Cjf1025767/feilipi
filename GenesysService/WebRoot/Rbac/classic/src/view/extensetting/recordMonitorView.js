Ext.define('Rbac.view.extensetting.recordMonitorModel', {
  extend: 'Ext.data.Model',
  idProperty: 'extension',
  fields: [
    {
      name: 'txtCallstate', type: 'string', convert: function (value, record) {
        var state = record.get('callstate');
        switch (state) {
          case 'inbound': return '<span class="phone-item-vertical" style="color:#42b533">状态:&nbsp;呼入&nbsp;</span><img class="phone-item-vertical" src="data:image/png;base64,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"></img>';
          case 'outbound': return '<span class="phone-item-vertical" style="color:#f29c2b">状态:&nbsp;呼出&nbsp;</span><img class="phone-item-vertical" src="data:image/png;base64,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"></img>';
          default: return '<span class="phone-item-vertical">状态:&nbsp;挂机</span>';
        }
      }
    },
    {
      name: 'txtAgent', type: 'string', convert: function (value, record) {
        var agent = record.get('agent');
        if (typeof agent == 'undefined' || agent.length == 0) {
          return '<i class="fa fa-user fa-3x" aria-hidden="true"></i>';
        }
        var username = record.get('username');
        if (typeof username != 'undefined' || username.length > 0){
          agent = username;
        }
        var ready = record.get('ready');
        var acw = (ready == false ? record.get('state') == 1 : false);
        if (ready) {
          return '<div class="phone-item"><i class="fa fa-user fa-3x phone-item-agent"></i>&nbsp;<i class="fa fa-headphones phone-item-agent">&nbsp;' + agent + '</i></div>';
        } else if (acw) {
          return '<div class="phone-item"><i class="fa fa-user-times fa-3x phone-item-agent" style="color:#e6c558"></i>&nbsp;<i class="fa fa-headphones phone-item-agent">&nbsp;' + agent + '</i></div>';
        }
        return '<div class="phone-item"><i class="fa fa-user fa-3x phone-item-agent" style="color:#e68c58"></i>&nbsp;<i class="fa fa-headphones phone-item-agent">&nbsp;' + agent + '</i></div>';
      }
    }
  ]
});

Ext.define('Rbac.view.extensetting.recordMonitorView', {
  extend: 'Ext.panel.Panel',
  xtype: 'recordmonitorview',
  layout: {
    type: 'hbox',
    align: 'stretch',
    pack: 'start'
  },
  requires: [
    'Ext.toolbar.TextItem',
    'Ext.view.View',
    'Ext.ux.BoxReorderer',
    'Ext.ux.DataView.Animated',
    'Ext.chart.*'
  ],
  onEvent: function (message) {
    var self = this;
    try {
      var objs = JSON.parse(message);
      if (objs.sender == 'Monitor') {
        //呼出模式:
        if (objs.ip.length > 0 && objs.port > 0) {
          var answer = {
            type: 'answer',
            sdp: 'v=0\r\n'
              + 'o=- 1629863438 1629863439 IN IP4 ' + objs.ip + '\r\n'
              + 's=-\r\n'
              + 'c=IN IP4 ' + objs.ip + '\r\n'
              + 't=0 0\r\n'
              + 'm=audio ' + objs.port + ' RTP/AVP 8\r\n'
              + 'a=rtpmap:8 PCMA/8000\r\n'
              + 'a=fingerprint:sha-256 C6:BD:B6:AA:71:19:12:2D:6D:E1:E8:2A:3D:91:11:2E:5B:9B:D5:6B:D3:8E:7B:B9:2A:10:D1:47:85:B9:5A:9A\r\n'
              + 'a=setup:active\r\n'
              + 'a=ice-ufrag:OMtJwfnuuOwbyObv\r\n'
              + 'a=ice-pwd:3tPRl2GZtrIlicsXOMwbSKwc\r\n'
              + 'a=rtcp-mux\r\n'
              + 'a=sendrecv\r\n'
          }
          self.dataview.peer.setRemoteDescription(new window.RTCSessionDescription(answer))
            .then(function () {
              console.debug(self.dataview.peer.remoteDescription);
            }).catch(function (reason) {
              console.error(reason.message);
            });

        } else {
          console.error("answer.sdp failed: " + objs.reason);
        }
        //*/
        //呼入模式:
        //无需操作
      } else if (objs.sender == 'Event') {
        if (objs.init) {
          if (objs.extensions && typeof objs.extensions != 'undefined') {
            self.dataview.rawData = objs.extensions;
            self.dataview.getStore().setData(objs.extensions);
            self.chart.up().down('button[name=all]').setText('全部(' + objs.extensions.length + ')');
            var nready = 0, nnotready = 0, nbusy = 0, nacw = 0, nlogout = 0;
            for (var newExten of objs.extensions) {
              if (newExten.agent && newExten.agent.length > 0 && newExten.ready && newExten.callstate == 'onhook') {
                nready++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.callstate == 'onhook' && newExten.ready == false && newExten.state == '1') {
                nacw++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.ready == false) {
                nnotready++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.callstate != 'onhook') {
                nbusy++;
              } else if (newExten.agent == null || typeof newExten.agent == 'undefined' || newExten.agent.length == 0) {
                nlogout++;
              }
            }
            self.chart.up().down('button[name=ready]').setText('空闲(' + nready + ')');
            self.chart.up().down('button[name=notready]').setText('小休(' + nnotready + ')');
            self.chart.up().down('button[name=nbusy]').setText('忙碌(' + nbusy + ')');
            self.chart.up().down('button[name=acw]').setText('后处理(' + nacw + ')');
            self.chart.up().down('button[name=logout]').setText('登出(' + nlogout + ')');
          }
          if (objs.queues && typeof objs.queues != 'undefined' && objs.queues.length > 0) {
            self.chart.setVisible(true);
            self.chart.rawData = objs.queues;
            self.chart.getStore().setData(objs.queues);
          } else {
            self.chart.setVisible(false);
          }
        } else {
          if (objs.extensions && typeof objs.extensions != 'undefined') {
            for (var newExten of objs.extensions) {
              for (var i = 0; i < self.dataview.rawData.length; i++) {
                if (self.dataview.rawData[i].extension == newExten.extension) {
                  Ext.apply(self.dataview.rawData[i], newExten);
                  break;
                }
              }
            }
            self.dataview.getStore().setData(self.dataview.rawData);
            var nready = 0, nnotready = 0, nbusy = 0, nacw = 0, nlogout = 0;
            for (var newExten of self.dataview.rawData) {
              if (newExten.agent && newExten.agent.length > 0 && newExten.ready && newExten.callstate == 'onhook') {
                nready++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.callstate == 'onhook' && newExten.ready == false && newExten.state == '1') {
                nacw++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.ready == false) {
                nnotready++;
              } else if (newExten.agent && newExten.agent.length > 0 && newExten.callstate != 'onhook') {
                nbusy++;                
              } else if (newExten.agent == null || typeof newExten.agent == 'undefined' || newExten.agent.length == 0) {
                nlogout++;
              }
            }
            self.chart.up().down('button[name=ready]').setText('空闲(' + nready + ')');
            self.chart.up().down('button[name=notready]').setText('小休(' + nnotready + ')');
            self.chart.up().down('button[name=nbusy]').setText('忙碌(' + nbusy + ')');
            self.chart.up().down('button[name=acw]').setText('后处理(' + nacw + ')');
            self.chart.up().down('button[name=logout]').setText('登出(' + nlogout + ')');
          }
          if (objs.queues && typeof objs.queues != 'undefined') {
            for (var newQueue of objs.queues) {
              for (var i = 0; i < self.chart.rawData.length; i++) {
                if (self.chart.rawData[i].group == newQueue.group) {
                  Ext.apply(self.chart.rawData[i], newQueue);
                  break;
                }
              }
            }
            self.chart.getStore().setData(self.chart.rawData);
          }
        }
      } else {
        console.log(message);
      }
    } catch (err) {
      console.log(err);
      console.log(message);
    }
  },
  listeners: {
    afterrender: function () {
      var self = this;
      window.TabMonitor = new function () {
        this.wshost = (window.location.protocol.indexOf('https') == 0 ? 'wss://' : 'ws://') + window.location.hostname + globalVars.monitorPort + '/tab/monitor';
        this.Reason = '';
        this.Connected = false;
        this.eventfn = null;
        this.rawData;

        this._Method = function (sMethod) {
          if (this.ws === null) return -1;
          this.ws.send(sMethod);
          if (!this.isConnecting) {
            this.ws.close();
            this.ws = null;
            return -1;
          }
          return 0;
        };

        this.isConnecting = false;
        this.ws = null;
        this.timerSocket = null;
        this.wsReconnect = function () {
          if (this.timerSocket) {
            clearTimeout(this.timerSocket);
            this.timerSocket = null;
          }
          if (this.ws) {
            this.ws.close();
            this.ws = null;
          }
          try {
            this.ws = new window.WebSocket(this.wshost + '/?Library=GetEvents', 'chat');
          } catch (e) { }
          this.ws.onmessage = function (evt) {
            TabMonitor.eventfn(evt.data);
            if ("Logout" === evt.data) {
              TabMonitor.isConnecting = false;
              TabMonitor.ws.close();
              TabMonitor.ws = null;
            }
          };
          this.ws.onclose = function () {
            TabMonitor.eventfn(JSON.stringify({ sender: 'Offline' }));
            if (TabMonitor.isConnecting) {
              TabMonitor.timerSocket = setTimeout(function () {
                TabMonitor.Connected = false;
                console.debug("WebSocket Reconnection....");
                TabMonitor.wsReconnect();
              }, 2000);
            }
          };
          this.ws.onopen = function () {
            if (TabMonitor.timerSocket) {
              clearTimeout(TabMonitor.timerSocket);
              TabMonitor.timerSocket = null;
            }
            this.send('Hello, WebSocket Server!');
            TabMonitor.Connected = true;
            TabMonitor.eventfn(JSON.stringify({ sender: 'Online' }));
          };
        };
        this.GetEvents = function (dataview, chart, fn) {
          if (!window.WebSocket) {
            this.Reason = "WebSocket not supported by this browser!";
            return -1;
          }
          if (typeof fn === 'function') {
            this.isConnecting = true;
            TabMonitor.eventfn = fn;
            TabMonitor.wsReconnect();
            this.Reason = "Connection...";
            this.dataview = dataview;
            this.chart = chart;
            return 0;
          } else {
            this.Reason = "Need a function as a parameter!";
            return -1;
          }
        };
        this.Monitor = function (ip, port, extension, agent) {
          var sMethod = "Monitor&Extension=" + extension;
          if (typeof agent != 'undefined' && agent !== null) {
            sMethod = sMethod + "&Agent=" + agent;
          }
          if (typeof port != 'undefined' && port !== null) {
            sMethod = sMethod + "&Port=" + port;
          }
          if (typeof ip != 'undefined' && ip !== null) {
            sMethod = sMethod + "&Ip=" + ip;
          }
          return this._Method(sMethod);
        };
        this.UnMonitor = function (ip, port, extension, agent) {
          var sMethod = "UnMonitor&Extension=" + extension;
          if (typeof agent != 'undefined' && agent !== null) {
            sMethod = sMethod + "&Agent=" + agent;
          }
          if (typeof port != 'undefined' && port !== null) {
            sMethod = sMethod + "&Port=" + port;
          }
          if (typeof ip != 'undefined' && ip !== null) {
            sMethod = sMethod + "&Ip=" + ip;
          }
          return this._Method(sMethod);
        };
        this.Close = function () {
          this.isConnecting = false;
          this.Reason = "Close";
          return this._Method("Close");
        };
      }
      window.TabMonitor.GetEvents(self.down('dataview'), self.down('cartesian'), self.onEvent);
    }
  },
  tbar: {
    items: [
      {
        xtype: 'container',
        layout: 'vbox',
        items: [
          {
            xtype: 'cartesian',
            width: '100%',
            height: 120,
            visible: false,
            store: {
              fields: ['group', {
                name: 'callerCount',
                mapping: 'callers',
                convert: function (v, r) {
                  return r.get('callers').length;
                }
              }]
            },
            axes: [{
              type: 'numeric',
              fields: ['callerCount'],
              hidden: true,
              grid: false,
              minimum: 500
            }, {
              type: 'category',
              position: 'bottom',
              fields: ['group'],
              renderer: function (axis, label, layoutContext) {
                if(typeof globalVars.monitorMaping[label] == 'undefined'){
                  return '队列';
                }
                return globalVars.monitorMaping[label];
              }
            }],
            series: [{
              type: 'bar',
              xField: 'group',
              yField: 'callerCount',
              label: {
                field: 'callerCount',
                display: 'insideStart',
              },
              colors: ['#e6586c']
            }]
          },
          {
            xtype: 'container',
            layout: 'hbox',
            items: [{
              xtype: 'segmentedbutton',
              items: [
                {
                  xtype: 'button',
                  pressed: true,
                  text: '全部(0)',
                  width: 100,
                  name: 'all',
                  iconCls: 'x-fa fa-users',
                  textAlign: 'left',
                  iconAlign: 'left',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().clearFilter();
                  }
                },
                {
                  xtype: 'button',
                  text: '空闲(0)',
                  width: 100,
                  name: 'ready',
                  iconCls: 'x-fa fa-user',
                  textAlign: 'left',
                  iconAlign: 'left',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().getFilters().replaceAll({
                      fn: function (item) {
                        if (item.get('agent').length > 0 && item.get('ready') && item.get('callstate') == 'onhook') {
                          return true;
                        }
                        return false;
                      }
                    });
                  }
                },
                {
                  xtype: 'button',
                  text: '小休(0)',
                  width: 100,
                  name: 'notready',
                  iconCls: 'x-fa fa-user-times',
                  textAlign: 'left',
                  iconAlign: 'left',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().getFilters().replaceAll({
                      fn: function (item) {
                        if (item.get('agent').length > 0 && item.get('callstate') == 'onhook' && item.get('ready') == false && item.get('state') == '1') {
                          return false;//ACW
                        } else if (item.get('agent').length > 0 && item.get('ready') == false) {
                          return true;
                        } else if (item.get('agent').length > 0 && item.get('callstate') != 'onhook') {
                          return false;
                        }
                        return false;
                      }
                    });
                  }
                },
                {
                  xtype: 'button',
                  text: '忙碌(0)',
                  width: 100,
                  name: 'nbusy',
                  iconCls: 'x-fa fa-user-times',
                  textAlign: 'left',
                  iconAlign: 'left',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().getFilters().replaceAll({
                      fn: function (item) {
                        if (item.get('agent').length > 0 && item.get('callstate') == 'onhook' && item.get('ready') == false && item.get('state') == '1') {
                          return false;//ACW
                        } else if (item.get('agent').length > 0 && item.get('ready') == false) {
                          return false;
                        } else if (item.get('agent').length > 0 && item.get('callstate') != 'onhook') {
                          return true;
                        }
                        return false;
                      }
                    });
                  }
                },
                {
                  xtype: 'button',
                  text: '后处理(0)',
                  width: 100,
                  name: 'acw',
                  iconCls: 'x-fa fa-user-plus',
                  textAlign: 'left',
                  iconAlign: 'left',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().getFilters().replaceAll({
                      fn: function (item) {
                        if (item.get('agent').length > 0 && item.get('callstate') == 'onhook' && item.get('ready') == false && item.get('state') == '1') {
                          return true;
                        }
                        return false;
                      }
                    });
                  }
                },
                {
                  xtype: 'button',
                  text: '登出(0)',
                  width: 100,
                  name: 'logout',
                  iconCls: 'x-fa fa-sign-out',
                  textAlign: 'left',
                  iconAlign: 'left',
                  margin: '0 31 0 0',
                  handler: function () {
                    this.up('panel').down('dataview').getStore().getFilters().replaceAll({
                      fn: function (item) {
                        if (item.get('agent').length == 0) {
                          return true;
                        }
                        return false;
                      }
                    });
                  }
                }
              ]
            }, { width: 30 },
            {
              xtype: 'button',
              text: '排序',
              width: 100,
              iconCls: 'x-fa fa-sort-amount-asc',
              enableToggle: true,
              toggleHandler: function (btn, state) {
                if (state) {
                  this.up('panel').down('dataview').getStore().sort('agent', 'ASC');
                } else {
                  this.up('panel').down('dataview').getStore().sort('extension', 'ASC');
                }
              }
            }
            ]
          }
        ]
      }
    ]
  },
  items: {
    xtype: 'dataview',
    layout: 'fit',
    peer: null,
    monitorAddress: null,
    attachStream: null,
    listeners: {
      'deselect': function (me, record) {
        if (window.location.protocol.indexOf('https') == 0) {
          var self = this;
          window.TabMonitor.UnMonitor(self.monitorAddress, 55000, record.get('extension'), record.get('agent'));
          var audio = document.getElementById("recordMonitorViewAudioId");
          if (audio != null) {
            audio.pause();
            console.debug('audio pause');
            audio.srcObject = null;
          }
          if (self.attachStream) {
            var tracks = self.attachStream.getTracks();
            tracks.forEach(function (track) {
              track.stop()
            })
            self.attachStream = null;
          }
          if (self.peer != null) {
            self.peer.close();
            self.peer = null;
            console.debug('peer close');
          }
          self.monitorAddress = null;
        }
      },
      'select': function (me, record, idx) {
        if (window.location.protocol.indexOf('https') == 0) {
          var self = this;
          navigator.mediaDevices.getUserMedia({ audio: true, video: false })
            .then(function (stream) {
              console.debug(stream);
              self.attachStream = stream;
              //var audioTracks = self.attachStream.getAudioTracks();
              //for (var i = 0, len = audioTracks.length; i < len; i++) {
              //  audioTracks[i].enabled = false;
              //}
              if (self.peer != null) {
                self.peer.close();
                self.peer = null;
                self.monitorAddress = null;
              }
              var config = {};
              config.bundlePolicy = "max-compat";
              self.peer = new window.RTCPeerConnection(config);
              stream.getAudioTracks().forEach(function (track) {
                console.debug(track);
                self.peer.addTrack(track, stream)
              });
              self.peer.ontrack = function (event) {
                var audio = document.getElementById("recordMonitorViewAudioId");
                if (audio != null) {
                  audio.pause();
                } else {
                  audio = document.createElement("audio");
                  audio.setAttribute("id", "recordMonitorViewAudioId");
                  document.body.appendChild(audio);
                }
                audio.srcObject = event.streams[0];
                console.debug(audio);
                console.debug(audio.srcObject);
              };
              self.peer.onicecandidate = function (event) {
                console.debug(event);
                if (event.candidate && event.candidate.address) {
                  var regex = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
                  if (regex.test(event.candidate.address)) {
                    if (!self.monitorAddress) self.monitorAddress = event.candidate.address;
                  }
                } else if (!event.candidate) {
                  window.TabMonitor.Monitor(self.monitorAddress, 55000, record.get('extension'), record.get('agent'));
                }
              }
              //呼出模式(首先createOffer发送,收到sdp后再setRemoteDescription):
              self.peer.createOffer().then(function (offer) {
                return self.peer.setLocalDescription(offer);
              })
                .then(function () {
                  console.debug(self.peer.localDescription);
                })
                .catch(function (reason) {
                  console.error(reason.message);
                });
              //*/
              /*//呼入模式(收到sdp后setRemoteDescription, 成功回调中调用createAnswer):
              var offer = {
                type: 'offer',
                sdp: 'v=0\r\n'
                  + 'o=- 1629863438 1629863439 IN IP4 10.6.9.12\r\n'
                  + 's=-\r\n'
                  + 'c=IN IP4 10.6.9.12\r\n'
                  + 't=0 0\r\n'
                  + 'm=audio 55000 RTP/AVP 8\r\n'
                  + 'a=rtpmap:8 PCMA/8000\r\n'
                  + 'a=fingerprint:sha-256 C6:BD:B6:AA:71:19:12:2D:6D:E1:E8:2A:3D:91:11:2E:5B:9B:D5:6B:D3:8E:7B:B9:2A:10:D1:47:85:B9:5A:9A\r\n'
                  + 'a=setup:actpass\r\n'
                  + 'a=ice-ufrag:OMtJwfnuuOwbyObv\r\n'
                  + 'a=ice-pwd:3tPRl2GZtrIlicsXOMwbSKwc\r\n'
                  + 'a=rtcp-mux\r\n'
                  + 'a=sendrecv\r\n'
              }
              self.peer.setRemoteDescription(new window.RTCSessionDescription(offer))
                .then(function () {
                  self.peer.createAnswer().then(function (answer) {
                    return self.peer.setLocalDescription(answer);
                  })
                    .then(function () {
                      console.log("answer.sdp:");
                      console.log(self.peer.remoteDescription);
                    })
                    .catch(function (reason) {
                      console.error(reason.message);
                    });
                });
              //*/
            })
            .catch(function (reason) {
              console.error(reason.message);
            });
        }
      }
    },
    reference: 'dataview',
    layout: 'fit',
    store: Ext.create('Ext.data.Store', {
      model: 'Rbac.view.extensetting.recordMonitorModel',
      sorters: [{
        property: 'extension',
        direction: 'ASC'
      }]
    }),
    id: 'phones',
    itemSelector: 'div.phone',
    overItemCls: 'phone-hover',
    scrollable: true,
    tpl: [
      '<tpl for=".">',
      '<div class="phone">',
      '{txtAgent}',
      '<div class="phone-item"><i class="fa fa-phone-square" aria-hidden="true">{extension}</i></div>',
      '{txtCallstate}<br/>',
      '{[this.message(values)]}',
      '</div>',
      '</tpl>',
      {
        message: function (values) {
          return "";
        },
      }
    ]
  }
})
