

Ext.define('Tab.view.report.feilipu.miss_callsModel',{
	extend: 'Ext.data.Model',
});
Ext.define('Tab.view.report.feilipu.miss_callsStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.miss_callsModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/FLPrec/UISearchMissCalls',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'list',
            totalProperty : 'totalCount'
        }
    }
});