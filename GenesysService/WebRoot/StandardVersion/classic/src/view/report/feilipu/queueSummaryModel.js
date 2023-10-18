

Ext.define('Tab.view.report.feilipu.queueSummaryModel',{
	extend: 'Ext.data.Model',
	 fields: [
	      ]
});
Ext.define('Tab.view.report.feilipu.queueSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.queueSummaryModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/FLPrec/UISearchQueuesSummary',
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