Ext.define('Tab.view.sms.smsSignModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.sms-smssign',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.sms.signModel',{
    extend: 'Ext.data.Model',
    idProperty:'signname',
    fields: [
    ]
});

Ext.define('Tab.view.sms.signStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.sms.signModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListSmsSign',
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