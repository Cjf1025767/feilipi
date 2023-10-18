Ext.define('Tab.view.main.reportViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-reportview',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.main.reportModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }}
    ]
});

Ext.define('Tab.view.main.reportStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.reportModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListRecord',
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