Ext.define('Tab.view.sms.SmsSummaryViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.sms-smssummaryview',
    data: {
        name: 'Tab'
    }

});


Ext.define('Tab.view.sms.SmsSummaryModel', {
    extend: 'Ext.data.Model',
    //fields: []
});

Ext.define('Tab.view.sms.SmsSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.sms.SmsSummaryModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/call/ReportSmsSummary',
        timeout: 120000,
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});