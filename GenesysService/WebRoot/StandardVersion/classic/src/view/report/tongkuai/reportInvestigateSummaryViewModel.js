
Ext.define('Tab.view.report.reportInvestigateSummaryViewModel', {
	extend:'Ext.data.Model',
	fields:[
	        {name:'nYear'},
	        {name: 'nMonth'},
	        {name: 'nDay'},
            {name: 'nDayOfWeek'},
            {name: 'nHour'},
            {name: 'nType'},
            {name: 'businessType'},
            {name: 'agentno'},
            {name: 'username'},
	        {name: 'n0Count'},
	        {name: 'n1Count'},
	        {name: 'n2Count'},
	        {name: 'n3Count'},
	        {name: 'n4Count'},
            {name: 'n5Count'},
            {name:'nTotalCallCount'},
            {name: "totalscore",convert:function(v,r){
                var veryscore=parseInt(r.data.n1count)*2
                var prety=parseInt(r.data.n2count)
                return veryscore+prety
            }},
	        ]
});

Ext.define('Tab.view.report.reportInvestigateSummaryViewStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.reportInvestigateSummaryViewModel',
    autoLoad: false,
    proxy : {
        type : 'rest',
		//url: '/tab/call/ReportInvestigateSummaryTK',
        url: '/tab/call/ReportInvestigateSummaryTKByQueues',
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
