Ext.define('Tab.view.newreport.reportAgentInOutSummaryView',{
    extend: 'Ext.grid.Panel',
    xtype:'reportAgentInOutSummaryView',

    requires: [
        'Tab.view.newreport.reportAgentInOutSummaryController',
        'Tab.view.newreport.reportAgentInOutSummaryModel',
        'Tab.view.newreport.ReportAgentInOutSummaryStore',
        'Ext.form.field.Spinner',
        'Ext.form.RadioGroup',
        'Ext.form.field.Radio',
        'Ext.grid.feature.Grouping',
        'Ext.grid.feature.GroupingSummary'
    ],

    controller: 'newreport-reportagentinoutsummary',
    viewModel: {
        type: 'newreport-reportagentinoutsummary'
    },
    store: Ext.create('Tab.view.newreport.ReportAgentInOutSummaryStore'),
    grouped: true,
	collapsible: true,
    collapseFirst: false,
    totalStore: null,
    features: [
    	{
            ftype : 'groupingsummary',
            groupHeaderTpl : '日期: {name} (共{rows.length}条)',
            hideGroupedHeader : false,
            enableGroupingMenu : false
        }
    ],
    header:false,
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
                cls:'fix-search-btn',
                items: [{
                    xtype: 'button',
                    width: 100,
                    margin:'0 0 10 0',
                    text: '查 询',
                    handler: function () {
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
                            type: me.down('radiogroup').getChecked()[0].inputValue
                        }
                        grid.store.load();
                    }
                }, {
                    xtype: 'button',
                    width: 100,
                    text: '导 出',
                    handler: function () {
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
                            type: me.down('radiogroup').getChecked()[0].inputValue
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
                            url: '/tab/call/ReportAgentDetailSummary',
                            method: 'POST',
                            hidden: true,
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                script: path + 'ReportAgentDetailSummaryExport.js',
                                starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                type: me.down('radiogroup').getChecked()[0].inputValue
                            }
                        });
                        Ext.defer(function () {
                            form.close(); //延迟关闭表单(不会影响浏览器下载)
                        }, 100);
                    }
                }]
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
                layout: 'vbox',
                items: [{
                    xtype: 'radiogroup',
                    width: 200,
                    layout: {
                        align: 'middle',
                        type: 'hbox'
                    },
                    items: [{
                            xtype: 'radiofield',
                            name: 'detailSummaryType',
                            inputValue: '3',
                            boxLabel: '每月'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'detailSummaryType',
                            inputValue: '2',
                            boxLabel: '每周'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'detailSummaryType',
                            inputValue: '1',
                            checked: true,
                            boxLabel: '每日'
                        },
                        {
                            xtype: 'radiofield',
                            name: 'detailSummaryType',
                            inputValue: '0',
                            boxLabel: '每时'
                        }
                    ]
                }]
            }
        ]
    },
    columns: [{
            xtype: 'rownumberer',
            width: 30,
            align: 'center'
        },
        {
            header: "时间",width: 160,dataIndex: 'nYear',
            renderer: function (v, metaData, record, rowIndex, colIndex, store, view) {
                switch (record.get('nType')) {
                    case 0:
                        return record.get('nYear') + '年' + record.get('nMonth') + '月' + record.get('nDay') + '日 ' + record.get('nHour')+'时';
                    case 1:
                        return record.get('nYear') + '年' + record.get('nMonth') + '月' + record.get('nDay')+ '日';
                    case 2:
                        return record.get('nYear') + '年' + record.get('nWeek') + '周';
                    case 3:
                        return record.get('nYear') + '年' + record.get('nMonth') + '月';
                }
            }
        },
        {
            header: "姓名",width: 115,align:'center',dataIndex: 'username',renderer: function (value,cell, records) {
				return (value == null || value == "")? records.get('agent'):value;
			}
        },
        {
            header: "工号",width: 125,align:'center',dataIndex: 'agent'
        },
        {
        	text:"呼入统计",
        	columns:[{
            	header:"呼入接通数",align:'center',dataIndex:'nInboundCount',summaryType: 'sum'
            },
            {
            	header:"呼入接通率",align:'center',dataIndex:'nInboundAnsweredRate',
            	summaryRenderer: function (value,summaryData,store) {
            		/* var me = this.up().up().up(),
            		nGroup = me.store.data._groups,
            		dAnswer = 0,dLess = 0,dTotal = 0,i = 0;
            		for(var i=0;i<nGroup.length;i++){
            			var obj = nGroup.items[i];
            			for(var j in obj){
            				if(j == obj[j].length-1){
            					dAnswer += nGroup.items[i].items[j].get('nInboundCount');
                    			dLess += nGroup.items[i].items[j].get('nNoAnswerCount');
                    			dTotal = dAnswer + dLess;	
            				}
            				return Math.round(dAnswer / dTotal * 10000) / 100.00 + "%";
            			}
            			
            		}
            		if(dTotal==0)return '0%'; */
                       
                    var me = this.up().up().up(),
            		dAnswer = 0,dLess = 0,dTotal = 0;
            		for(var i=0;i<me.store.data.length;i++){
            			dAnswer += me.store.data.items[i].get('nInboundCount');
            			dLess += me.store.data.items[i].get('nNoAnswerCount');
            			dTotal = dAnswer + dLess;
            		}
            			
            		if(dTotal==0)return '0%';
                    return Math.round(dAnswer / dTotal * 10000) / 100.00 + "%";
            		
                }
            },{
            	header:"呼入放弃数",align:'center',dataIndex:'nNoAnswerCount',summaryType: 'sum'
            },{
            	header:"呼入振铃时长",align:'center',dataIndex:'nInboundTotalRingLength',
            	summaryRenderer: function (value,summaryData,records) {
            		var me = this.up().up().up(),
            		dAnswer = 0,dLess = 0,dTotal = 0;
            		for(var i=0;i<me.store.data.length;i++){
            			dAnswer += me.store.data.items[i].get('nNoAnswerWait');
            			dLess += me.store.data.items[i].get('nInboundWait');
            			dTotal = dAnswer + dLess;
            		}
            			
            		if(dTotal==0)return '0';
                    return standartLength(dTotal);
                }
            },
            {
            	header:"呼入平均振铃时长",align:'center',width:150,dataIndex:'nInboundAvgRingLength',
            	summaryRenderer: function (value,summaryData,records) {
            		var me = this.up().up().up(),
            		dAnswer = 0,dLess = 0,dTotal = 0,dAnswerlen=0,dLesslen=0,dTotalLen=0;
            		
            		for(var i=0;i<me.store.data.length;i++){
            			dAnswerlen += me.store.data.items[i].get('nNoAnswerWait');
            			dLesslen += me.store.data.items[i].get('nInboundWait');
            			
            			dAnswer += me.store.data.items[i].get('nInboundCount');
            			dLess += me.store.data.items[i].get('nNoAnswerCount');
            			
            			dTotalLen = dAnswerlen + dLesslen;
            			dTotal = dAnswer + dLess;
            		}
            			
            		if(dTotalLen==0 || dTotal==0)return '0';
                    return standartLength(dTotalLen/dTotal);
                }
            },
            {
            	header:"呼入通话时长",align:'center',dataIndex:'nInboundLength',summaryType: 'sum',renderer:function(value,cell,records){
            		return standartLength(value);
            	}
            },
            {
            	header:"呼入平均通话时长",align:'center',width:150,dataIndex:'nAvgInboundLength'
            }]
        	
        },
        {
        	header:"呼入最大通话时长",align:'center',width:150,dataIndex:'nMaxAnswerLength',summaryType: 'sum',renderer:function(value,cell,records){
        		return standartLength(value);
        	}
        },
        {
        	text:"呼出统计",
        	columns:[{
        		header:"呼出数",align:'center',dataIndex:'nOutboundTotalCount',summaryType: 'sum'
        	},{
            	header:"呼出接通数",align:'center',dataIndex:'nOutboundCount',summaryType: 'sum'
            },
            {
            	header:"呼出接通率",align:'center',dataIndex: 'nOutboundAnsweredRate',
            	summaryRenderer: function (value,summaryData,records) {
            		var me = this.up().up().up(),
            		dAnswer = 0,dLess = 0,dTotal = 0;
            		for(var i=0;i<me.store.data.length;i++){
            			dAnswer += me.store.data.items[i].get('nOutboundCount');
            			dLess += me.store.data.items[i].get('nNoConnectCount');
            			dTotal = dAnswer + dLess;
            		}
            			
            		if(dTotal==0)return '0%';
                    return Math.round(dAnswer / dTotal * 10000) / 100.00 + "%";
                }
            },
            {
            	header:"呼出未接听",align:'center',dataIndex:'nNoConnectCount',summaryType: 'sum'
            },
            {
            	header:"呼出振铃时长",align:'center',dataIndex:'nTotalRingLength',summaryType: 'sum'
            },
            {
            	header:"呼出通话时长",align:'center',dataIndex:'nOutboundLength',summaryType: 'sum',renderer:function(value,cell,records){
            		return standartLength(value);
            	}/*,
            	summaryRenderer: function () {
					var me = this.up().up();
					if(me.totalStore==null || me.totalStore.totalCount==0)return '0';
                    return standartLength(me.totalStore.list[0].nOutboundLength);
                }*/
            },
            {
            	header:"呼出平均通话时长",align:'center',width:150,dataIndex:'nAvgOutboundLength',summaryType: 'sum'
            },]
        	
        },
        {
        	header:"呼出最大通话时长",align:'center',width:150,dataIndex:'nMaxConnectLength',summaryType: 'sum',renderer:function(value,cell,records){
        		return standartLength(value);
        	}
        }
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});
