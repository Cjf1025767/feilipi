Ext.define('Tab.view.main.batchViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-batchview',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.main.batchModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }}
    ]
});

Ext.define('Tab.view.main.batchStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.batchModel',
    autoLoad: false,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListBatch',
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

Ext.define('Tab.view.main.batchPhoneModel',{
    extend: 'Ext.data.Model',
    idProperty:'_id',
    fields: [
        {name:'batchid',mapping:'id.batchid'},
        {name:'phone',mapping:'id.phone'}
    ]
});

Ext.define('Tab.view.main.batchPhoneStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.batchPhoneModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListBatchPhone',
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