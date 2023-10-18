Ext.define('Tab.view.main.reportSummaryViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-reportsummaryview',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.main.reportSummaryModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }}
    ]
});

Ext.define('Tab.view.main.reportSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.reportSummaryModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListSummaryRecord',
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