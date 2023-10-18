Ext.define('Tab.view.newreport.reportAgentInOutSummaryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.newreport-reportagentinoutsummary',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Tab.view.newreport.ReportAgentInOutSummaryModel', {
	extend:'Ext.data.Model',
	fields:[
        {name: 'nYear'},
        {name: 'nMonth'},
        {name: 'nDay'},
        {name: 'nDayOfWeek'},
        {name: 'nHour'},
        {name: 'nType'},
        {name: 'agent'},
        {name: 'username'},
        {name: 'nOutboundCount'},          	//呼出接通数
        {name: 'nMaxConnectLength',type:'number'},		//呼出最大通话时长
        {name: 'nOutboundLength',type:'number'},			//呼出通话时长
        {name: 'nOutboundWait',type:'number'},			//呼出通话振铃时长
        {name: 'nNoConnectWait',type:'number'},			//呼出未应答振铃时长
        {name: 'nInboundCount'},			//呼入接通数
        {name: 'nNoAnswerCount'},			//呼入未应答数
        {name: 'nInboundWaitLessCount'},	//
        {name: 'nNoAnswerWait'},			//呼入无应答振铃时长
        {name: 'nInboundWait'},				//呼入应答振铃时长
        {name: 'nInboundLength'},
        {name: 'nMaxAnswerLength',type:'number'},			//呼入最大通话时长
        {
        	name: 'nOutboundTotalCount',	//呼出总数
        	convert:function(value,records){
        		return records.get('nOutboundCount') + records.get('nNoConnectCount');
        	}
        },
        {
        	name: 'nAvgOutboundLength',	type:'number',	//呼出平均通话时长
        	convert:function(value,records){
        		return standartLength(Math.round(records.get('nOutboundCount')== 0 ? 
        				0:(records.get('nOutboundLength')/records.get('nOutboundCount'))));
        	}
        },
        {
        	name: 'nTotalRingLength',type:'number',		//呼出总振铃时长
        	convert:function(value,records){
        		return standartLength(records.get('nOutboundWait') + records.get('nNoConnectWait'));
        	}
        },	
        {
        	name: 'nOutboundAvgRing',type:'number',		//呼出平均振铃时长
        	convert:function(value,records){
        		return standartLength(Math.round((records.get('nNoConnectCount') + records.get('nOutboundCount'))
        				== 0?0:((records.get('nNoConnectWait') + records.get('nOutboundWait')) / (records.get('nNoConnectCount') + records.get('nOutboundCount')))));
        	}
        },
        {
        	name: 'nInboundTotalCount',		//呼入总数
        	convert:function(value,records){
        		return records.get('nInboundCount') + records.get('nNoAnswerCount');
        	}
        },
        {
        	name: 'nInOutboundTotal',		//呼入呼出总数
        	convert:function(value,records){
        		return records.get('nInboundCount') +records.get('nNoAnswerCount')+records.get('nOutboundCount')+records.get('nNoConnectCount');
        	}
        },
        {
        	name: 'nInboundAnsweredRate',	//呼入接通率
        	direction:'DESC',
    		convert	: function(value,records) {
    			if((records.get('nNoAnswerCount') + records.get('nInboundCount'))==0)return '0%';
                var dLess = records.get('nInboundCount');
                var dTotal = records.get('nNoAnswerCount') + records.get('nInboundCount');
                return Math.round(dLess / dTotal * 10000) / 100.00 + "%";
    		}
        	
        },
        {
        	name: 'nServiceRate',
        	convert:function(value,records){
        		var dLess = records.get('nInboundWaitLessCount');
                var dTotal = records.get('nNoAnswerCount') + records.get('nInboundCount');
            	if(dLess > 0 && dLess<=dTotal){
            		return Math.round(dLess / dTotal * 10000) / 100.00 + "%";
            	}else{
            		return '0%';
            	} 
        	}
        },
        {
        	name: 'nInboundTotalRingLength', //呼入总振铃时长
        	convert:function(value,records){
        		return standartLength(records.get('nNoAnswerWait')+records.get('nInboundWait'));
        	}
        },
        {
        	name: 'nInboundAvgRingLength',type:'number',	//呼入平均振铃时长
        	convert:function(value,records){
        		return standartLength((records.get('nNoAnswerCount') + records.get('nInboundCount')) <= 0 || (records.get('nNoAnswerWait') + records.get('nInboundWait')) <= 0 ? 0 : (records.get('nNoAnswerWait') + records.get('nInboundWait')) / (records.get('nNoAnswerCount') + records.get('nInboundCount')));
        	}
        },
        {
        	name: 'nAvgInboundLength',type:'number',		//呼入平均通话时长
        	convert:function(value,records){
        		return standartLength(records.get('nInboundCount')==0?0:(records.get('nInboundLength') / records.get('nInboundCount')));
        	}
        },
        {
        	name: 'nOutboundAnsweredRate',type:'number',	//呼出接通率
        	direction:'DESC',
    		convert	: function(value,records) {
    			if((records.get('nNoConnectCount') + records.get('nOutboundCount'))==0)return '0%';
                var dLess = records.get('nOutboundCount');
                var dTotal = records.get('nNoConnectCount') + records.get('nOutboundCount');
                return Math.round(dLess / dTotal * 10000) / 100.00 + "%";
    		}
        }
    ]
});

Ext.define('Tab.view.newreport.ReportAgentInOutSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.newreport.ReportAgentInOutSummaryModel',
    groupField:'nDay',
    autoLoad: false,
    proxy : {
        type : 'rest',
		url: '/tab/call/ReportAgentDetailSummary',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'list',
            totalProperty : 'totalCount'
        }
    }
});
