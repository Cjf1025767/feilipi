Ext.define('Tab.view.main.appTrunkViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-apptrunkview',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.main.appTrunkModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }}
    ]
});

Ext.define('Tab.view.main.appTrunkStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.appTrunkModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListTrunk',
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