Ext.define('Tab.view.sms.smsTemplateModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.sms-smstemplate',
    data: {
        name: 'Tab'
    }

});


Ext.define('Tab.view.sms.templateModel',{
    extend: 'Ext.data.Model',
    idProperty:'templatecode',
    fields: [
    ]
});

Ext.define('Tab.view.sms.templateStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.sms.templateModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListSmsTemplate',
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