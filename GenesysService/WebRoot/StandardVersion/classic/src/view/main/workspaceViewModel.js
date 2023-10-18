Ext.define('Tab.view.main.workspaceViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-workspaceview',
    data: {
        name: 'Tab'
    }

});


Ext.define('Tab.view.main.dashboardTaskModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }},
        {name:'complete',type:'bool',mapping:function(v){
            return ((v.status&2)==2);
        }},
        {name:'running',type:'bool',mapping:function(v){
            return ((v.status&4)==4);
        }},
        {name:'success',mapping:'summary.complete'},
        {name:'failed',mapping:'summary.failed'},
        {name:'processing',mapping:'summary.processing'},
        {name:'talklength',mapping:'summary.talklength'},
        {name:'retrycount',mapping:'summary.retrycount'}
    ]
});

Ext.define('Tab.view.main.dashboardTaskStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.dashboardTaskModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/DashboardTaskInfo',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'items',
            totalProperty: 'total'
        }
    }
});