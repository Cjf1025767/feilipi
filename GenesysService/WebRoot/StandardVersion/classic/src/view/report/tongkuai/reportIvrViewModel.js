Ext.define('Tab.view.report.reportIvrViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.report-reportivrview',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Tab.view.report.ReportIvrModel',{
	extend: 'Ext.data.Model',
	
	 fields: [
			{name: "callivrrecordId"},
			{name: "callrefid"},
			{name: "ipaddress"},
			{name: "channel"},
			{name: "ani"},
			{name: "dnis"},
			{name: "lastnode",convert:function(v,r){
				if(v!=0){
					return v;
				}else{
					return "";
				}
            }},
			{name: "begintime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name: "endtime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
            {name: "type"},
            {name: "length"},
            {name: "agent"},
            {name: "transfer"},
            {name: "ucid"},
            {name: "host"},
            {name: "vdn"},
            {name: "username"},
            {name: "enterqueuetime"},
            {name: "exitqueuetime"},
            {name: 'department'}
	      ]
});
Ext.define('Tab.view.report.ReportIvrStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.ReportIvrModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ReportQueuesCallDetail',
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