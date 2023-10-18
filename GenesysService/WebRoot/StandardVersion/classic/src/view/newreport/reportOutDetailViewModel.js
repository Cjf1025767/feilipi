Ext.define('Tab.view.newreport.reportOutDetailViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.newreport-reportoutdetailview',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Tab.view.newreport.ReportOutDetailModel',{
	extend: 'Ext.data.Model',
	 fields: [	          
			{name:"userAgent",mapping:"user.userAgent"},
			{name:"username",mapping:"user.userName"},
			{name:'ani'},
			{name:'begintime',convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name:'callagentrecordid'},
			{name:'data'},
			{name:'dnis'},
			{name:'endtime',convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name:'channel'},
			{name:'hold'},
			{name:'length'},
			{name:'ringtime',convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name:'skill'},
			{name:'split'},
			{name:'transfer'},
			{name:'type'},
			{name:'ucid'},
			{name:'vdn'},
			{name:'wait'},
			{name:'workshifts'}
	 ]
});

Ext.define('Tab.view.newreport.ReportOutDetailStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.newreport.ReportOutDetailModel',
    autoLoad: false,
    proxy : {
        type : 'rest',
		url: '/tab/call/ReportAgentDetail',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'list',
            totalProperty : 'totalCount'
        }
    },
    groupField:''
});