Ext.define('Tab.view.report.reportIvrWebSummaryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.report-reportIvrSummary',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Tab.view.report.reportIvrSummaryModel', {
	extend:'Ext.data.Model',
	fields:[
        {name: 'nyear'},
        {name: 'nmonth'},
        {name: 'nday'},
        {name: 'nweek'},
        {name: 'nhour'},
        {name: 'ntype'},
        {name: 'queue'},
        {name: 'department'},
        
    ]
});

Ext.define('Tab.view.report.reportIvrSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.reportIvrSummaryModel',
    autoLoad: false,
    proxy : {
        type : 'rest',
		//url: '/tab/call/ReportQueuesCallSummary',
        url: '/tab/call/ReportQueuesCallSummaryByQueues',
        
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
