Ext.define('Tab.view.report.reportDeptDetailWebModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.report-reportDeptDetailWebModel',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Tab.view.report.ReportDeptDetailModel',{
	extend: 'Ext.data.Model',
	
	 fields: [	          
			{name:"userAgent",mapping:"user.userAgent"},
			{name:"username",mapping:"user.userName"},
			{name:'ani'},
			{name:'begintime',convert:function(v,r){
				return Ext.Date.format(new Date(v),'H:i:s');
            }},
			{name:'callagentrecordid'},
			{name:'data'},
			{name:'dnis'},
			{name:'endtime',convert:function(v,r){
				return Ext.Date.format(new Date(v),'H:i:s');
            }},
			{name:'channel'},
			{name:'hold'},
			{name:'length'},
			{name:'ringdate',convert:function(v,r){
                return Ext.Date.format(new Date(r.get('ringtime')),'Y-m-d');
            }},
			{name:'ringtime',convert:function(v,r){
                return Ext.Date.format(new Date(v),'H:i:s');
            }},
			{name:'skill'},
			{name:'split'},
			{name:'transfer'},
			{name:'type'},
			{name:'ucid'},
			{name:'vdn'},
			{name:'wait'},
			{name:'workshifts'},
			{name:'department'}
	 ]
});

Ext.define('Tab.view.report.ReportDeptDetailStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.ReportDeptDetailModel',
    autoLoad: false,
	remoteSort: true,
    proxy : {
        type : 'rest',
		//url: '/tab/call/ReportAgentDetail',
		url: '/tab/call/ReportDeptDetailByqueues',
		
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