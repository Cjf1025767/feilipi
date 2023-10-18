Ext.define('Tab.view.setting.vipSettingViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.setting-vipsettingview',
    data: {
        name: 'WebServer'
    }
});

Ext.define('Tab.view.setting.VipSettingModel', {
	extend:'Ext.data.Model',
	fields:[{
		name:'phone',
		name:'blockid',
		name:'updatetime'
	}],
});
Ext.define('Tab.view.setting.VipSettingStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.setting.VipSettingModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListCustomerPhone',
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
